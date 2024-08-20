package com.krickert.search.indexer.enhancers;

import com.krickert.search.indexer.solr.httpclient.select.HttpSolrSelectClient;

import java.io.IOException;

public class MockSolrSelectClient implements HttpSolrSelectClient {
    @Override
    public String getSolrDocs(String solrHost, String solrCollection, Integer paginationSize, Integer pageNumber) {
        return "";
    }

    @Override
    public String getSolrDocs(Integer paginationSize, Integer pageNumber) throws IOException, InterruptedException {
        return "";
    }
}
