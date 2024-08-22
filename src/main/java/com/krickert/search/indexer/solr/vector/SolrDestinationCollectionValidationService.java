package com.krickert.search.indexer.solr.vector;

import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.config.SolrConfiguration;
import com.krickert.search.indexer.config.VectorConfig;
import com.krickert.search.indexer.solr.client.SolrAdminActions;
import com.krickert.search.service.EmbeddingServiceGrpc;
import com.krickert.search.service.EmbeddingsVectorRequest;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class SolrDestinationCollectionValidationService {
    private static final Logger log = LoggerFactory.getLogger(SolrDestinationCollectionValidationService.class);
    private final IndexerConfiguration indexerConfiguration;
    private final SolrAdminActions solrAdminActions;
    private final Integer dimensionality;

    public Integer getDimensionality() {
        return dimensionality;
    }

    @Inject
    public SolrDestinationCollectionValidationService(IndexerConfiguration indexerConfiguration, SolrAdminActions solrAdminActions, EmbeddingServiceGrpc.EmbeddingServiceBlockingStub embeddingServiceBlockingStub) {
        this.indexerConfiguration = indexerConfiguration;
        this.solrAdminActions = solrAdminActions;
        int numberOfDimensions = embeddingServiceBlockingStub.createEmbeddingsVector(
                EmbeddingsVectorRequest.newBuilder().setText("Dummy").build()).getEmbeddingsCount();
        assert numberOfDimensions > 0;
        this.dimensionality = numberOfDimensions;
    }

    public void validate() {
        log.info("Validating the destination collection");
        validateDestinationCollection();
        log.info("Validating the vector collections");
        validateVectorCollections();
    }

    private void validateVectorCollections() {
        Map<String, VectorConfig> configs = indexerConfiguration.getVectorConfig();
        // Partition the map into two maps based on getChunkField method
        Map<Boolean, Map<String, VectorConfig>> partitionedMaps = configs.entrySet()
                .stream()
                .collect(Collectors.partitioningBy(
                        entry -> entry.getValue().getChunkField(),
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
                ));

        // Maps based on the value of getChunkField
        Map<String, VectorConfig> inlineConfigs = partitionedMaps.get(false);  // Map with getChunkField() == false
        inlineConfigs.forEach(this::validateInlineConfig);

        Map<String, VectorConfig> chunkFieldTrue = partitionedMaps.get(true);  // Map with getChunkField() == true
        chunkFieldTrue.forEach(this::validateChunkFieldConfig);
    }

    private void validateInlineConfig(String fieldName, VectorConfig vectorConfig) {
        String destinationCollection = indexerConfiguration.getDestinationSolrConfiguration().getCollection();

        String vectorFieldName = vectorConfig.getChunkFieldVectorName();
        if (StringUtils.isEmpty(vectorFieldName)) {
            vectorFieldName = fieldName + "-vector";
        }

        validateVectorField(vectorFieldName, vectorConfig, destinationCollection);
    }

    private void validateChunkFieldConfig(String fieldName, VectorConfig vectorConfig) {
        String destinationCollection = vectorConfig.getDestinationCollection();
        if (StringUtils.isEmpty(destinationCollection)) {
            destinationCollection = indexerConfiguration.getDestinationSolrConfiguration().getCollection() +
                    "-" + fieldName + "-chunks";
        }

        if (solrAdminActions.doesCollectionExist(destinationCollection)) {
            log.info("Collection {} already exists", destinationCollection);
        } else {
            log.info("Creating collection {} ", destinationCollection);
            solrAdminActions.createCollection(destinationCollection, vectorConfig.getCollectionCreation());
        }

        String vectorFieldName = vectorConfig.getChunkFieldVectorName();
        if (StringUtils.isEmpty(vectorFieldName)) {
            vectorFieldName = fieldName + "-vector";
        }

        validateVectorField(vectorFieldName, vectorConfig, destinationCollection);
    }

    private void validateVectorField(String vectorFieldName, VectorConfig vectorConfig, String destinationCollection) {
        try {
            solrAdminActions.validateVectorField(
                    vectorFieldName,
                    vectorConfig.getSimilarityFunction(),
                    vectorConfig.getHnswMaxConnections(),
                    vectorConfig.getHnswBeamWidth(),
                    dimensionality,
                    destinationCollection);
        } catch (IOException | SolrServerException e) {
            throw new RuntimeException(e);
        }
    }

    private void validateDestinationCollection() {
        if(solrAdminActions.doesCollectionExist(indexerConfiguration.getDestinationSolrConfiguration().getCollection())) {
            log.info("Destination collection already exists {}", indexerConfiguration.getDestinationSolrConfiguration().getCollection());
        } else {
            SolrConfiguration destinationSolrConfiguration = indexerConfiguration.getDestinationSolrConfiguration();
            String destinationCollection = destinationSolrConfiguration.getCollection();
            //create the collection
            solrAdminActions.createCollection(destinationCollection, destinationSolrConfiguration.getCollectionCreation());
        }
    }
}
