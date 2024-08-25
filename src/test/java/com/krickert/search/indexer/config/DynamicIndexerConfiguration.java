package com.krickert.search.indexer.config;

import java.util.Collection;
import java.util.Map;

public class DynamicIndexerConfiguration extends IndexerConfiguration {
    public DynamicIndexerConfiguration(IndexerConfigurationProperties indexerConfigurationProperties, Collection<SolrConfiguration> solrConfigurations, Map<String, VectorConfig> vectorConfig) {
        super(indexerConfigurationProperties, solrConfigurations, vectorConfig);
    }

    public DynamicIndexerConfiguration(IndexerConfiguration indexerConfiguration) {
        super(indexerConfiguration.getIndexerConfigurationProperties(), indexerConfiguration.getSolrConfiguration().values(), indexerConfiguration.getVectorConfig());

    }
}
