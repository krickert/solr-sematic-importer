package com.krickert.search.indexer.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.serde.annotation.Serdeable;

@ConfigurationProperties("indexer")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Serdeable
public class IndexerConfigurationProperties {

    @JsonProperty("vector-grpc-channel")
    private String vectorGrpcChannel;

    @JsonProperty("chunker-grpc-channel")
    private String chunkerGrpcChannel;

    @JsonProperty("source-seed-data")
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
    @Serdeable
    public static class SourceSeedData {

        @JsonProperty("enabled")
        private boolean enabled;

        @JsonProperty("seed-json-file")
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