package com.krickert.search.indexer.config;

import com.google.common.base.MoreObjects;
import io.micronaut.context.annotation.ConfigurationProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@ConfigurationProperties("indexer")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IndexerConfigurationProperties {

    @JsonProperty("vectorGrpcChannel")
    private String vectorGrpcChannel;

    @JsonProperty("chunkerGrpcChannel")
    private String chunkerGrpcChannel;

    @JsonProperty("sourceSeedData")
    private SourceSeedData sourceSeedData;

    // Getters and Setters
    public String getVectorGrpcChannel() {
        return vectorGrpcChannel;
    }

    public void setVectorGrpcChannel(String vectorGrpcChannel) {
        this.vectorGrpcChannel = vectorGrpcChannel;
    }

    public String getChunkerGrpcChannel() {
        return chunkerGrpcChannel;
    }

    public void setChunkerGrpcChannel(String chunkerGrpcChannel) {
        this.chunkerGrpcChannel = chunkerGrpcChannel;
    }

    public SourceSeedData getSourceSeedData() {
        return sourceSeedData;
    }

    public void setSourceSeedData(SourceSeedData sourceSeedData) {
        this.sourceSeedData = sourceSeedData;
    }

    @ConfigurationProperties("source-seed-data")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SourceSeedData {

        @JsonProperty("enabled")
        private boolean enabled;

        @JsonProperty("seedJsonFile")
        private String seedJsonFile;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSeedJsonFile() {
            return seedJsonFile;
        }

        public void setSeedJsonFile(String seedJsonFile) {
            this.seedJsonFile = seedJsonFile;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("enabled", enabled)
                    .add("seedJsonFile", seedJsonFile)
                    .toString();
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("vectorGrpcChannel", vectorGrpcChannel)
                .add("chunkerGrpcChannel", chunkerGrpcChannel)
                .add("sourceSeedData", sourceSeedData)
                .toString();
    }
}