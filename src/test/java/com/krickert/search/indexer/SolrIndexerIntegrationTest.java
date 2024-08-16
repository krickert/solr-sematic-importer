package com.krickert.search.indexer;

import com.krickert.search.indexer.solr.HttpSolrSelectClient;
import com.krickert.search.indexer.solr.HttpSolrSelectClientImpl;
import com.krickert.search.indexer.solr.JsonToSolrDoc;
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
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
public class SolrIndexerIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(SolrIndexerIntegrationTest.class);

    private final DockerImageName SOLR9_IMAGE = DockerImageName.parse("solr:9.6.1");
    private final SolrContainer container9 = createContainer(SOLR9_IMAGE);
    private final DockerImageName SOLR7_IMAGE = DockerImageName.parse("solr:7.7.3");
    private final SolrContainer container7 = createContainer(SOLR7_IMAGE);
    private final SolrDynamicClient solrDynamicClient;
    private final ResourceLoader resourceLoader;
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

    @Inject
    public SolrIndexerIntegrationTest(SolrDynamicClient solrDynamicClient, ResourceLoader resourceLoader) {
        this.solrDynamicClient = solrDynamicClient;
        this.resourceLoader = resourceLoader;
    }

    private SolrContainer createContainer(DockerImageName image) {
        // Create the solr container.
        SolrContainer container = new SolrContainer(image);
        // Start the container. This step might take some time...
        container.start();
        return container;
    }

    private SolrClient createSolr9Client() {
        String baseSolrUrl = "http://" + container9.getHost() + ":" + container9.getSolrPort() + "/solr";
        log.info("Base Solr URL: {}", baseSolrUrl);
        return new Http2SolrClient.Builder(baseSolrUrl).build();
    }

    @Test
    void testSolr9Ping() throws SolrServerException, IOException {
        SolrClient client = createSolr9Client();
        client.ping("dummy");
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
            JsonToSolrDoc jsonToSolrDoc = new JsonToSolrDoc();
            Collection<SolrInputDocument> docs = jsonToSolrDoc.parseSolrDocuments(solrDocs).getDocs();
            SolrClient solrClient = createSolr9Client();
            uploadConfigSet(solrClient);
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

    private void uploadConfigSet(SolrClient client) throws SolrServerException, IOException {
        ConfigSetAdminRequest.Upload request = new ConfigSetAdminRequest.Upload();
        request.setConfigSetName("semantic_simple");
        Optional<URL> resource = resourceLoader.getResource("classpath:semantic_example.zip");
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

}

