package com.krickert.search.indexer.solr.client;

import com.google.common.base.Preconditions;
import io.micronaut.core.io.ResourceLoader;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.ConfigSetAdminRequest;
import org.apache.solr.client.solrj.request.GenericSolrRequest;
import org.apache.solr.client.solrj.response.*;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Singleton
public class SolrAdminActions {
    private static final Logger log = LoggerFactory.getLogger(SolrAdminActions.class);

    private final SolrClient solrClient;
    private final ResourceLoader resourceLoader;

    @Inject
    public SolrAdminActions(SolrClient solrClient, ResourceLoader resourceLoader) {
        this.solrClient = solrClient;
        this.resourceLoader = resourceLoader;
        assert solrClient != null;
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
            log.error("Exception thrown", e);
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

    public boolean doesConfigExist(String configToCheck) {
        return doesConfigExist(solrClient, configToCheck);
    }

    public boolean collectionExists(SolrClient solrClient, String collection) {
        try {
            // List collections request
            CollectionAdminRequest.List listRequest = new CollectionAdminRequest.List();
            CollectionAdminResponse response = listRequest.process(solrClient);

            // Extract the list of collections
            @SuppressWarnings("unchecked") List<String> collections = (List<String>) response.getResponse().get("collections");

            return collections.contains(collection);
        } catch (RuntimeException | SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    public boolean doesConfigExist(SolrClient solrClient, String configToCheck) {
        Preconditions.checkNotNull(solrClient);
        Preconditions.checkNotNull(configToCheck);
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set("action", "LIST");

        // Make the request to the Solr Admin API to list config sets
        GenericSolrRequest request = new GenericSolrRequest(GenericSolrRequest.METHOD.GET, "/admin/configs", params);

        try {
            SimpleSolrResponse response = request.process(solrClient);
            // Extract config sets from the response
            NamedList<Object> responseNamedList = response.getResponse();
            @SuppressWarnings("unchecked")
            ArrayList<String> configSets = (ArrayList<String>) responseNamedList.get("configSets");

            if (configSets == null) {
                log.info("There are no configsets in this solr instance.");
                return false;
            }
            boolean configExists = false;

            for (String config : configSets) {
                if (configToCheck.equals(config)) {
                    configExists = true;
                    break;
                }
            }
            return configExists;
        } catch (SolrServerException | IOException e) {
            log.error("exception thrown", e);
            throw new RuntimeException(e);
        }
    }


    private void uploadConfigSet(SolrClient client) throws SolrServerException, IOException {
        ConfigSetAdminRequest.Upload request = new ConfigSetAdminRequest.Upload();
        request.setConfigSetName("semantic_simple");
        Optional<URL> resource = resourceLoader.getResource("classpath:semantic_base_config.zip");
        final File file;
        try {
            file = Paths.get(resource.get().toURI()).toFile();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        request.setUploadFile(file, "zip" );
        // Execute the request
        ConfigSetAdminResponse response = request.process(client);


        // Check the response status
        if (response.getStatus() == 0) {
            System.out.println("Configset uploaded successfully!");
        } else {
            System.out.println("Error uploading configset: " + response);
        }
    }
}
