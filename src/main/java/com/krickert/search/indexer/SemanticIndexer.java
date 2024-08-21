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
import com.krickert.search.indexer.solr.vector.CreateVectorCollectionService;
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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class SemanticIndexer {

    private static final Logger log = LoggerFactory.getLogger(SemanticIndexer.class);

    private final HttpSolrSelectClient httpSolrSelectClient;
    private final JsonToSolrDocParser jsonToSolrDoc;
    private final IndexerConfiguration indexerConfiguration;
    private final SolrAdminActions solrAdminActions;
    private final CreateVectorCollectionService createVectorCollectionService;
    ResourceLoader resourceLoader;

    @Inject
    public SemanticIndexer(HttpSolrSelectClient httpSolrSelectClient,
                           JsonToSolrDocParser jsonToSolrDoc,
                           IndexerConfiguration indexerConfiguration,
                           SolrAdminActions solrAdminActions,
                           CreateVectorCollectionService createVectorCollectionService,
                           ResourceLoader resourceLoader) {
        this.httpSolrSelectClient = httpSolrSelectClient;
        this.jsonToSolrDoc = jsonToSolrDoc;
        this.indexerConfiguration = indexerConfiguration;
        this.solrAdminActions = solrAdminActions;
        this.createVectorCollectionService = createVectorCollectionService;
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
        exportSolrDocsFromExternalSolrCollection(
                indexerConfiguration.getSourceSolrConfiguration().getConnection().getUrl(),
                indexerConfiguration.getDestinationSolrConfiguration().getConnection().getUrl(),
                indexerConfiguration.getSourceSolrConfiguration().getCollection(),
                indexerConfiguration.getDestinationSolrConfiguration().getCollection(),
                paginationSize);
    }

    public void exportSolrDocsFromExternalSolrCollection(String solr7Host, String solr9Host, String solrSourceCollection, String solrDestinationCollection, Integer paginationSize) {
        SolrClient solrClient = createSolr9Client(solr9Host, solrDestinationCollection);
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
            String solrDocs = httpSolrSelectClient.getSolrDocs(solr7Host, solrSourceCollection, paginationSize, currentPage++);
            HttpSolrSelectResponse response = jsonToSolrDoc.parseSolrDocuments(solrDocs);
            if (response.getNumFound() == 0) {
                log.info("No solr documents in source collection. Breaking");
                break;
            }
            if (totalExpected == -1) {
                totalExpected = response.getNumFound();
                numOfPagesExpected = (response.getNumFound() / paginationSize) + 1;
            }
            try {
                Collection<SolrInputDocument> documents = response.getDocs();
                if (response.getDocs().isEmpty()){
                    break;
                }
                log.info("Exporting {} documents from source collection {} to destination collection {}", documents.size(), solrSourceCollection, solrDestinationCollection);
                documents.forEach(doc -> {
                    convertSolrArrayLongToDate(doc, "creation_date");
                    createVectorCollectionService.addVectorFieldToDocument(doc, "title");
                });
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

    private static void convertSolrArrayLongToDate(SolrInputDocument doc, String fieldName) {
        // Retrieve the creation_date field
        @SuppressWarnings("unchecked")
        Long creationDate = (Long) doc.getFieldValue(fieldName);
        // Update the Solr document with the converted date strings
        doc.setField("creation_date", convertToSolrDateString(creationDate));
;
    }
    private static final DateTimeFormatter solrDateFormat = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    private static String convertToSolrDateString(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        return solrDateFormat.format(instant);
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
        Optional<URL> resource = resourceLoader.getResource(destinationSolrConfiguration.getCollectionConfigFile());
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
            log.error("Failed to upload configset {}. Exception: {}", destinationSolrConfiguration.getCollectionConfigFile(), e.getMessage());
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

    protected SolrClient createSolr9Client(String solrHost, String solrCollection) {
        log.info("Base Solr URL: {}", solrHost);
        return new Http2SolrClient.Builder(solrHost).withDefaultCollection(solrCollection).build();
    }

    protected SolrClient createSolr9Client() {
        String baseUrl = indexerConfiguration.getDestinationSolrConfiguration().getConnection().getUrl();
        log.info("Base Solr URL: {}", baseUrl);
        return createSolr9Client(baseUrl, indexerConfiguration.getDestinationSolrConfiguration().getCollection());
    }
}
