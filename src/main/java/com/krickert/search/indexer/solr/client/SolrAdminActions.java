package com.krickert.search.indexer.solr.client;

import com.google.common.base.Preconditions;
import com.krickert.search.indexer.config.SolrConfiguration;
import com.krickert.search.indexer.config.VectorConfig;
import io.micronaut.core.io.ResourceLoader;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.ConfigSetAdminRequest;
import org.apache.solr.client.solrj.request.GenericSolrRequest;
import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.*;
import org.apache.solr.client.solrj.response.schema.FieldTypeRepresentation;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Singleton
public class SolrAdminActions {
    private static final Logger log = LoggerFactory.getLogger(SolrAdminActions.class);

    private final SolrClient solrClient;
    private final ResourceLoader resourceLoader;

    @Inject
    public SolrAdminActions(SolrClient solrClient, ResourceLoader resourceLoader) {
        this.solrClient = solrClient;
        this.resourceLoader = resourceLoader;
        assert solrClient != null;
    }

    /**
     * Checks if the connection to the Solr server is alive by sending a ping request.
     *
     * @return true if the ping is successful, false otherwise
     */
    public boolean isSolrServerAlive() {
        return isSolrServerAlive(solrClient);
    }

    public boolean isSolrServerAlive(SolrClient solrClient) {
        try {
            SolrPingResponse pingResponse = solrClient.ping();
            return pingResponse.getStatus() == 0;
        } catch (SolrServerException | IOException e) {
            log.error("Exception thrown", e);
            return false;
        }
    }

    public boolean doesCollectionExist(String collectionName) {
        return doesCollectionExist(collectionName, solrClient);
    }

    /**
     * Checks if a specific collection exists on the Solr server.
     *
     * @param collectionName the name of the collection to check
     * @return true if the collection exists, false otherwise
     */
    public boolean doesCollectionExist(String collectionName, SolrClient solrClient) {
        try {
            CollectionAdminRequest.List listRequest = new CollectionAdminRequest.List();
            CollectionAdminResponse response = listRequest.process(solrClient);
            NamedList<NamedList<Object>> collections = response.getCollectionStatus();
            if (collections == null) {
                return false;
            }
            return collections.asShallowMap().containsValue(collectionName);
        } catch (SolrServerException | IOException e) {
            log.error("Exception thrown", e);
            return false;
        }
    }


    public boolean doesConfigExist(String configToCheck) {
        return doesConfigExist(solrClient, configToCheck);
    }


    public boolean doesConfigExist(SolrClient solrClient, String configToCheck) {
        Preconditions.checkNotNull(solrClient);
        Preconditions.checkNotNull(configToCheck);
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set("action", "LIST");

        // Make the request to the Solr Admin API to list config sets
        GenericSolrRequest request = new GenericSolrRequest(GenericSolrRequest.METHOD.GET, "/admin/configs", params);

        try {
            SimpleSolrResponse response = request.process(solrClient);
            // Extract config sets from the response
            NamedList<Object> responseNamedList = response.getResponse();
            @SuppressWarnings("unchecked")
            ArrayList<String> configSets = (ArrayList<String>) responseNamedList.get("configSets");

            if (configSets == null) {
                log.info("There are no configsets in this solr instance.");
                return false;
            }
            boolean configExists = false;

            for (String config : configSets) {
                if (configToCheck.equals(config)) {
                    configExists = true;
                    break;
                }
            }
            return configExists;
        } catch (SolrServerException | IOException e) {
            log.error("exception thrown", e);
            throw new RuntimeException(e);
        }
    }

    public void uploadConfigSet(String configFile, String configName) {
        uploadConfigSet(solrClient, configFile, configName);
    }

    public void uploadConfigSet(SolrClient client, String configFile, String configName) {
        ConfigSetAdminRequest.Upload request = new ConfigSetAdminRequest.Upload();
        request.setConfigSetName(configName);
        final File file = loadFile(configFile);
        request.setUploadFile(file, "zip");
        // Execute the request
        final ConfigSetAdminResponse response;
        try {
            response = request.process(client);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }

        // Check the response status
        if (response.getStatus() == 0) {
            log.info("Configset uploaded successfully!");
        } else {
            log.error("Error uploading configset: {}", response);
        }
    }

    public File loadFile(String configFile) {
        Optional<URL> resource = resourceLoader.getResource(configFile);
        if (resource.isEmpty()) {
            log.error("Resource {} not found", configFile);
            throw new IllegalStateException("Missing resource file " + " configFile");
        }

        try {
            URL resourceUrl = resource.get();
            URI resourceUri = resourceUrl.toURI();
            Path path = Paths.get(resourceUri);
            File file = path.toFile();

            log.info("File loaded successfully: {}", file.getAbsolutePath());
            return file;
        } catch (Exception e) {
            log.error("Failed to load file from resource {}", configFile, e);
            throw new RuntimeException(e);
        }
    }

    public void createCollection(String destinationCollection, SolrConfiguration.SolrCollectionCreationConfig config) {
        createCollection(solrClient, destinationCollection, config.getCollectionConfigName(), config.getCollectionConfigFile(), config.getNumberOfReplicas(), config.getNumberOfShards());
    }

    public void createCollection(String destinationCollection, VectorConfig.VectorCollectionCreationConfig config) {
        createCollection(solrClient, destinationCollection, config.getCollectionConfigName(), config.getCollectionConfigFile(), config.getNumberOfReplicas(), config.getNumberOfShards());
    }

