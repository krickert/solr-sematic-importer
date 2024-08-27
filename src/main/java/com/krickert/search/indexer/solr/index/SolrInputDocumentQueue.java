package com.krickert.search.indexer.solr.index;

import com.krickert.search.indexer.dto.SolrDocumentType;
import com.krickert.search.indexer.tracker.IndexingTracker;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateHttp2SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class SolrInputDocumentQueue {

    private static final Logger log = LoggerFactory.getLogger(SolrInputDocumentQueue.class);

    private final ConcurrentUpdateHttp2SolrClient solrClient;
    private final IndexingTracker indexingTracker;

    @Inject
    public SolrInputDocumentQueue(ConcurrentUpdateHttp2SolrClient solrClient, IndexingTracker indexingTracker) {
        log.info("Creating SolrInputDocumentQueue");
        this.solrClient = solrClient;
        this.indexingTracker = indexingTracker;
        log.info("Created SolrInputDocumentQueue");
    }

    @Retryable(attempts = "5", delay = "500ms", includes = RuntimeException.class)
    public void addDocument(String collection, SolrInputDocument document, SolrDocumentType type) {
        try {
            solrClient.add(collection, document);
            indexingTracker.documentProcessed();
        } catch (SolrServerException | IOException e) {
            indexingTracker.documentFailed();
            log.error("Error adding document to Solr. Document ID: {}", document.getFieldValue("id"), e);
            throw new RuntimeException(e);
        }
    }

    @Retryable(attempts = "5", delay = "500ms", includes = RuntimeException.class)
    public void addDocuments(String collection, List<SolrInputDocument> documents, SolrDocumentType type) {
        try {
            solrClient.add(collection, documents);
            documents.forEach(doc -> indexingTracker.documentProcessed());
        } catch (SolrServerException | IOException e) {
            documents.forEach(doc -> indexingTracker.documentFailed());
            List<String> docIds = documents.stream().map(doc -> doc.getFieldValue("id").toString()).collect(Collectors.toList());
            log.error("Error adding documents to Solr. Document IDs: {}", docIds, e);
            throw new RuntimeException(e);
        }
    }

    @Retryable(attempts = "5", delay = "500ms", includes = RuntimeException.class)
    public void commit(String collection) {
        try {
            solrClient.commit(collection);
        } catch (SolrServerException | IOException e) {
            log.error("Error committing to Solr collection: {}", collection, e);
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            solrClient.close();
        } catch (Exception e) {
            log.error("Error closing solr client", e);
        }
    }
}