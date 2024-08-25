package com.krickert.search.indexer.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IndexerConfiguration {

    @JsonProperty("indexerConfigurationProperties")
    private final IndexerConfigurationProperties indexerConfigurationProperties;

    @JsonProperty("solrConfiguration")
    private final Map<String, SolrConfiguration> solrConfiguration;

    @JsonProperty("vectorConfig")
    private final Map<String, VectorConfig> vectorConfig;

    @Inject
    public IndexerConfiguration(
            @JsonProperty("indexerConfigurationProperties") IndexerConfigurationProperties indexerConfigurationProperties,
            Collection<SolrConfiguration> solrConfigurations,
            @JsonProperty("vectorConfig") Map<String, VectorConfig> vectorConfig
    ) {
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

    protected Map<String, SolrConfiguration> getSolrConfiguration() {
        return solrConfiguration;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("indexerConfigurationProperties", indexerConfigurationProperties)
                .add("solrConfiguration", solrConfiguration)
                .add("vectorConfig", vectorConfig)
                .toString();
    }
}