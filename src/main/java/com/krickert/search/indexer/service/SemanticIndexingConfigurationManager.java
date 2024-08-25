package com.krickert.search.indexer.service;

import com.google.common.collect.Maps;
import com.krickert.search.indexer.SemanticIndexer;

import java.util.Map;

public class SemanticIndexingConfigurationManager {

    private final Map<String, SemanticIndexer> indexerMap;

    private SemanticIndexingConfigurationManager() {
        this.indexerMap = Maps.newConcurrentMap();
    }

}
