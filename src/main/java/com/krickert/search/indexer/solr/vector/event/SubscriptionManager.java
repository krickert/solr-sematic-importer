package com.krickert.search.indexer.solr.vector.event;

import jakarta.inject.Singleton;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Singleton
public class SubscriptionManager {

    private final SolrSourceDocumentPublisher solrSourceDocumentPublisher;
    private final List<DocumentListener> documentListeners;

    public SubscriptionManager(SolrSourceDocumentPublisher solrSourceDocumentPublisher, List<DocumentListener> documentListeners) {
        this.solrSourceDocumentPublisher = solrSourceDocumentPublisher;
        this.documentListeners = documentListeners;
        subscribeListeners();
    }

    private void subscribeListeners() {
        documentListeners.forEach(listener ->
            solrSourceDocumentPublisher.getDocumentFlux()
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .doOnNext(listener::processDocument)
                .subscribe());
    }
}