package com.krickert.search.indexer.config;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class IndexerConfiguration {
    private final IndexerConfigurationProperties indexerConfigurationProperties;
    private final Map<String, SolrConfiguration> solrConfiguration;
    private final Map<String, VectorConfig> vectorConfig;

    @Inject
    public IndexerConfiguration(IndexerConfigurationProperties indexerConfigurationProperties, Collection<SolrConfiguration> solrConfigurations, Map<String, VectorConfig> vectorConfig) {
        this.indexerConfigurationProperties = indexerConfigurationProperties;
        this.vectorConfig = vectorConfig;
        this.solrConfiguration = solrConfigurations.stream()
                .collect(Collectors.toMap(SolrConfiguration::getName, solrConfiguration -> solrConfiguration));


    }
    public IndexerConfigurationProperties getIndexerConfigurationProperties() {
        return indexerConfigurationProperties;
    }

    public SolrConfiguration getSourceSolrConfiguration() {
        return solrConfiguration.get("source");
    }

    public SolrConfiguration getDestinationSolrConfiguration() {
        return solrConfiguration.get("destination");
    }

    public Map<String, VectorConfig> getVectorConfig() {
        return vectorConfig;
    }
}
