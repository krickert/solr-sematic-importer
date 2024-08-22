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

    @Override
    public Long getTotalNumberOfDocumentsForCollection() {
        return 0L;
    }

    @Override
    public Long getTotalNumberOfDocumentsForCollection(String solr7Host, String solr7Collection) {
        return 0L;
    }
}
