package com.krickert.search.indexer.solr.vector;

import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.config.SolrConfiguration;
import com.krickert.search.indexer.config.VectorConfig;
import com.krickert.search.indexer.solr.client.SolrAdminActions;
import com.krickert.search.service.EmbeddingServiceGrpc;
import com.krickert.search.service.EmbeddingsVectorRequest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

@Singleton
public class SolrDestinationCollectionValidationService {
    private static final Logger log = LoggerFactory.getLogger(SolrDestinationCollectionValidationService.class);
    private final IndexerConfiguration indexerConfiguration;
    private final SolrAdminActions solrAdminActions;
    private final Integer dimensionality;

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
        configs.forEach((fieldName, vectorConfig) -> {
            if(vectorConfig.getChunkField()) {
                String destinationCollection = vectorConfig.getDestinationCollection();
                if(solrAdminActions.doesCollectionExist(destinationCollection)) {
                    log.info("Collection {} already exists", destinationCollection);
                } else {
                    log.info("Creating collection {} ", destinationCollection);
                    solrAdminActions.createCollection(destinationCollection, vectorConfig.getCollectionCreation());
                }
                //at this point we need to validate that the right vector field is
                //there, and that the vector field we create is of the right dimensionality
                String vectorFieldName = vectorConfig.getChunkFieldVectorName();
                if (vectorFieldName == null) {
                    vectorFieldName = fieldName + "_vector";
                }
                try {
                    solrAdminActions.validateVectorField(vectorFieldName, dimensionality, destinationCollection);
                } catch (IOException | SolrServerException e) {
                    throw new RuntimeException(e);
                }
            }

        });
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
