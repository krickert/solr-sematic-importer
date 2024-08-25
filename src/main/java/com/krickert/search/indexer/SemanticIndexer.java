package com.krickert.search.indexer;

import com.krickert.search.indexer.config.IndexerConfiguration;

public interface SemanticIndexer {

    void runDefaultExportJob();

    void runExportJob(IndexerConfiguration indexerConfiguration);
}
