package com.krickert.search.indexer.solr.seed;

import com.krickert.search.indexer.SolrDynamicClient;
import com.krickert.search.indexer.enhancers.ProtobufToSolrDocument;
import com.krickert.search.indexer.solr.SolrDocumentConverter;
import com.krickert.search.model.pipe.PipeDocument;
import com.krickert.search.model.test.util.TestDataHelper;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.http.client.HttpClient;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jakarta.inject.Inject;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Requires(notEnv = Environment.TEST)
@MicronautTest(environments = "dev")
@Property(name = "micronaut.application.name", value = "dev-app")
@Property(name = "micronaut.config.files", value = "application-dev.yml")
public class SolrMicronautDevTest {

    private static final String SOLR_URL = "http://localhost:8980/solr";
    private static final String COLLECTION_NAME = "source_collection";

    @Inject
    HttpClient httpClient;

    @Inject
    ProtobufToSolrDocument protobufToSolrDocument;


    SolrClient solrClient = new Http2SolrClient.Builder(SOLR_URL).build();

    @BeforeEach
    public void seedSolrData() throws Exception {
        SolrDynamicClient solrDynamicClient = new SolrDynamicClient(httpClient);
        try (SolrClient solrClient = new Http2SolrClient.Builder(SOLR_URL).build()) {
            solrDynamicClient.createCollection(SOLR_URL, "source_collection");
            Collection<PipeDocument> protos = TestDataHelper.getFewHunderedPipeDocuments().stream().filter(doc -> doc.getDocumentType().equals("ARTICLE")).toList();
            List<SolrInputDocument> solrDocuments = protos.stream().map(protobufToSolrDocument::convertProtobufToSolrDocument).collect(Collectors.toList());
            solrDynamicClient.sendJsonToSolr(SOLR_URL, COLLECTION_NAME, SolrDocumentConverter.convertSolrDocumentsToJson(solrDocuments));
            solrDynamicClient.commit(SOLR_URL, COLLECTION_NAME);
            solrClient.commit(COLLECTION_NAME);
        }
    }

    @Test
    public void testSolrDataSeeding() throws Exception {
        long documentCount = solrClient.query(COLLECTION_NAME, new SolrQuery("*:*")).getResults().getNumFound();
        assertTrue(documentCount >= 2, "Expected at least 2 documents in Solr");
    }
}