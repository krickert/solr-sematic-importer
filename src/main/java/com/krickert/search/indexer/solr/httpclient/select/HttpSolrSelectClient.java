package com.krickert.search.indexer.solr.httpclient.select;

import java.io.IOException;

public interface HttpSolrSelectClient {
    String getSolrDocs(Integer paginationSize, Integer pageNumber) throws IOException, InterruptedException;
}
