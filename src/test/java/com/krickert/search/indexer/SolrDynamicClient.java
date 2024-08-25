package com.krickert.search.indexer;

import com.google.api.Http;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SolrDynamicClient {
    private static final Logger log = LoggerFactory.getLogger(SolrDynamicClient.class);

    private final HttpClient httpClient;

    public SolrDynamicClient(@Client HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public HttpResponse<String> sendJsonToSolr(String solrURL, String collection, String jsonDocument) throws HttpClientResponseException {
        return httpClient.toBlocking().exchange(HttpRequest.POST(solrURL + "/" + collection + "/update", jsonDocument).contentType("application/json"), String.class);
    }

    public HttpResponse<String> createCollection(String solrURL, String collectionName) throws HttpClientResponseException {
        log.info("Creating collection: {} on solr: {}", collectionName, solrURL);
        String collectionAPI = "/admin/collections?action=CREATE&name=" + collectionName + "&numShards=1&replicationFactor=1&maxShardsPerNode=1&collection.configName=_default";
        return httpClient.toBlocking().exchange(HttpRequest.GET(solrURL + collectionAPI), String.class);
    }

    public HttpResponse<String> deleteCollection(String solrURL, String collectionName) throws HttpClientResponseException {
        log.info("Deleting collection: {} on solr: {}", collectionName, solrURL);
        String collectionAPI = "/admin/collections?action=DELETE&name=" + collectionName;
        return httpClient.toBlocking().exchange(HttpRequest.GET(solrURL + collectionAPI), String.class);
    }

    public HttpResponse<String> commit(String solrURL, String collectionName) throws HttpClientResponseException {
        log.info("Committing collection: {} on solr: {}", collectionName, solrURL);
        return sendJsonToSolr(solrURL, collectionName, "{ \"commit\": {} }");
    }
}