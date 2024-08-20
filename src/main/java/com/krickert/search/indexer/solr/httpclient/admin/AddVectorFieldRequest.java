package com.krickert.search.indexer.solr.httpclient.admin;

    import java.util.Map;

    public record AddVectorFieldRequest(Map<String, VectorField> addField) implements SolrSchemaRequest {
        public static record VectorField(String name, String type, String vectorFormat) {}
    }