package com.krickert.search.indexer.solr.vector.event;

import org.apache.solr.common.SolrInputDocument;

public interface DocumentListener {
    void processDocument(SolrInputDocument document);
}