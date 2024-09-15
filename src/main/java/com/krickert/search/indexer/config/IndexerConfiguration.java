package com.krickert.search.indexer.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
@JsonInclude(JsonInclude.Include.NON_NULL)
@Serdeable
public class IndexerConfiguration {

    @JsonProperty("name")
    private String name = "default";

    @JsonProperty("indexer_config")
    private final IndexerConfigurationProperties indexerConfigurationProperties;

    @JsonProperty("solr_config")
    private final Map<String, SolrConfiguration> solrConfiguration;

    @JsonProperty("vector_config")
    private final Map<String, VectorConfig> vectorConfig;

    private final Map<String, VectorConfig> inlineVectorConfig;

    private final Map<String, VectorConfig> chunkVectorConfig;

    @Inject
    public IndexerConfiguration(
            @JsonProperty("indexerConfigurationProperties") IndexerConfigurationProperties indexerConfigurationProperties,
            Collection<SolrConfiguration> solrConfigurations,
            @JsonProperty("vectorConfig") Map<String, VectorConfig> vectorConfig
    ) {
        this.indexerConfigurationProperties = indexerConfigurationProperties;
        this.vectorConfig = checkNotNull(vectorConfig);
        this.chunkVectorConfig = vectorConfig.entrySet().stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getValue().getChunkField())) // Handles nulls by treating them as false
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.inlineVectorConfig = vectorConfig.entrySet().stream()
                .filter(entry -> Boolean.FALSE.equals(entry.getValue().getChunkField())) // Handles nulls by treating them as false
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, VectorConfig> getInlineVectorConfig() {
        return inlineVectorConfig;
    }

    public Map<String, VectorConfig> getChunkVectorConfig() {
        return chunkVectorConfig;
    }
}