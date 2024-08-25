package com.krickert.search.indexer.solr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.solr.common.SolrInputDocument;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class SolrDocumentConverter {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(SerializationFeature.INDENT_OUTPUT, true);

    private static Map<String, Object> convertSolrDocumentToMap(SolrInputDocument solrInputDocument) {
        return solrInputDocument.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getValue()));
    }

    public static String convertSolrDocumentsToJson(List<SolrInputDocument> solrDocuments) {
        List<Map<String, Object>> documents = solrDocuments.stream()
                .map(SolrDocumentConverter::convertSolrDocumentToMap)
                .collect(Collectors.toList());
        try {
            return objectMapper.writeValueAsString(documents);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
