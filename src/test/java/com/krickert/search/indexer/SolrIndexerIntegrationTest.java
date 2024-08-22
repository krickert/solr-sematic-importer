package com.krickert.search.indexer;

import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.enhancers.ProtobufToSolrDocument;
import com.krickert.search.indexer.grpc.ClientGrpcTestContainers;
import com.krickert.search.indexer.solr.SolrDocumentConverter;
import com.krickert.search.indexer.solr.SolrTestContainers;
import com.krickert.search.model.pipe.PipeDocument;
import com.krickert.search.model.test.util.TestDataHelper;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.SolrContainer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import static org.mockito.Mockito.*;

@MicronautTest
public class SolrIndexerIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(SolrIndexerIntegrationTest.class);


    private final ClientGrpcTestContainers clientGrpcTestContainers;
    private final SolrDynamicClient solrDynamicClient;
    private final ResourceLoader resourceLoader;
    private SemanticIndexer semanticIndexer;
    private final IndexerConfiguration indexerConfiguration;
    private final SolrContainer container7;
    private final SolrContainer container9;
    private final ProtobufToSolrDocument protobufToSolrDocument = new ProtobufToSolrDocument();

    @BeforeEach
    void setUp() {
        clientGrpcTestContainers.getContainers().forEach(container -> log.info("Container {}: {}", container.getContainerName(), container.getHost()));
        // Mock the behavior of createSolr9Client
        // Assuming createSolr9Client is a method in SemanticIndexer

        // Partial mock of the semanticIndexer to spy on it
        semanticIndexer = spy(semanticIndexer);
        SolrClient solr9Client = createSolr9Client();
        // Override the createSolr9Client method
    }

    @Inject
    public SolrIndexerIntegrationTest(
            ClientGrpcTestContainers clientGrpcTestContainers, SolrDynamicClient solrDynamicClient,
            ResourceLoader resourceLoader,
            SemanticIndexer semanticIndexer,
            IndexerConfiguration indexerConfiguration,
            SolrTestContainers solrTestContainers) {
        log.info("Solr Test Containers: {} ", solrTestContainers);
        this.clientGrpcTestContainers = clientGrpcTestContainers;
        this.solrDynamicClient = solrDynamicClient;
        this.resourceLoader = resourceLoader;
        this.semanticIndexer = semanticIndexer;
        this.indexerConfiguration = indexerConfiguration;
        this.container7 = solrTestContainers.getContainer7();
        this.container9 = solrTestContainers.getContainer9();
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
                indexerConfiguration.getSourceSolrConfiguration().getCollection(),
                indexerConfiguration.getDestinationSolrConfiguration().getCollection(),
                5);

        //let's reindex everything - see if it works or messes up
        semanticIndexer.exportSolrDocsFromExternalSolrCollection(
                getSolr7Url(),
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
        log.info("Solr protocol buffer documents have been imported to Solr 7.  We are ready to start the test.");
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

