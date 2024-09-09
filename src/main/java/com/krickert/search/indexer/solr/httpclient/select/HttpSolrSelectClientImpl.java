package com.krickert.search.indexer.solr.httpclient.select;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krickert.search.indexer.util.SimpleGetRequest;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class HttpSolrSelectClientImpl implements HttpSolrSelectClient {
    private static final Logger log = LoggerFactory.getLogger(HttpSolrSelectClientImpl.class);

    private final SimpleGetRequest simpleGetRequest;
    private final String solrHost;
    private final String solrCollection;
    private final ObjectMapper objectMapper;

    @Inject
    public HttpSolrSelectClientImpl(SimpleGetRequest simpleGetRequest,
                                    @Value("${solr-config.source.connection.url}") String solrHost,
                                    @Value("${solr-config.source.collection}") String solrCollection) {
        log.info("Creating Http-based solr client");
        this.simpleGetRequest = checkNotNull(simpleGetRequest, "get request failed to load");
        this.solrHost = checkNotNull(solrHost, "solr host is needed");
        this.solrCollection = checkNotNull(solrCollection, "solr collection is needed");
        this.objectMapper = new ObjectMapper();
        log.info("Created Http-based solr client");
    }

    @Override
    public String getSolrDocs(String solrHost, String solrCollection, Integer paginationSize, Integer pageNumber) {
        return simpleGetRequest.getResponseAsString(createSolrRequest(solrHost, solrCollection, paginationSize, pageNumber));
    }

    @Override
    public String getSolrDocs(Integer paginationSize, Integer pageNumber) throws IOException, InterruptedException {
        return simpleGetRequest.getResponseAsString(createSolrRequest(solrHost, solrCollection, paginationSize, pageNumber));
    }

    private String createSolrRequest(Integer paginationSize, Integer pageNumber) {
        return createSolrRequest(solrHost, solrCollection, paginationSize, pageNumber);
    }

    private String createSolrRequest(String solrHost, String solrCollection, Integer paginationSize, Integer pageNumber) {
        return solrHost + "/" + solrCollection + "/select?q=*:*&wt=json&start=" + pageNumber * paginationSize + "&rows=" + paginationSize;
    }

    @Override
    public Long getTotalNumberOfDocumentsForCollection() {
        return getTotalNumberOfDocumentsForCollection(solrHost, solrCollection);
    }

    @Override
    public Long getTotalNumberOfDocumentsForCollection(String solr7Host, String solr7Collection) {
        try {
            // Create the request for getting the total number of documents
            String solrRequestUrl = solr7Host + "/" + solr7Collection + "/select?q=*:*&wt=json&rows=0";

            // Get the response as a string
            String responseStr = simpleGetRequest.getResponseAsString(solrRequestUrl);

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
