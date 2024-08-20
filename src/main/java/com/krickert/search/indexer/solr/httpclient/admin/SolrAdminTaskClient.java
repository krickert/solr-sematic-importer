package com.krickert.search.indexer.solr.httpclient.admin;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.multipart.MultipartBody;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

@Client("/solr")
public abstract class SolrAdminTaskClient {
    private static final Logger log = LoggerFactory.getLogger(SolrAdminTaskClient.class);

    @Inject
    HttpClient httpClient;

    @Put("/admin/collections?action=CREATE&name={name}&numShards={numShards}&replicationFactor={replicationFactor}&configName={configName}")
    public abstract HttpResponse<String> createCollection(String name, int numShards, int replicationFactor, String configName);

    @Post("/admin/configs?action=UPLOAD")
    public HttpResponse<String> uploadConfiguration(String configName, @Body InputStream configZipStream) {
        log.info("Uplaoding config {} ...", configName);
        MultipartBody requestBody = MultipartBody.builder()
                .addPart("name", configName)
                .addPart("file", "config.zip")
                .build();

        return httpClient.toBlocking()
                .exchange(HttpRequest.POST("/admin/configs?action=UPLOAD", requestBody)
                        .contentType(MediaType.MULTIPART_FORM_DATA), String.class);
    }

    @Post("/{collection}/schema")
    public HttpResponse<String> addField(String collection, @Body SolrSchemaRequest addSchemaRequest) {
        log.info("Adding field {} to collection {} ...", addSchemaRequest, collection);
        return httpClient.toBlocking()
                .exchange(HttpRequest.POST("/{collection}/schema", addSchemaRequest).contentType(MediaType.APPLICATION_JSON), String.class);
    }

    @Post("/{collection}/schema")
    public HttpResponse<String> addVectorField(String collection, @Body AddVectorFieldRequest addVectorFieldRequest) {
        log.info("Adding vector field {} to collection {} ...", addVectorFieldRequest, collection);
        return addField(collection, addVectorFieldRequest);
    }

    @Post("/{collection}/schema")
    public HttpResponse<String> addVectorTypeField(String collection, @Body AddVectorTypeFieldRequest addVectorTypeFieldRequest) {
        log.info("Adding vector type field {} to collection {} ...", addVectorTypeFieldRequest, collection);
        return addField(collection, addVectorTypeFieldRequest);
    }

    @Delete("/admin/collections?action=DELETE&name={name}")
    public abstract HttpResponse<String> deleteCollection(String name);

    @Get("/{collection}/update?commit=true")
    public abstract HttpResponse<String> commit(String collection);

    @Get("/{collection}/admin/ping")
    public abstract HttpResponse<String> ping(String collection);
}