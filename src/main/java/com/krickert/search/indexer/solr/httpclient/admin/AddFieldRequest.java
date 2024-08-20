package com.krickert.search.indexer.solr.httpclient.admin;

    import java.util.Map;

    public record AddFieldRequest(Map<String, Field> addField) implements SolrSchemaRequest {
        public static record Field(String name, String type) {}
    }