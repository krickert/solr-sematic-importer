package com.krickert.search.indexer.solr.vector.event;

import jakarta.inject.Singleton;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;


@Singleton
public class SolrChunkDocumentPublisher implements SourceSolrDocumentListener {

    Logger log = org.slf4j.LoggerFactory.getLogger(SolrChunkDocumentPublisher.class);
    private final Sinks.Many<SolrInputDocument> sink;

    public SolrChunkDocumentPublisher() {
        this.sink = Sinks.many().unicast().onBackpressureBuffer();
    }

    @Override
    public void publishDocument(SolrInputDocument document) {
        Sinks.EmitResult result = this.sink.tryEmitNext(document);
        if (result.isFailure()) {
            log.warn("Failed to publish document: {}", document.getFieldValue("id"));
        }
    }

    @Override
    public Flux<SolrInputDocument> getDocumentFlux() {
        return sink.asFlux();
    }
}