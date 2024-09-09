package com.krickert.search.indexer;

import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.enhancers.ProtobufToSolrDocument;
import com.krickert.search.indexer.grpc.ClientGrpcTestContainers;
import com.krickert.search.indexer.solr.SolrDocumentConverter;
import com.krickert.search.indexer.solr.SolrTestContainers;
import com.krickert.search.model.pipe.PipeDocument;
import com.krickert.search.model.test.util.TestDataHelper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@MicronautTest
public class SolrIndexerIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(SolrIndexerIntegrationTest.class);


    private final ClientGrpcTestContainers clientGrpcTestContainers;
    private final SolrDynamicClient solrDynamicClient;
    private final SemanticIndexer semanticIndexer;
    private final IndexerConfiguration indexerConfiguration;
    private final ProtobufToSolrDocument protobufToSolrDocument = new ProtobufToSolrDocument();

    @BeforeEach
    void setUp() {
        clientGrpcTestContainers.getContainers().forEach(container -> log.info("Container {}: {}", container.getContainerName(), container.getHost()));
    }

    @Inject
    public SolrIndexerIntegrationTest(
            ClientGrpcTestContainers clientGrpcTestContainers, SolrDynamicClient solrDynamicClient,
            SemanticIndexer semanticIndexer,
            IndexerConfiguration indexerConfiguration,
            SolrTestContainers solrTestContainers) {
        log.info("Solr Test Containers: {} ", solrTestContainers);
        this.clientGrpcTestContainers = clientGrpcTestContainers;
        this.solrDynamicClient = solrDynamicClient;
        this.semanticIndexer = semanticIndexer;
        this.indexerConfiguration = indexerConfiguration;
        log.info("Indexer configuration: {}", indexerConfiguration);
        log.info("solrTestContainers: {}", solrTestContainers);
    }


    @Test
    void testSemanticIndexer() {
        //this would just run, but we first have to setup the source and destination solr
        setupSolr7ForExportTest();
        semanticIndexer.runExportJob(indexerConfiguration);

        //let's reindex everything - see if it works or messes up
        semanticIndexer.runExportJob(indexerConfiguration);
    }

    private void setupSolr7ForExportTest() {
        String solrUrl = indexerConfiguration.getSourceSolrConfiguration().getConnection().getUrl();
        String collection = indexerConfiguration.getSourceSolrConfiguration().getCollection();
        log.info("Solr source collection: {} being created from the solr host: {}", collection, solrUrl);
        solrDynamicClient.createCollection(solrUrl, collection);
        log.info("Solr source collection created: {}", collection);
        Collection<PipeDocument> protos = TestDataHelper.getFewHunderedPipeDocuments().stream().filter(doc -> doc.getDocumentType().equals("ARTICLE")).toList();
        List<SolrInputDocument> solrDocuments = protos.stream().map(protobufToSolrDocument::convertProtobufToSolrDocument).collect(Collectors.toList());
        solrDynamicClient.sendJsonToSolr(solrUrl, collection, SolrDocumentConverter.convertSolrDocumentsToJson(solrDocuments));
        solrDynamicClient.commit(solrUrl, collection);
        log.info("Solr protocol buffer documents have been imported to Solr 7.  We are ready to start the test.");
    }


}

