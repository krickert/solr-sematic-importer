package com.krickert.search.indexer.solr.vector.event;

import org.apache.solr.common.SolrInputDocument;
import jakarta.inject.Singleton;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;


@Singleton
public class SolrSourceDocumentPublisher implements SourceSolrDocumentListener {

    private final Sinks.Many<SolrInputDocument> sink;

    public SolrSourceDocumentPublisher() {
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
    }

    @Override
    public void publishDocument(SolrInputDocument document) {
        this.sink.tryEmitNext(document);
    }

    @Override
    public Flux<SolrInputDocument> getDocumentFlux() {
        return sink.asFlux();
    }
}