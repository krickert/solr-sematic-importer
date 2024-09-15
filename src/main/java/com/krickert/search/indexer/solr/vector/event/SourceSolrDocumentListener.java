package com.krickert.search.indexer.solr.vector.event;

import org.apache.solr.common.SolrInputDocument;
import reactor.core.publisher.Flux;

public interface SourceSolrDocumentListener {
    void publishDocument(SolrInputDocument doc);
    Flux<SolrInputDocument> getDocumentFlux();

}