    public void createCollection(SolrClient solrClient, String collectionName,
                                 String collectionConfigName, String collectionConfigFile, int numberOfReplicas,
                                 int numberOfShards) {
        if (doesCollectionExist(collectionName)) {
            log.info("Collection {} exists.  No need to create it.", collectionName);
            return;
        }
        log.info("Collection {} does not exist.  Creating it.", collectionName);
        if (!doesConfigExist(collectionConfigName)) {
            uploadConfigSet(
                    collectionConfigFile, collectionConfigName);
        } else {
            log.info("Configuration exists.  Will create the collection.");
        }
        try {
            // Create the collection request
            CollectionAdminRequest.Create createRequest = CollectionAdminRequest.createCollection(
                    collectionName, collectionConfigName, numberOfShards, numberOfReplicas);
            // Process the request
            CollectionAdminResponse response = createRequest.process(solrClient);
            checkResponseStatus(collectionName, response);
        } catch (IOException | SolrServerException e) {
            throw new RuntimeException(e);
        }
    }

    private static void checkResponseStatus(String collectionName, CollectionAdminResponse response) {
        if (response.isSuccess()) {
            log.info("Collection {} created successfully Response: {}.", collectionName, response);
        } else {
            log.error("Error creating collection: " + response.getErrorMessages());
            throw new IllegalStateException("Error creating collection: " + response.getErrorMessages());
        }
    }

    public void commit(String collectionName){
        try {
            solrClient.commit(collectionName);
        } catch (SolrServerException | IOException e) {
            log.error("Could not commit collection {} due to {}", collectionName, e.getMessage());
            throw new RuntimeException(e);
        }

    }

    public void validateVectorField(String vectorFieldName, Integer dimensionality, String collection) throws IOException, SolrServerException {
        // Check if the field type exists
        String vectorFieldType = vectorFieldName + "_" + dimensionality;
        boolean fieldTypeExists = checkFieldTypeExists(dimensionality, collection);
        if (!fieldTypeExists) {
            log.info("A dense vector field of dimensionality {} does not exist.  Creating it.", dimensionality);
            // Create the field type
            createFieldType(vectorFieldType, dimensionality, collection);
        }

        // Check if the field exists
        boolean fieldExists = checkFieldExists(vectorFieldName, collection);
        if (!fieldExists) {
            // Create the field
            createField(vectorFieldName, vectorFieldType, collection);
        }
    }

    private boolean checkFieldTypeExists(Integer dimensionality, String collectionName) throws IOException, SolrServerException {
        SchemaRequest.FieldTypes fieldTypesRequest = new SchemaRequest.FieldTypes();
        SchemaResponse.FieldTypesResponse fieldTypesResponse = fieldTypesRequest.process(solrClient, collectionName);
        List<FieldTypeRepresentation> fieldTypes = fieldTypesResponse.getFieldTypes();
        for (FieldTypeRepresentation fieldTypeInfo : fieldTypes) {
            Map<String, Object> attributes = fieldTypeInfo.getAttributes();

            Object classAttribute = attributes.get("class");
            Object vectorDimensionAttribute = attributes.get("vectorDimension");

            // check if 'class' equals "solr.DenseVectorField" and 'vectorDimension' equals the provided dimensionality
            if ("solr.DenseVectorField".equals(classAttribute) &&
                    (vectorDimensionAttribute != null &&
                            (vectorDimensionAttribute.equals(dimensionality)
                            || vectorDimensionAttribute.equals(dimensionality.toString())))) {
                return true;
            }
        }
        return false;
    }

    private boolean checkFieldExists(String fieldName, String collectionName) throws IOException, SolrServerException {
        SchemaRequest.Fields fieldsRequest = new SchemaRequest.Fields();
        SchemaResponse.FieldsResponse fieldsResponse = fieldsRequest.process(solrClient, collectionName);
        List<Map<String, Object>> fields = fieldsResponse.getFields();
        for (Map<String, Object> fieldInfo : fields) {
            if (fieldName.equals(fieldInfo.get("name").toString())) {
                return true;
            }
        }
        return false;
    }

    private void createFieldType(String fieldType, Integer dimensionality, String collectionName) throws IOException, SolrServerException {
        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        Map<String, Object> fieldTypeAttributes = new HashMap<>();
        fieldTypeAttributes.put("name", fieldType);
        fieldTypeAttributes.put("class", "solr.DenseVectorField");
        fieldTypeAttributes.put("vectorDimension", dimensionality);
        fieldTypeDefinition.setAttributes(fieldTypeAttributes);
        log.info("Created the fieldTypeDefinition: {}", fieldTypeDefinition);
        SchemaRequest.AddFieldType addFieldTypeRequest = new SchemaRequest.AddFieldType(fieldTypeDefinition);
        solrClient.request(addFieldTypeRequest, collectionName);
    }

    private void createField(String fieldName, String fieldType, String collectionName) throws IOException, SolrServerException {
        Map<String, Object> fieldAttributes = new HashMap<>();
        fieldAttributes.put("name", fieldName);
        fieldAttributes.put("type", fieldType);
        fieldAttributes.put("stored", true);
        fieldAttributes.put("indexed", true);

        SchemaRequest.AddField addFieldRequest = new SchemaRequest.AddField(fieldAttributes);
        solrClient.request(addFieldRequest, collectionName);
    }
}
