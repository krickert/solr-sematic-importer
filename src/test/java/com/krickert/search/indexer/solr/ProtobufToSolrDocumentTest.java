package com.krickert.search.indexer.solr;

import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.enhancers.ProtobufToSolrDocument;
import com.krickert.search.model.pipe.PipeDocument;
import com.krickert.search.model.test.util.TestDataHelper;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@MicronautTest
public class ProtobufToSolrDocumentTest {
    private static final Logger log = LoggerFactory.getLogger(ProtobufToSolrDocumentTest.class);

    final ProtobufToSolrDocument unit;
    private final Collection<PipeDocument> pipeDocumentCollection;
    private final SolrContainer container9;
    private ProtobufSolrIndexer protobufSolrIndexer;
    private final IndexerConfiguration indexerConfiguration;

    @BeforeEach
    void setUp() {
        // Mock the behavior of createSolr9Client
        // Assuming createSolr9Client is a method in SemanticIndexer

        // Partial mock of the semanticIndexer to spy on it
        protobufSolrIndexer = spy(protobufSolrIndexer);
        SolrClient solr9Client = createSolr9Client();
        // Override the createSolr9Client method
        doReturn(solr9Client).when(protobufSolrIndexer).createSolr9Client();
    }

    @Inject
    ProtobufToSolrDocumentTest(ProtobufToSolrDocument unit,
                               ResourceLoader resourceLoader,
                               ProtobufSolrIndexer protobufSolrIndexer,
                               IndexerConfiguration indexerConfiguration)
            throws SolrServerException, IOException {

        this.unit = unit;

        this.protobufSolrIndexer = protobufSolrIndexer;

        this.pipeDocumentCollection = TestDataHelper.getFewHunderedPipeDocuments();
        final DockerImageName solrImage = DockerImageName.parse("solr:9.6.1");
        this.container9 = createContainer(solrImage);

        String testCollection = indexerConfiguration.getDestinationSolrConfiguration().getCollection();

        try (SolrClient solrClient = createSolr9Client()) {
            solrClient.request(CollectionAdminRequest.createCollection(testCollection, "_default", 1, 1));
        }
        String solrDestinationUrl = "http://" + container9.getHost() + ":" + container9.getSolrPort() + "/solr";
        this.indexerConfiguration = indexerConfiguration;
    }

    @Test
    void testConversionOfPipeDocuments() {
        List<SolrInputDocument> solrDocuments = pipeDocumentCollection.stream()
                .map(unit::convertProtobufToSolrDocument)
                .toList();
        solrDocuments.forEach(System.out::println);
        assertEquals(pipeDocumentCollection.size(), solrDocuments.size());
    }

    @Test
    void testInsertProtobufToSolrDocument() {
        protobufSolrIndexer.exportProtobufToSolr(new ArrayList<>(TestDataHelper.getFewHunderedPipeDocuments()));
        try (SolrClient solrClient = createSolr9Client()) {
            try {
                String testCollection = indexerConfiguration.getDestinationSolrConfiguration().getCollection();
                QueryResponse response = solrClient.query(testCollection, new SolrQuery("*:*"));
                assertEquals(TestDataHelper.getFewHunderedPipeDocuments().size(), response.getResults().getNumFound());
            } catch (SolrServerException | IOException e) {
                fail(e);
            }
        } catch (IOException e) {
            log.error("IOException while exporting protobuf to Solr", e);
            fail(e);
        }
    }

    private SolrClient createSolr9Client() {
        String baseSolrUrl = "http://" + container9.getHost() + ":" + container9.getSolrPort() + "/solr";
        log.info("Base Solr URL: {}", baseSolrUrl);
        return new Http2SolrClient.Builder(baseSolrUrl).build();
    }
    private SolrContainer createContainer(DockerImageName image) {
        // Create the solr container.
        SolrContainer container = new SolrContainer(image);
        // Start the container. This step might take some time...
        container.start();
        return container;
    }
}
