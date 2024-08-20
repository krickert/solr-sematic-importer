package com.krickert.search.indexer;

import com.google.common.base.Preconditions;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.config.SolrConfiguration;
import com.krickert.search.indexer.config.VectorConfig;
import com.krickert.search.indexer.enhancers.ProtobufToSolrDocument;
import com.krickert.search.indexer.solr.JsonToSolrDocParser;
import com.krickert.search.indexer.solr.client.SolrAdminActions;
import com.krickert.search.indexer.solr.httpclient.select.HttpSolrSelectClient;
import com.krickert.search.indexer.solr.httpclient.select.HttpSolrSelectResponse;
import com.krickert.search.model.pipe.PipeDocument;
import io.micronaut.core.io.ResourceLoader;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.ConfigSetAdminRequest;
import org.apache.solr.client.solrj.request.GenericSolrRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.client.solrj.response.ConfigSetAdminResponse;
import org.apache.solr.client.solrj.response.SimpleSolrResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class SemanticIndexer {

    private static final Logger log = LoggerFactory.getLogger(SemanticIndexer.class);

    private final HttpSolrSelectClient httpSolrSelectClient;
    private final JsonToSolrDocParser jsonToSolrDoc;
    private final IndexerConfiguration indexerConfiguration;
    private final SolrAdminActions solrAdminActions;
    ResourceLoader resourceLoader;

    @Inject
    public SemanticIndexer(HttpSolrSelectClient httpSolrSelectClient, JsonToSolrDocParser jsonToSolrDoc, IndexerConfiguration indexerConfiguration, SolrAdminActions solrAdminActions, ResourceLoader resourceLoader) {
        this.httpSolrSelectClient = httpSolrSelectClient;
        this.jsonToSolrDoc = jsonToSolrDoc;
        this.indexerConfiguration = indexerConfiguration;
        this.solrAdminActions = solrAdminActions;
        this.resourceLoader = resourceLoader;
    }

    public List<Message> convertDescriptorsToMessages(Collection<PipeDocument> descriptors) {
        List<Message> messages = new ArrayList<>();

        for (PipeDocument descriptor : descriptors) {
            DynamicMessage message = DynamicMessage.newBuilder(descriptor).build();
            messages.add(message);
        }

        return messages;
    }


    public void exportSolrDocsFromExternalSolrCollection(Integer paginationSize) {
        SolrClient solrClient = createSolr9Client();
        //ensures that the vector configuration is there.  If it's not, then it will
        //add a default configuration that comes with the app.
        setupSolr9Config(solrClient, indexerConfiguration.getDestinationSolrConfiguration());
        setupSolr9DocumentVectorCollection(solrClient, indexerConfiguration.getDestinationSolrConfiguration());
        setupSolr9VectorCollections(indexerConfiguration.getVectorConfig(), indexerConfiguration.getDestinationSolrConfiguration());

        if (paginationSize == null || paginationSize <= 0) {
            throw new IllegalArgumentException("paginationSize must be greater than 0");
        }
        int currentPage = 0;
        long totalExpected = -1;
        long numOfPagesExpected = -1;
        while (numOfPagesExpected != currentPage) {
            String solrDocs = null;
            try {
                solrDocs = httpSolrSelectClient.getSolrDocs(paginationSize, currentPage++);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            HttpSolrSelectResponse response = jsonToSolrDoc.parseSolrDocuments(solrDocs);
            if (totalExpected == -1) {
                totalExpected = response.getNumFound();
                numOfPagesExpected = (response.getNumFound() / paginationSize) + 1;
            }

            try {
                solrClient.add(response.getDocs());
            } catch (SolrServerException | IOException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            String collection = indexerConfiguration.getDestinationSolrConfiguration().getCollection();
            solrClient.commit(collection);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupSolr9VectorCollections(Map<String, VectorConfig> vectorConfig, SolrConfiguration destinationSolrConfiguration) {

    }

    private void setupSolr9Config(SolrClient solrClient, SolrConfiguration destinationSolrConfiguration) {

        if (solrAdminActions.doesConfigExist(solrClient, "senamtic_default_config")) {
            log.info("default semantic configuration already exists. Skipping creation");
            return;
        }

        ConfigSetAdminRequest.Upload request = new ConfigSetAdminRequest.Upload();
        request.setConfigSetName("semantic_default_config");
        Optional<URL> resource = resourceLoader.getResource("classpath:semantic_base_config.zip");
        final File file;
        try {
            file = Paths.get(resource.get().toURI()).toFile();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        request.setUploadFile(file, "zip");
        // Execute the request
        ConfigSetAdminResponse response = null;
        try {
            response = request.process(solrClient);
            log.info("Configset semantic_base_config.zip uploaded successfully! {}", response);
        } catch (SolrServerException | IOException e) {
            log.error("Failed to upload configset semantic_base_config.zip", e);
            throw new RuntimeException(e);
        }


        // Check the response status
        if (response.getStatus() == 0) {
            log.info("Configset uploaded successfully!");
        } else {
            System.out.println("Error uploading configset: " + response);
        }
    }



    public void setupSolr9DocumentVectorCollection(SolrClient solrClient, SolrConfiguration config) {

        if (!solrAdminActions.collectionExists(solrClient, config.getCollection())) {
            log.info("Creating collection {}", config.getCollection());
            // Create a collection request
            //TODO: add shards and replicas
            CollectionAdminRequest.Create createRequest = CollectionAdminRequest.createCollection(config.getCollection(), "semantic_default_config", 1, 1);
            // Execute the request
            CollectionAdminResponse response = null;
            try {
                response = createRequest.process(solrClient);
            } catch (SolrServerException | IOException e) {
                throw new RuntimeException(e);
            }

            // Check the response
            if (response.isSuccess()) {
                log.info("Solr collection was created successfully. {}", response);
            } else {
                log.info("Solr collection was not created successfully. {}", response);
            }
        }

    }


    protected SolrClient createSolr9Client() {
        String baseUrl = indexerConfiguration.getDestinationSolrConfiguration().getConnection().getUrl();
        log.info("Base Solr URL: {}", baseUrl);
        return new Http2SolrClient.Builder(baseUrl).withDefaultCollection(indexerConfiguration.getDestinationSolrConfiguration().getCollection()).build();
    }
}
