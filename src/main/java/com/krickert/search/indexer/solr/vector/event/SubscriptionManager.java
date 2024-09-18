package com.krickert.search.indexer.solr.vector.event;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Singleton
public class SubscriptionManager {
    private static final Logger log = LoggerFactory.getLogger(SubscriptionManager.class);

    private final SolrSourceDocumentPublisher solrSourceDocumentPublisher;
    private final List<DocumentListener> documentListeners;

    public SubscriptionManager(SolrSourceDocumentPublisher solrSourceDocumentPublisher, List<DocumentListener> documentListeners) {
        this.solrSourceDocumentPublisher = solrSourceDocumentPublisher;
        this.documentListeners = documentListeners;
        subscribeListeners();
    }

    private void subscribeListeners() {
        documentListeners.forEach(listener -> {
            solrSourceDocumentPublisher.getDocumentFlux()
                    .parallel()
                    .runOn(Schedulers.boundedElastic())
                    .doOnNext(document -> {
                        try {
                            listener.processDocument(document);
                        } catch (Exception e) {
                            log.error("Error processing document: {}", document.getFieldValue("id"), e);
                        }
                    })
                    .doOnError(throwable -> log.error("Error in Flux pipeline: ", throwable))
                    .subscribe();
        });
    }
}