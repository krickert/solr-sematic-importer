package com.krickert.search.indexer.config;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigTestHelpers {
    protected static void testConnectionString(SolrConfiguration.Connection sourceConnection) {
        String connectionUrl = sourceConnection.getUrl();
        assertNotNull(connectionUrl);
        assertTrue(connectionUrl.startsWith("http://localhost:"));
        assertTrue(connectionUrl.endsWith("/solr"));
        String port = sourceConnection.getUrl().replaceAll(".*:(\\d+).*", "$1");
        assertEquals("http://localhost:" + Integer.parseInt(port) + "/solr", connectionUrl);
    }
}
