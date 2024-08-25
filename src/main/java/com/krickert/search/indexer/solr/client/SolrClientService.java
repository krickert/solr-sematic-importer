package com.krickert.search.indexer.solr.client;

import com.krickert.search.indexer.config.IndexerConfiguration;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateHttp2SolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.request.RequestWriter;
import org.eclipse.jetty.http.HttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Factory
@Requires(notEnv = Environment.TEST)
public class SolrClientService {
    private static final Logger log = LoggerFactory.getLogger(SolrClientService.class);
    @Value("${solr-config.destination.connection.authentication.issuer}")
    private String tokenUrl;

    @Value("${solr-config.destination.connection.authentication.client-id}")
    private String clientId;

    @Value("${solr-config.destination.connection.authentication.client-secret}")
    private String clientSecret;

    @Inject
    @Client("${solr-config.destination.connection.authentication.issuer}")
    HttpClient oktaHttpClient;

    public Http2SolrClient createOktaSolrClient(String solrUrl) throws InterruptedException, ExecutionException, TimeoutException {
        String accessToken = getOktaAccessToken();

        return new Http2SolrClient.Builder(solrUrl)
                .withRequestWriter(new CustomRequestWriter(accessToken))
                .build();
    }

    @Bean
    @Named("solrClient")
    public Http2SolrClient createSolrClient(IndexerConfiguration indexerConfiguration) {
        String solrUrl = indexerConfiguration.getDestinationSolrConfiguration().getConnection().getUrl();
        String collection = indexerConfiguration.getDestinationSolrConfiguration().getCollection();
        return new Http2SolrClient.Builder(solrUrl)
                .withDefaultCollection(collection)
                .withFollowRedirects(true)
                .build();
    }


    @Bean
    @Named("concurrentClient")
    public ConcurrentUpdateHttp2SolrClient createConcurrentUpdateSolrClient(IndexerConfiguration indexerConfiguration) {
        String solrUrl = indexerConfiguration.getDestinationSolrConfiguration().getConnection().getUrl();
        Http2SolrClient solrClient = createSolrClient(indexerConfiguration);
        return new ConcurrentUpdateHttp2SolrClient.Builder(solrUrl, solrClient, false)
                .withQueueSize(indexerConfiguration.getDestinationSolrConfiguration().getConnection().getQueueSize())
                .withThreadCount(indexerConfiguration.getDestinationSolrConfiguration().getConnection().getThreadCount())
                .build();
    }

    private String getOktaAccessToken() throws InterruptedException, ExecutionException, TimeoutException {
        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);

        HttpResponse<Map> response = oktaHttpClient.toBlocking()
                .exchange(io.micronaut.http.HttpRequest.POST(tokenUrl, body)
                          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                          .accept(MediaType.APPLICATION_JSON), Map.class);

        if (response.getStatus().getCode() == 200) {
            @Nullable Map responseBody = response.body();
            return (String) responseBody.get("access_token");
        } else {
            throw new RuntimeException("Failed to retrieve access token: " + response.getStatus());
        }
    }

    // CustomRequestWriter to add authorization header
    public static class CustomRequestWriter extends RequestWriter {
        private final String accessToken;

        public CustomRequestWriter(String accessToken) {
            this.accessToken = accessToken;
        }

        @Override
        public void write(SolrRequest<?> request, OutputStream os) throws IOException {
            request.setBasePath(request.getBasePath());
            request.getHeaders().put(HttpHeader.AUTHORIZATION.asString(), "Bearer " + accessToken);
            super.write(request, os);
        }
    }
}