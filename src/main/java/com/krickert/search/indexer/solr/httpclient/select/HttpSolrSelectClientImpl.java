package com.krickert.search.indexer.solr.httpclient.select;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Value;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


@Singleton
public class HttpSolrSelectClientImpl implements HttpSolrSelectClient {
    private static final Logger log = LoggerFactory.getLogger(HttpSolrSelectClientImpl.class);

    private final HttpClient httpClient;
    private final String solrHost;
    private final String solrCollection;
    private final ObjectMapper objectMapper;

    @Inject
    public HttpSolrSelectClientImpl(@Client HttpClient httpClient,
                                    @Value("${solr-config.source.connection.url}") String solrHost,
                                    @Value("${solr-config.source.collection}") String solrCollection) {
        log.info("Creating Http-based solr client");
        this.httpClient = httpClient;
        this.solrHost = solrHost;
        this.solrCollection = solrCollection;
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
        Future<String> futureResponse = Single.fromPublisher(httpClient.retrieve(HttpRequest.GET(uri), String.class))
                .toFuture();
        return futureResponse.get();
    }

    private URI createSolrRequest(Integer paginationSize, Integer pageNumber) {
        return createSolrRequest(solrHost, solrCollection, paginationSize, pageNumber);
    }

    private URI createSolrRequest(String solrHost, String solrCollection, Integer paginationSize, Integer pageNumber) {
        return UriBuilder.of(solrHost)
                .path(solrCollection)
                .path("select")
                .queryParam("q", "*:*")
                .queryParam("wt", "json")
                .queryParam("start", pageNumber * paginationSize)
                .queryParam("rows", paginationSize)
                .build();
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