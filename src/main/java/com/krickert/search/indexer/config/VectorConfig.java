package com.krickert.search.indexer.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.core.annotation.Introspected;

@EachProperty("vector-config")
@Introspected
public class VectorConfig {

    private Integer chunkOverlap;
    private Integer chunkSize;
    private Boolean chunkField;
    private String model;
    private String destinationCollection;
    private String chunkFieldVectorName;
    private String similarityFunction;
    private Integer hnswMaxConnections;
    private Integer hnswBeamWidth;
    private VectorCollectionCreationConfig collectionCreation;

    public Integer getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(int chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }

    public Integer getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Boolean getChunkField() {
        return chunkField;
    }

    public void setChunkField(Boolean chunkField) {
        this.chunkField = chunkField;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getDestinationCollection() {
        return destinationCollection;
    }

    public void setDestinationCollection(String destinationCollection) {
        this.destinationCollection = destinationCollection;
    }
    public String getChunkFieldVectorName() {
        return chunkFieldVectorName;
    }

    public void setChunkFieldVectorName(String chunkFieldVectorName) {
        this.chunkFieldVectorName = chunkFieldVectorName;
    }

    public String getSimilarityFunction() {
        return similarityFunction;
    }
    public void setSimilarityFunction(String similarityFunction) {
        this.similarityFunction = similarityFunction;
    }
    public Integer getHnswMaxConnections() {
        return hnswMaxConnections;
    }
    public void setHnswMaxConnections(Integer hnswMaxConnections) {
        this.hnswMaxConnections = hnswMaxConnections;
    }
    public Integer getHnswBeamWidth() {
        return hnswBeamWidth;
    }
    public void setHnswBeamWidth(Integer hnswBeamWidth) {
        this.hnswBeamWidth = hnswBeamWidth;
    }

    public VectorCollectionCreationConfig getCollectionCreation() {
        return collectionCreation;
    }

    public void setCollectionCreation(VectorCollectionCreationConfig collectionCreation) {
        this.collectionCreation = collectionCreation;
    }

    @ConfigurationProperties("collection-creation")
    public static class VectorCollectionCreationConfig  {
        private String collectionConfigFile;
        private String collectionConfigName;
        private int numberOfShards;
        private int numberOfReplicas;

        public String getCollectionConfigFile() {
            return collectionConfigFile;
        }

        public void setCollectionConfigFile(String collectionConfigFile) {
            this.collectionConfigFile = collectionConfigFile;
        }

        public String getCollectionConfigName() {
            return collectionConfigName;
        }

        public void setCollectionConfigName(String collectionConfigName) {
            this.collectionConfigName = collectionConfigName;
        }

        public int getNumberOfShards() {
            return numberOfShards;
        }

        public void setNumberOfShards(int numberOfShards) {
            this.numberOfShards = numberOfShards;
        }

        public int getNumberOfReplicas() {
            return numberOfReplicas;
        }

        public void setNumberOfReplicas(int numberOfReplicas) {
            this.numberOfReplicas = numberOfReplicas;
        }
    }
}