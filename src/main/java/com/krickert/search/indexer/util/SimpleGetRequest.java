package com.krickert.search.indexer.util;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import jakarta.inject.Singleton;

@Singleton
public class SimpleGetRequest {

    private final HttpClient httpClient;

    public SimpleGetRequest(@Client HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String getResponseAsString(String url) {
        return httpClient.toBlocking().retrieve(HttpRequest.GET(url));
    }
}