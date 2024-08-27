package com.krickert.search.indexer.solr.client;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.solr.client.solrj.impl.HttpListenerFactory.RequestResponseListener;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Result;

import java.io.IOException;

@Singleton
public class OktaAuthenticatedRequestResponseListener extends RequestResponseListener {

    private final OktaAuth oktaAuth;
    private final boolean isEnabled;

    /**
     * This class is a Solr listener that intercepts requests and adds authentication headers using Okta.
     * It extends the RequestResponseListener class.  It's used to add okta Bearer tokens to the header to allow the
     * client to use JWT authentication
     *
     * @param oktaAuth the OktaAuth object used to obtain the access token
     * @param isEnabled a boolean value indicating if authentication is enabled or not
     */
    @Inject
    public OktaAuthenticatedRequestResponseListener(OktaAuth oktaAuth, @Value("${solr-config.destination.connection.authentication.enabled}") boolean isEnabled ) {
        this.oktaAuth = oktaAuth;
        this.isEnabled = isEnabled;
    }

    @Override
    public void onQueued(Request request) {
        try {
            if (isEnabled) {
                // Get the access token, leveraging caching
                String accessToken = oktaAuth.getAccessToken();

                // Check and add Authorization header if it doesn't exist
                if (!request.getHeaders().contains("Authorization")) {
                    // Set the Authorization header using the Consumer
                    request.headers(headers -> headers.add("Authorization", "Bearer " + accessToken));
                }
            }
            // Add any other necessary headers here
            // e.g., request.header("Another-Header", "HeaderValue");

        } catch (IOException e) {
            throw new RuntimeException("Failed to obtain Okta access token", e);
        }

        // Proceed with queued request
        super.onQueued(request);
    }

    @Override
    public void onBegin(Request request) {
        super.onBegin(request);
    }

    @Override
    public void onComplete(Result result) {
        super.onComplete(result);
    }
}