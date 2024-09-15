package com.krickert.search.indexer;

import com.krickert.search.indexer.config.IndexerConfiguration;

public interface SemanticIndexer {

    void runDefaultExportJob() throws IndexingFailedExecption;

}
