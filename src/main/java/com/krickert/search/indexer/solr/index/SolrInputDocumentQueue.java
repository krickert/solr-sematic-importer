package com.krickert.search.indexer.solr.index;

import io.grpc.StatusRuntimeException;
import io.micronaut.context.annotation.Value;
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

@Singleton
public class SolrInputDocumentQueue {
    private static final Logger log = LoggerFactory.getLogger(SolrInputDocumentQueue.class);
    private final ConcurrentUpdateHttp2SolrClient solrClient;

    @Inject
    public SolrInputDocumentQueue(ConcurrentUpdateHttp2SolrClient solrClient) {
        log.info("Creating SolrInputDocumentQueue");
        this.solrClient = solrClient;
        log.info("Created SolrInputDocumentQueue");
    }

    @Retryable(attempts = "5", delay = "500ms", includes = RuntimeException.class)
    public void addDocument(String collection, SolrInputDocument document) {
        try {
            solrClient.add(collection, document);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Retryable(attempts = "5", delay = "500ms", includes = RuntimeException.class)
    public void addDocuments(String collection, List<SolrInputDocument> documents) {
        try {
            solrClient.add(collection, documents);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Retryable(attempts = "5", delay = "500ms", includes = RuntimeException.class)
    public void addBeans(String collection, List<SolrInputDocument> documents) {
        try {
            solrClient.add(collection, documents);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Retryable(attempts = "5", delay = "500ms", includes = RuntimeException.class)
    public void addBean(String collection, Object bean) {
        try {
            solrClient.addBean(collection, bean);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Retryable(attempts = "5", delay = "500ms", includes = RuntimeException.class)
    public void commit(String collection) {
        try {
            solrClient.commit(collection);
        } catch (SolrServerException | IOException e) {
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
