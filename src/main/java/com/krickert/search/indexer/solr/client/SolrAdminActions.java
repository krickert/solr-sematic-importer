package com.krickert.search.indexer.solr.client;

import jakarta.inject.Inject;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class SolrAdminActions {
    private static final Logger log = LoggerFactory.getLogger(SolrAdminActions.class);

    private final SolrClient solrClient;

    @Inject
    public SolrAdminActions(SolrClient solrClient) {
        this.solrClient = solrClient;
        assert solrClient != null;
        assert isSolrServerAlive();
    }

    /**
     * Checks if the connection to the Solr server is alive by sending a ping request.
     *
     * @return true if the ping is successful, false otherwise
     */
    public boolean isSolrServerAlive() {
        try {
            SolrPingResponse pingResponse = solrClient.ping();
            return pingResponse.getStatus() == 0;
        } catch (SolrServerException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    /**
     * Checks if a specific collection exists on the Solr server.
     *
     * @param collectionName the name of the collection to check
     * @return true if the collection exists, false otherwise
     */
    public boolean doesCollectionExist(String collectionName) {
        try {
            CollectionAdminRequest.List listRequest = new CollectionAdminRequest.List();
            CollectionAdminResponse response = listRequest.process(solrClient);
            Set<String> collections = response.getCollectionStatus().get(collectionName).asMap().keySet();
            return collections.contains(collectionName);
        } catch (SolrServerException | IOException e) {
            log.error("Exception thrown", e);
            return false;
        }
    }
}
