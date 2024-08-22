package com.krickert.search.indexer.config;

import io.micronaut.context.annotation.ConfigurationProperties;


@ConfigurationProperties("indexer")
public class IndexerConfigurationProperties {

    private String vectorGrpcChannel;
    private String chunkerGrpcChannel;
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
    public static class SourceSeedData {

        private boolean enabled;
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
    }

}