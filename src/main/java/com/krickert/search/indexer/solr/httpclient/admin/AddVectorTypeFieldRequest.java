package com.krickert.search.indexer.solr.httpclient.admin;

    import java.util.Map;

    public record AddVectorTypeFieldRequest(Map<String, VectorFieldType> addFieldType) implements SolrSchemaRequest {
        public static record VectorFieldType(String name, String clazz, String vectorDimension) {}
    }