package com.krickert.search.indexer.solr.vector;

import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.config.SolrConfiguration;
import com.krickert.search.indexer.config.VectorConfig;
import com.krickert.search.indexer.service.HealthService;
import com.krickert.search.indexer.solr.client.SolrAdminActions;
import com.krickert.search.service.EmbeddingServiceGrpc;
import com.krickert.search.service.EmbeddingsVectorRequest;
import io.micronaut.core.util.StringUtils;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class SolrDestinationCollectionValidationService {
    private static final Logger log = LoggerFactory.getLogger(SolrDestinationCollectionValidationService.class);

    private final IndexerConfiguration indexerConfiguration;
    private final SolrAdminActions solrAdminActions;
    private final EmbeddingServiceGrpc.EmbeddingServiceBlockingStub embeddingServiceBlockingStub;
    private final Integer dimensionality;
    private final HealthService healthService;
    private final AtomicBoolean embeddingsUp = new AtomicBoolean(false);


    @Scheduled(fixedRate = "20s")
    public void checkEmbeddingsUp() {
        if (healthService.checkVectorizerHealth()) {
            if (dimensionality == null || dimensionality < 1) {
                initializeDimensionality();
            }
            embeddingsUp.set(true);
        }
        if (!embeddingsUp.get()) {
            log.error("Embeddings are not up");
        }

    }

    @Inject
    public SolrDestinationCollectionValidationService(IndexerConfiguration indexerConfiguration, SolrAdminActions solrAdminActions, EmbeddingServiceGrpc.EmbeddingServiceBlockingStub embeddingServiceBlockingStub, HealthService healthService) {
        this.indexerConfiguration = indexerConfiguration;
        this.solrAdminActions = solrAdminActions;
        this.embeddingServiceBlockingStub = embeddingServiceBlockingStub;
        this.healthService = checkNotNull(healthService);
        this.dimensionality = initializeDimensionality();
    }

    private Integer initializeDimensionality() {
        log.info("Initializing dimensionality by creating embeddings vector");
        try {
            int numberOfDimensions = embeddingServiceBlockingStub.createEmbeddingsVector(
                    EmbeddingsVectorRequest.newBuilder().setText("Dummy").build()).getEmbeddingsCount();
            assert numberOfDimensions > 0;
            log.info("Finished initializing dimensionality");
            return numberOfDimensions;
        } catch (Exception e) {
            log.error("Failed to initialize dimensionality due to: {}", e.getMessage(), e);
            embeddingsUp.set(false);
            // Return a default dimensionality when exception occurs
            return null; // if dimensions do no exist make it null
        }
    }

    public Integer getDimensionality() {
        return dimensionality;
    }

    public void validate() {
        log.info("Validating the destination collection");
        validateDestinationCollection();
        log.info("Validating the vector collections");
        validateVectorCollections();
    }

    private void validateVectorCollections() {
        Map<String, VectorConfig> configs = indexerConfiguration.getVectorConfig();

        Map<Boolean, Map<String, VectorConfig>> partitionedMaps = configs.entrySet()
                .stream()
                .collect(Collectors.partitioningBy(
                        entry -> entry.getValue().getChunkField(),
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
                ));

        Map<String, VectorConfig> inlineConfigs = partitionedMaps.get(false);
        inlineConfigs.forEach(this::validateInlineConfig);

        Map<String, VectorConfig> chunkFieldTrue = partitionedMaps.get(true);
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
            solrAdminActions.createCollection(destinationCollection, destinationSolrConfiguration.getCollectionCreation());
        }
    }
}