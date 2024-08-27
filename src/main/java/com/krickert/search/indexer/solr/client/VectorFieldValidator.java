package com.krickert.search.indexer.solr.client;

import com.google.common.collect.Sets;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.schema.FieldTypeRepresentation;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public class VectorFieldValidator {

    private static final Logger log = LoggerFactory.getLogger(VectorFieldValidator.class);
    private final SolrClient solrClient;
    private final static Set<String> validValues = Sets.newHashSet("euclidean", "dot_product", "cosine");

    public VectorFieldValidator(
            @Named("solrClient") SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    public String validateVectorField(
            String vectorFieldName,
            String similarityFunction,
            Integer hnswMaxConnections,
            Integer hnswBeamWidth,
            Integer dimensionality,
            String collection) throws IOException, SolrServerException {
        String fieldNameCreated = vectorFieldName;
        String vectorFieldType = fieldNameCreated + "_" + dimensionality;

        // Looking in the collection to see if it's been created
        FieldTypeRepresentation fieldTypeRepresentation = getFieldTypeByName(vectorFieldType, collection);

        if (fieldTypeRepresentation != null) {
            validateFieldTypeAttributes(similarityFunction, hnswMaxConnections, hnswBeamWidth, dimensionality, fieldTypeRepresentation, vectorFieldType);
        } else {
            createDenseVectorFieldType(vectorFieldType, dimensionality, similarityFunction, hnswMaxConnections, hnswBeamWidth, collection);
            fieldTypeRepresentation = getFieldTypeByName(vectorFieldType, collection);
            if (fieldTypeRepresentation == null) {
                throw new IllegalStateException("Field type " + vectorFieldType + " was not created.");
            }
            validateFieldTypeAttributes(similarityFunction, hnswMaxConnections, hnswBeamWidth, dimensionality, fieldTypeRepresentation, vectorFieldType);
        }

        try {
            boolean fieldExists = checkFieldExists(fieldNameCreated, collection, dimensionality, similarityFunction);
            if (!fieldExists) {
                createDenseVectorField(fieldNameCreated, vectorFieldType, collection);
            } else {
                log.info("Field {} exists. No need to create it.", fieldNameCreated);
            }
        } catch (IOException | SolrServerException e) {
            throw new RuntimeException(e);
        } catch (FieldCreationExistsAttributeMismatchException e) {
            if (StringUtils.isEmpty(similarityFunction)) {
                similarityFunction = "cosine";
                log.info("Retrying with similarity function: {}", similarityFunction);
                createDenseVectorFieldType(vectorFieldType, dimensionality, similarityFunction, hnswMaxConnections, hnswBeamWidth, collection);
            }
            fieldNameCreated = vectorFieldName + "_" + similarityFunction + "_" + dimensionality;
            createDenseVectorField(fieldNameCreated, vectorFieldType, collection);
        }

        return fieldNameCreated;
    }

    private void validateFieldTypeAttributes(
            String similarityFunction, Integer hnswMaxConnections, Integer hnswBeamWidth, Integer dimensionality,
            FieldTypeRepresentation fieldTypeRepresentation, String vectorFieldType) 
    {
        Map<String, Object> attributes = fieldTypeRepresentation.getAttributes();
        String fieldTypeClass = (String) attributes.get("class");
        if (!"solr.DenseVectorField".equals(fieldTypeClass)) {
            throw new IllegalArgumentException("The field type " + vectorFieldType + " is not a dense vector field. Check the configuration.");
        }

        validateAttribute("vectorDimension", attributes.get("vectorDimension"), dimensionality, vectorFieldType);

        if (similarityFunction != null) {
            validateAttribute("similarityFunction", attributes.get("similarityFunction"), similarityFunction, vectorFieldType);
        }
        if (hnswMaxConnections != null) {
            validateAttribute("hnswMaxConnections", attributes.get("hnswMaxConnections"), hnswMaxConnections, vectorFieldType);
        }
        if (hnswBeamWidth != null) {
            validateAttribute("hnswBeamWidth", attributes.get("hnswBeamWidth"), hnswBeamWidth, vectorFieldType);
        }
    }

    private void validateAttribute(String attributeName, Object actualValue, Object expectedValue, String fieldType) {
        if (expectedValue != null && !expectedValue.toString().equals(actualValue)) {
            throw new IllegalArgumentException("The field type " + fieldType + " has a " + attributeName + " of " + actualValue + " but the provided " + attributeName + " is " + expectedValue + ". Check the configuration.");
        }
    }

    private FieldTypeRepresentation getFieldTypeByName(String fieldTypeName, String collectionName) throws IOException, SolrServerException {
        SchemaRequest.FieldTypes fieldTypesRequest = new SchemaRequest.FieldTypes();
        SchemaResponse.FieldTypesResponse fieldTypesResponse = fieldTypesRequest.process(solrClient, collectionName);
        List<FieldTypeRepresentation> fieldTypes = fieldTypesResponse.getFieldTypes();

        for (FieldTypeRepresentation fieldTypeInfo : fieldTypes) {
            Map<String, Object> attributes = fieldTypeInfo.getAttributes();
            String name = (String) attributes.get("name");
            if (fieldTypeName.equals(name)) {
                return fieldTypeInfo;
            }
        }
        return null;
    }

    private boolean checkFieldExists(String fieldName, String collectionName, Integer targetDimensionality, String targetSimilarityFunction) throws IOException, SolrServerException, FieldCreationExistsAttributeMismatchException {
        SchemaRequest.Fields fieldsRequest = new SchemaRequest.Fields();
        SchemaResponse.FieldsResponse fieldsResponse = fieldsRequest.process(solrClient, collectionName);
        List<Map<String, Object>> fields = fieldsResponse.getFields();
        for (Map<String, Object> fieldInfo : fields) {
            if (fieldName.equals(fieldInfo.get("name"))) {
                String fieldTypeName = (String) fieldInfo.get("type");
                FieldTypeRepresentation fieldTypeInfo = getFieldTypeByName(fieldTypeName, collectionName);
                String dimensionality = (String) fieldTypeInfo.getAttributes().get("vectorDimension");
                String similarityFunction = (String) fieldTypeInfo.getAttributes().get("similarityFunction");
                boolean throwException = false;
                StringBuilder exceptionMessage = new StringBuilder();
                if (targetDimensionality != null && !dimensionality.equals(targetDimensionality.toString())) {
                    throwException = true;
                    exceptionMessage.append(String.format("Collection [%s] has Field [%s] with mismatched dimensionality [%s vs %s].", collectionName, fieldName, targetDimensionality, dimensionality));
                } else if (dimensionality == null) {
                    throwException = true;
                    exceptionMessage.append(String.format("Field in collection [%s] has no dimensionality [%s vs %s].", collectionName, fieldName, targetDimensionality));
                }
                if (targetSimilarityFunction != null && !similarityFunction.equals(targetSimilarityFunction)) {
                    throwException = true;
                    exceptionMessage.append(String.format("Collection [%s] has Field [%s] with mismatched similarity function [%s vs %s].", collectionName, fieldName, targetSimilarityFunction, similarityFunction));
                } else if (similarityFunction == null) {
                    throwException = true;
                    exceptionMessage.append(String.format("Field in collection [%s] has no similarity function [%s vs %s].", collectionName, fieldName, targetSimilarityFunction));
                }
                if (throwException) {
                    throw new FieldCreationExistsAttributeMismatchException(exceptionMessage.toString());
                }
                return true;
            }
        }
        return false;
    }

    private void createDenseVectorFieldType(
            String fieldType, Integer dimensionality, String similarityFunction, Integer hnswMaxConnections,
            Integer hnswBeamWidth, String collectionName) throws IOException, SolrServerException 
    {
        Map<String, Object> fieldTypeAttributes = new HashMap<>();
        fieldTypeAttributes.put("name", fieldType);
        fieldTypeAttributes.put("class", "solr.DenseVectorField");
        fieldTypeAttributes.put("vectorDimension", dimensionality);

        if (validValues.contains(similarityFunction)) {
            fieldTypeAttributes.put("similarityFunction", similarityFunction);
        } else {
            fieldTypeAttributes.put("similarityFunction", "cosine");
        }

        if (hnswMaxConnections != null) {
            fieldTypeAttributes.put("hnswMaxConnections", hnswMaxConnections);
        }
        if (hnswBeamWidth != null) {
            fieldTypeAttributes.put("hnswBeamWidth", hnswBeamWidth);
        }

        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        fieldTypeDefinition.setAttributes(fieldTypeAttributes);
        log.info("Created the fieldTypeDefinition: {}", fieldTypeDefinition);

        SchemaRequest.AddFieldType addFieldTypeRequest = new SchemaRequest.AddFieldType(fieldTypeDefinition);
        solrClient.request(addFieldTypeRequest, collectionName);
    }

    private void createDenseVectorField(String fieldName, String fieldType, String collectionName) throws IOException, SolrServerException {
        Map<String, Object> fieldAttributes = new HashMap<>();
        fieldAttributes.put("name", fieldName);
        fieldAttributes.put("type", fieldType);
        fieldAttributes.put("stored", true);
        fieldAttributes.put("indexed", true);

        SchemaRequest.AddField addFieldRequest = new SchemaRequest.AddField(fieldAttributes);
        solrClient.request(addFieldRequest, collectionName);
    }

    private static class FieldCreationExistsAttributeMismatchException extends Exception {
        public FieldCreationExistsAttributeMismatchException(String message) {
            super(message);
        }
    }
}