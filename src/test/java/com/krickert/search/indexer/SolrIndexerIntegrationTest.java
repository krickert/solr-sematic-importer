package com.krickert.search.indexer;

import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.enhancers.ProtobufToSolrDocument;
import com.krickert.search.indexer.solr.JsonToSolrDocParser;
import com.krickert.search.indexer.solr.SolrDocumentConverter;
import com.krickert.search.indexer.solr.SolrTestContainers;
import com.krickert.search.indexer.solr.client.SolrAdminActions;
import com.krickert.search.indexer.solr.httpclient.admin.SolrAdminTaskClient;
import com.krickert.search.indexer.solr.httpclient.select.HttpSolrSelectClient;
import com.krickert.search.indexer.solr.httpclient.select.HttpSolrSelectClientImpl;
import com.krickert.search.model.pipe.PipeDocument;
import com.krickert.search.model.test.util.TestDataHelper;
import com.krickert.search.model.util.ProtobufUtils;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;

import io.micronaut.http.HttpResponse;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.ConfigSetAdminRequest;
import org.apache.solr.client.solrj.response.ConfigSetAdminResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.SolrContainer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@MicronautTest
public class SolrIndexerIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(SolrIndexerIntegrationTest.class);


    private final SolrDynamicClient solrDynamicClient;
    private final ResourceLoader resourceLoader;
    private SemanticIndexer semanticIndexer;
    private final IndexerConfiguration indexerConfiguration;
    private final SolrTestContainers solrTestContainers;
    private final SolrContainer container7;
    private final SolrContainer container9;
    private final ProtobufToSolrDocument protobufToSolrDocument = new ProtobufToSolrDocument();
    private final SolrAdminTaskClient adminTaskClient;

    @BeforeEach
    void setUp() {
        // Mock the behavior of createSolr9Client
        // Assuming createSolr9Client is a method in SemanticIndexer

        // Partial mock of the semanticIndexer to spy on it
        semanticIndexer = spy(semanticIndexer);
        SolrClient solr9Client = createSolr9Client();
        // Override the createSolr9Client method
        doReturn(solr9Client).when(semanticIndexer).createSolr9Client();
    }

    @Inject
    public SolrIndexerIntegrationTest(
            SolrDynamicClient solrDynamicClient,
            ResourceLoader resourceLoader,
            SemanticIndexer semanticIndexer,
            IndexerConfiguration indexerConfiguration,
            SolrTestContainers solrTestContainers,
            SolrAdminTaskClient adminTaskClient) {
        this.solrDynamicClient = solrDynamicClient;
        this.resourceLoader = resourceLoader;
        this.semanticIndexer = semanticIndexer;
        this.indexerConfiguration = indexerConfiguration;
        this.solrTestContainers = solrTestContainers;
        this.container7 = solrTestContainers.getContainer7();
        this.container9 = solrTestContainers.getContainer9();
        this.adminTaskClient = adminTaskClient;
        log.info("Indexer configuration: {}", indexerConfiguration);
        log.info("solrTestContainers: {}", solrTestContainers);
    }

    private SolrClient createSolr9Client() {
        String baseSolrUrl = "http://" + container9.getHost() + ":" + container9.getSolrPort() + "/solr";
        log.info("Base Solr URL: {}", baseSolrUrl);
        return new Http2SolrClient.Builder(baseSolrUrl).build();
    }

    @Test
    void testSolr9Ping() throws SolrServerException, IOException {
        try (SolrClient client = createSolr9Client()) {
            client.ping("dummy");
        }
    }

    @Test
    void testSemanticIndexer() {
        //this would just run, but we first have to setup the source and destination solr
        setupSolr7ForExportTest();
        semanticIndexer.exportSolrDocsFromExternalSolrCollection(
                getSolr7Url(),
                getSolr9Url(),
                indexerConfiguration.getSourceSolrConfiguration().getCollection(),
                indexerConfiguration.getDestinationSolrConfiguration().getCollection(),
                5);
    }

    private void setupSolr7ForExportTest() {
        String solrUrl = getSolr7Url();
        String collection = indexerConfiguration.getSourceSolrConfiguration().getCollection();
        log.info("Solr source collection: {}", collection);
        solrDynamicClient.createCollection(solrUrl, collection);
        log.info("Solr source collection created: {}", collection);
        Collection<PipeDocument> protos = TestDataHelper.getFewHunderedPipeDocuments().stream().filter(doc -> doc.getDocumentType().equals("ARTICLE")).toList();
        List<SolrInputDocument> solrDocuments = protos.stream().map(protobufToSolrDocument::convertProtobufToSolrDocument).collect(Collectors.toList());
        solrDynamicClient.sendJsonToSolr(solrUrl, collection, SolrDocumentConverter.convertSolrDocumentsToJson(solrDocuments));
        solrDynamicClient.commit(solrUrl, collection);
        log.info("Solr protocol buffer documents have been imported to Solr 7");
    }

    private @NotNull String getSolr7Url() {
        String solr7Url =  "http://" + container7.getHost() + ":" + container7.getSolrPort() + "/solr";
        log.info("Solr 7 URL: {}", solr7Url);
        return solr7Url;
    }

    private @NotNull String getSolr9Url() {
        String solr9Url =  "http://" + container9.getHost() + ":" + container9.getSolrPort() + "/solr";
        log.info("Solr 9 URL: {}", solr9Url);
        return solr9Url;
    }

    @Test
    void testImportSolr7ToSolr9() throws SolrServerException, IOException {
        String testCollection = "solr_7_test_docs";
        String sol7rUrl = "http://" + container7.getHost() + ":" + container7.getSolrPort() + "/solr";
        SimpleGetRequest simpleGetRequest = new SimpleGetRequest();
        HttpSolrSelectClient httpSolrSelectClient = new HttpSolrSelectClientImpl(simpleGetRequest, sol7rUrl, testCollection);
        HttpResponse<String> createCollectionResponse = null;
        try {
            createCollectionResponse = solrDynamicClient.createCollection(sol7rUrl, testCollection);
            assertEquals(200, createCollectionResponse.code());
            String jsonData = loadResource();
            HttpResponse<String> addDocsResponse = solrDynamicClient.sendJsonToSolr(sol7rUrl, testCollection, jsonData);
            assertEquals(200, addDocsResponse.code());
            HttpResponse<String> commitResponse = solrDynamicClient.sendJsonToSolr(sol7rUrl, testCollection, "{ \"commit\": {} }");
            assertEquals(200, commitResponse.code());
            String solrDocs = httpSolrSelectClient.getSolrDocs(10, 0);
            JsonToSolrDocParser jsonToSolrDoc = new JsonToSolrDocParser();
            Collection<SolrInputDocument> docs = jsonToSolrDoc.parseSolrDocuments(solrDocs).getDocs();
            SolrClient solrClient = createSolr9Client();
            uploadConfigSet(solrClient, "price_semantic_example.zip");
            solrClient.request(CollectionAdminRequest.createCollection(testCollection, "semantic_simple",1, 1));
            UpdateResponse addSolr9DocsResponse = solrClient.add(testCollection, docs);
            assertEquals(0, addSolr9DocsResponse.getStatus());
            UpdateResponse commitSolr9Response = solrClient.commit(testCollection);
            assertEquals(0, commitSolr9Response.getStatus());

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (createCollectionResponse != null && createCollectionResponse.code() == 200) {
                HttpResponse<String> deleteCollectionResponse = solrDynamicClient.deleteCollection(sol7rUrl, testCollection);
                assertEquals(200, deleteCollectionResponse.code());
            }
        }
    }

    private void uploadConfigSet(SolrClient client, String resourceConfigZip) throws SolrServerException, IOException {
        ConfigSetAdminRequest.Upload request = new ConfigSetAdminRequest.Upload();
        request.setConfigSetName("semantic_simple");
        Optional<URL> resource = resourceLoader.getResource(resourceConfigZip);
        assertTrue(resource.isPresent(), "Resource not found!");
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

    private String loadResource() throws IOException {
        Optional<InputStream> file = resourceLoader.getResourceAsStream("solr_docs.json");
        if (file.isPresent()) {
            try (InputStream is = file.get()) {
                return IOUtils.toString(is, StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new IOException(e);
            }
        } else {
            throw new FileNotFoundException("solr_docs.json");
        }
    }
    String[] docs =
            {
                    "Watermelons are red.",
                    "I like watermelons for breakfast.",
                    "Saffron is a wonderful spice for soup.",
                    "Hollywood writers are demanding higher wages",
                    "The fruit is large, green outside and red inside with seeds.",
                    "Skyscrapers are taller than most buildings",
                    "Construction workers did not cross the picket line",
                    "A bowling score of 300 is a terrible score",
                    "bat, ball, mitt, diamond, park"
            };
}

