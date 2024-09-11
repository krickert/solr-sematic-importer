package com.krickert.search.indexer.solr;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krickert.search.indexer.solr.httpclient.select.HttpSolrSelectResponse;
import jakarta.inject.Singleton;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Singleton
public class JsonToSolrDocParser {
    private static final Logger log = LoggerFactory.getLogger(JsonToSolrDocParser.class);

    public JsonToSolrDocParser() {
        log.info("Created JsonToSolrDocParser");
    }

    ObjectMapper mapper = new ObjectMapper();

    public Collection<String> parseSolrDocumentsToJSON(String jsonString) {
        Map<String, Object> map;
        try {
            map = mapper.readValue(jsonString, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) map.get("response");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> docs = (List<Map<String, Object>>) response.get("docs");

        List<String> jsonDocuments = new ArrayList<>();
        for (Map<String, Object> doc : docs) {
            try {
                doc.remove("_version_");
                jsonDocuments.add(mapper.writeValueAsString(doc));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        return jsonDocuments;
    }

    public HttpSolrSelectResponse parseSolrDocuments(String jsonString) {
        Map<String, Object> map;
        try {
            map = mapper.readValue(jsonString, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) map.get("response");
        @SuppressWarnings("unchecked")
        Map<String, Object> responseHeader = (Map<String, Object>) map.get("responseHeader");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> docs = (List<Map<String, Object>>) response.get("docs");

        List<SolrInputDocument> solrDocuments = new ArrayList<>();
        for (Map<String, Object> doc : docs) {
            SolrInputDocument solrDoc = new SolrInputDocument();
            doc.forEach((k, v) -> {
                SolrInputField inputField = new SolrInputField(k);
                inputField.setValue(v);
                solrDoc.put(k, inputField);
            });
            solrDoc.remove("_version_");
            solrDocuments.add(solrDoc);
        }

        Long numFound = Long.parseLong(response.get("numFound").toString());
        Long qtime = Long.parseLong(responseHeader.get("QTime").toString());
        Long start = Long.parseLong(response.get("start").toString());

        // Assuming pageSize is not available in the json response
        Long pageSize = null;

        // Build the response object

        return new HttpSolrSelectResponse.Builder()
                .numFound(numFound)
                .qtime(qtime)
                .start(start)
                .docs(solrDocuments)
                .pageSize(pageSize)
                .build();
    }

    private final JsonFactory factory = new JsonFactory();

    public List<SolrInputDocument> parseSolrDocuments(InputStream inputStream) {
        List<SolrInputDocument> solrDocuments = new ArrayList<>();
        try (JsonParser parser = factory.createParser(inputStream)) {
            String currentField = null;
            while (!parser.isClosed()) {
                JsonToken token = parser.nextToken();
                if (token == JsonToken.FIELD_NAME) {
                    currentField = parser.currentName();
                } else if ("docs".equals(currentField) && token == JsonToken.START_ARRAY) {
                    // Start processing documents array
                    token = parser.nextToken();
                    while (token != JsonToken.END_ARRAY) {
                        SolrInputDocument solrDoc = parseSolrDocument(parser);
                        solrDoc.remove("_version_");
                        solrDocuments.add(solrDoc);
                        token = parser.nextToken();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing Solr documents", e);
            throw new RuntimeException(e);
        }
        return solrDocuments;
    }

    private SolrInputDocument parseSolrDocument(JsonParser parser) throws Exception {
        SolrInputDocument solrDoc = new SolrInputDocument();
        String fieldName = null;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            JsonToken token = parser.currentToken();
            if (token == JsonToken.FIELD_NAME) {
                fieldName = parser.currentName();
            } else if (token == JsonToken.VALUE_STRING) {
                solrDoc.addField(fieldName, parser.getValueAsString());
            } else if (token == JsonToken.VALUE_NUMBER_INT) {
                solrDoc.addField(fieldName, parser.getValueAsInt());
            } else if (token == JsonToken.VALUE_NUMBER_FLOAT) {
                solrDoc.addField(fieldName, parser.getValueAsDouble());
            } else if (token == JsonToken.VALUE_TRUE || token == JsonToken.VALUE_FALSE) {
                solrDoc.addField(fieldName, parser.getValueAsBoolean());
            } else if (token == JsonToken.START_ARRAY) {
                solrDoc.addField(fieldName, parseArray(parser));
            } else {
                log.warn("Unhandled token type: {}", token);
            }
        }
        return solrDoc;
    }

    private List<Object> parseArray(JsonParser parser) throws Exception {
        List<Object> list = new ArrayList<>();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            JsonToken token = parser.currentToken();
            if (token == JsonToken.VALUE_STRING) {
                list.add(parser.getValueAsString());
            } else if (token == JsonToken.VALUE_NUMBER_INT) {
                list.add(parser.getValueAsInt());
            } else if (token == JsonToken.VALUE_NUMBER_FLOAT) {
                list.add(parser.getValueAsDouble());
            } else if (token == JsonToken.VALUE_TRUE || token == JsonToken.VALUE_FALSE) {
                list.add(parser.getValueAsBoolean());
            } else if (token == JsonToken.START_OBJECT) {
                list.add(parseSolrDocument(parser));
            } else if (token == JsonToken.START_ARRAY) {
                list.add(parseArray(parser));
            } else {
                log.warn("Unhandled token type in array: {}", token);
            }
        }
        return list;
    }
}