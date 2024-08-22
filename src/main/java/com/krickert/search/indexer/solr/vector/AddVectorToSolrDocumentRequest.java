package com.krickert.search.indexer.solr.vector;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.solr.client.solrj.beans.Field;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import jakarta.inject.Named;

import java.util.Collection;
import java.util.Date;

@Named
@Introspected
public class AddVectorToSolrDocumentRequest {

    @Field
    @JsonProperty("docId")
    @NonNull
    private String docId;

    @Field
    @JsonProperty("parentId")
    @NonNull
    private String parentId;

    @Field
    @JsonProperty("chunk")
    @NonNull
    private String chunk;

    @Field
    @JsonProperty("chunkNumber")
    @NonNull
    private Integer chunkNumber;

    @Field
    @JsonProperty("vector")
    @NonNull
    private Collection<Float> vector;

    @Field
    @JsonProperty("parentFieldName")
    @NonNull
    private String parentFieldName;

    @Field
    @JsonProperty("crawlId")
    @NonNull
    private String crawlId;

    @Field
    @JsonProperty("dateCreated")
    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Date dateCreated;

    @Field
    @JsonProperty("parentCollection")
    @NonNull
    private String parentCollection;

    // Default constructor
    public AddVectorToSolrDocumentRequest() {
    }

    // Parameterized constructor
    public AddVectorToSolrDocumentRequest(
            String docId,
            String parentId,
            String chunk,
            Integer chunkNumber,
            Collection<Float> vector,
            String parentFieldName,
            String crawlId,
            Date dateCreated,
            String parentCollection) {
        this.docId = docId;
        this.parentId = parentId;
        this.chunk = chunk;
        this.chunkNumber = chunkNumber;
        this.vector = vector;
        this.parentFieldName = parentFieldName;
        this.crawlId = crawlId;
        this.dateCreated = dateCreated;
        this.parentCollection = parentCollection;
    }

    // Getters and Setters
    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getChunk() {
        return chunk;
    }

    public void setChunk(String chunk) {
        this.chunk = chunk;
    }

    public Integer getChunkNumber() {
        return chunkNumber;
    }

    public void setChunkNumber(Integer chunkNumber) {
        this.chunkNumber = chunkNumber;
    }

    public Collection<Float> getVector() {
        return vector;
    }

    public void setVector(Collection<Float> vector) {
        this.vector = vector;
    }

    public String getParentFieldName() {
        return parentFieldName;
    }

    public void setParentFieldName(String parentFieldName) {
        this.parentFieldName = parentFieldName;
    }

    public String getCrawlId() {
        return crawlId;
    }

    public void setCrawlId(String crawlId) {
        this.crawlId = crawlId;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getParentCollection() {
        return parentCollection;
    }

    public void setParentCollection(String parentCollection) {
        this.parentCollection = parentCollection;
    }
}