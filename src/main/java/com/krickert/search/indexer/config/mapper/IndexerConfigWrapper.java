package com.krickert.search.indexer.config.mapper;

import com.krickert.search.indexer.config.IndexerConfigurationProperties;

public class IndexerConfigWrapper {

    private IndexerConfigurationProperties indexer;

    // Getters and Setters
    public IndexerConfigurationProperties getIndexer() {
        return indexer;
    }

    public void setIndexer(IndexerConfigurationProperties indexer) {
        this.indexer = indexer;
    }
}