package com.krickert.search.indexer.solr.httpclient.select;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.config.SolrConfiguration;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.uri.UriBuilder;
import io.reactivex.rxjava3.core.Single;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class HttpSolrSelectClientImpl implements HttpSolrSelectClient {
    private static final Logger log = LoggerFactory.getLogger(HttpSolrSelectClientImpl.class);

    private final HttpClient httpClient;
    private final String solrHost;
    private final String solrCollection;
    private final SolrConfiguration sourceSolrConfiguration;
    private final Collection<String> filters;
    private final ObjectMapper objectMapper;

    @Inject
    public HttpSolrSelectClientImpl(@Client HttpClient httpClient,
                                    IndexerConfiguration configuration) {
        log.info("Creating Http-based solr client");
        this.httpClient = checkNotNull(httpClient);
        checkNotNull(configuration);
        this.sourceSolrConfiguration = checkNotNull(configuration.getSourceSolrConfiguration());
        SolrConfiguration.Connection connection = checkNotNull(sourceSolrConfiguration.getConnection());
        this.solrHost = checkNotNull(connection.getUrl());
        this.solrCollection = checkNotNull(sourceSolrConfiguration.getCollection());
        final Collection<String> filters;
        if (CollectionUtils.isEmpty(sourceSolrConfiguration.getFilters())) {
            filters = Collections.emptyList();
        } else {
            filters = sourceSolrConfiguration.getFilters();
        }
        this.filters = filters;
        this.objectMapper = new ObjectMapper();
        log.info("Created Http-based solr client");
    }

    @Override
    public String getSolrDocs(String solrHost, String solrCollection, Integer paginationSize, Integer pageNumber) {
        try {
            return getResponseAsString(createSolrRequest(solrHost, solrCollection, paginationSize, pageNumber));
        } catch (Exception e) {
            log.error("Failed to get Solr documents", e);
            throw new RuntimeException("Failed to get Solr documents", e);
        }
    }

    @Override
    public String getSolrDocs(Integer paginationSize, Integer pageNumber) {
        try {
            return getResponseAsString(createSolrRequest(solrHost, solrCollection, paginationSize, pageNumber));
        } catch (Exception e) {
            log.error("Failed to get Solr documents", e);
            throw new RuntimeException("Failed to get Solr documents", e);
        }
    }

    private String getResponseAsString(URI uri) throws ExecutionException, InterruptedException {
        HttpRequest<?> request = HttpRequest.GET(uri);

        // If authentication is enabled, enhance the request with the basic auth header
        if (sourceSolrConfiguration.getConnection().getAuthentication() != null &&
                sourceSolrConfiguration.getConnection().getAuthentication().isEnabled()) {
            SolrConfiguration.Connection.Authentication authentication = sourceSolrConfiguration.getConnection().getAuthentication();
            if ("basic".equalsIgnoreCase(authentication.getType())) {
                request = HttpRequest.GET(uri)
                        .basicAuth(authentication.getUserName(), authentication.getPassword());
            } else {
                log.warn("Source authentication type '{}' is not supported. Skipping authentication for Solr request to {}",
                        authentication.getType(), sourceSolrConfiguration.getConnection().getUrl());
            }
        }

        Future<String> futureResponse = Single.fromPublisher(httpClient.retrieve(request, String.class)).toFuture();
        return futureResponse.get();
    }

    private URI createSolrRequest(Integer paginationSize, Integer pageNumber) {
        return createSolrRequest(solrHost, solrCollection, paginationSize, pageNumber);
    }

    private URI createSolrRequest(String solrHost, String solrCollection, Integer paginationSize, Integer pageNumber) {
        UriBuilder builder = UriBuilder.of(solrHost)
                .path(solrCollection)
                .path("select")
                .queryParam("q", "*:*")
                .queryParam("wt", "json")
                .queryParam("start", pageNumber * paginationSize)
                .queryParam("rows", paginationSize);
        for (String filter : filters) {
            builder.queryParam("fq", filter);
        }
        return builder.build();
    }

    @Override
    public Long getTotalNumberOfDocumentsForCollection() {
        return getTotalNumberOfDocumentsForCollection(solrHost, solrCollection);
    }

    @Override
    public Long getTotalNumberOfDocumentsForCollection(String solrHost, String solrCollection) {
        try {
            // Create the request for getting the total number of documents
            URI solrRequestUrl = UriBuilder.of(solrHost)
                    .path(solrCollection)
                    .path("select")
                    .queryParam("q", "*:*")
                    .queryParam("wt", "json")
                    .queryParam("rows", 0)
                    .build();

            // Get the response as a string
            String responseStr = getResponseAsString(solrRequestUrl);

            // Log the response for debugging purposes
            log.debug("Response from Solr: {}", responseStr);

            // Parse the JSON response to get `numFound`
            JsonNode jsonNode = objectMapper.readTree(responseStr);
            return jsonNode.path("response").path("numFound").asLong();
        } catch (Exception e) {
            log.error("Failed to fetch the total number of documents", e);
            throw new RuntimeException("Failed to fetch the total number of documents", e);
        }
    }
}