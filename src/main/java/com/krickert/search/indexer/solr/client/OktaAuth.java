package com.krickert.search.indexer.solr.client;

import java.io.IOException;

public interface OktaAuth {
    String getAccessToken() throws IOException;
}
