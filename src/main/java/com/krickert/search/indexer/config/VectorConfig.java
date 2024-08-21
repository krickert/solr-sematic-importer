package com.krickert.search.indexer.config;

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
    private Boolean destinationCollectionCreate;
    private String destinationCollectionVectorFieldName;

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

    public Boolean isDestinationCollectionCreate() {
        return destinationCollectionCreate;
    }

    public void setDestinationCollectionCreate(boolean destinationCollectionCreate) {
        this.destinationCollectionCreate = destinationCollectionCreate;
    }

    public String getDestinationCollectionVectorFieldName() {
        return destinationCollectionVectorFieldName;
    }

    public void setDestinationCollectionVectorFieldName(String destinationCollectionVectorFieldName) {
        this.destinationCollectionVectorFieldName = destinationCollectionVectorFieldName;
    }
}