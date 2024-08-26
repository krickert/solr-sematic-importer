package com.krickert.search.indexer.solr.client;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.Date;

@Singleton
public class OktaAuth {
    public final String clientSecret;
    public final String clientId;
    public final String tokenEndpoint;
    public OktaAuth(
            @Value("${solr-config.destination.connection.authentication.client-secret}") String clientSecret,
            @Value("${solr-config.destination.connection.authentication.client-id}") String clientId,
            @Value("${solr-config.destination.connection.authentication.issuer}") String tokenEndpoint) {
        this.clientSecret = clientSecret;
        this.clientId = clientId;
        this.tokenEndpoint = tokenEndpoint;
    }


    public static String createDpopHeader(String htu, String htm, KeyPair keyPair, String nonce) throws Exception {
        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();

        // JWK key
        JWK jwk = new ECKey.Builder(Curve.P_256, publicKey).keyID("123").build();

        // Create JWT
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim("htu", htu)
                .claim("htm", htm)
                .claim("nonce", nonce)
                .issueTime(new Date())
                .jwtID("unique-dpop-jwt-id")
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.ES256)
                        .type(new JOSEObjectType("dpop+jwt"))
                        .jwk(jwk)
                        .build(),
                claimsSet);

        // Sign the JWT
        signedJWT.sign(new ECDSASigner(privateKey));

        return signedJWT.serialize();
    }

    public static String getNonceFromResponse(Response response) {
        return response.header("DPoP-Nonce");
    }

    public String getAccessToken() throws IOException {
        OkHttpClient client = new OkHttpClient();

        String credentials = clientId + ":" + clientSecret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        // Generate EC Key Pair
        KeyPair keyPair;
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(Curve.P_256.toECParameterSpec());
            keyPair = generator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Step 1: Make an initial token request to get the nonce
        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("scope", "solr")
                .build();

        Request nonceRequest = new Request.Builder()
                .url(tokenEndpoint)
                .addHeader("Authorization", "Basic " + encodedCredentials)
                .post(formBody)
                .build();

        String nonce;
        try (Response nonceResponse = client.newCall(nonceRequest).execute()) {
            if (nonceResponse.code() == 401) {
                nonce = getNonceFromResponse(nonceResponse);
            } else if (nonceResponse.code() == 200) {
                // Successful request; no nonce needed
                String responseBody = nonceResponse.body() != null ? nonceResponse.body().string() : "";
                JSONObject jsonObject = new JSONObject(responseBody);
                return jsonObject.getString("access_token");
            } else {
                String responseBody = nonceResponse.body() != null ? nonceResponse.body().string() : "No error body";
                throw new IOException("Unexpected code " + nonceResponse + " with body: " + responseBody);
            }
        }

        // Step 2: Make the actual token request with the DPoP proof
        String dpopProof;
        try {
            dpopProof = createDpopHeader(tokenEndpoint, "POST", keyPair, nonce);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Request request = new Request.Builder()
                .url(tokenEndpoint)
                .addHeader("Authorization", "Basic " + encodedCredentials)
                .addHeader("DPoP", dpopProof)
                .post(formBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "No error body";
                throw new IOException("Unexpected code " + response + " with body: " + responseBody);
            }
            String responseBody = response.body().string();
            JSONObject jsonObject = new JSONObject(responseBody);
            String token = jsonObject.getString("access_token");



            return token;
        }
    }


}