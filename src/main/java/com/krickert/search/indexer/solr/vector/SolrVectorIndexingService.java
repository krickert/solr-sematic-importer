package com.krickert.search.indexer.solr.vector;

import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.config.VectorConfig;
import com.krickert.search.indexer.solr.index.SolrInputDocumentQueue;
import com.krickert.search.service.*;
import io.grpc.StatusRuntimeException;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Service that takes in the indexing configuration and determines if the collections for creating vectors
 *
 */
@Singleton
public class SolrVectorIndexingService {

    private static final Logger log = LoggerFactory.getLogger(SolrVectorIndexingService.class);
    private final EmbeddingServiceGrpc.EmbeddingServiceBlockingStub embeddingServiceBlockingStub;
    private final ChunkServiceGrpc.ChunkServiceBlockingStub chunkServiceBlockingStub;
    private final IndexerConfiguration indexerConfiguration;
    private final SolrInputDocumentQueue solrInputDocumentQueue;
    private final String destinationCollectionName;

    @Inject
    public SolrVectorIndexingService(EmbeddingServiceGrpc.EmbeddingServiceBlockingStub embeddingServiceBlockingStub,
                                     IndexerConfiguration indexerConfiguration, SolrClient solrClient,
                                     SolrInputDocumentQueue solrInputDocumentQueue,
                                     ChunkServiceGrpc.ChunkServiceBlockingStub chunkServiceBlockingStub) {
        this.embeddingServiceBlockingStub = checkNotNull(embeddingServiceBlockingStub);
        this.indexerConfiguration = checkNotNull(indexerConfiguration);
        this.destinationCollectionName = indexerConfiguration.getDestinationSolrConfiguration().getCollection();
        this.solrInputDocumentQueue = checkNotNull(solrInputDocumentQueue);
        this.chunkServiceBlockingStub = checkNotNull(chunkServiceBlockingStub);
        log.info("CreateVectorCollectionService created");
    }


    public void addVectorFieldsToSolr(SolrInputDocument solrInputDocument, String crawlId, Date dateCreated) {
        checkNotNull(solrInputDocument);

        String origDocId = Optional.ofNullable(solrInputDocument.getFieldValue("id"))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException("Field 'id' is missing or null in the SolrInputDocument"));

        indexerConfiguration.getVectorConfig().forEach((fieldName, vectorConfig) -> {
            String fieldData = Optional.ofNullable(solrInputDocument.getFieldValue(fieldName))
                    .map(Object::toString)
                    .orElse(null);

            if (vectorConfig.getChunkField()) {
                processChunkField(fieldName, vectorConfig, fieldData, origDocId, crawlId, dateCreated);
            } else {
                processInlineDocumentField(solrInputDocument, fieldName, fieldData, origDocId);
            }
        });

        solrInputDocumentQueue.addDocument(destinationCollectionName, solrInputDocument);
    }


    private void processChunkField(String fieldName, VectorConfig vectorConfig,
                                   String fieldData, String origDocId, String crawlId, Date dateCreated) {
        if (fieldData == null) {
            log.warn("Field data for {} is null in document with id {}", fieldName, origDocId);
            return;
        }

        ChunkRequest request = createChunkRequest(fieldData, vectorConfig);

        ChunkReply chunkerReply = getChunks(request);
        log.info("There are {} chunks in document with id {}", chunkerReply.getChunksCount(), origDocId);

        // Now we have each of the chunks for this field we need to save them in the new collection
        EmbeddingsVectorsReply embeddingsVectorsReply = getEmbeddingsVectorsReply(chunkerReply.getChunksList());

        // Create a list of beans based on these vectors
        solrInputDocumentQueue.addBeans(vectorConfig.getDestinationCollection(),
                createChunkDocuments(fieldName, embeddingsVectorsReply.getEmbeddingsList(),
                        chunkerReply.getChunksList().stream().toList(),
                        origDocId, crawlId, dateCreated,
                        indexerConfiguration.getDestinationSolrConfiguration().getCollection()));
    }


    private ChunkRequest createChunkRequest(String fieldData, VectorConfig vectorConfig) {
        return ChunkRequest.newBuilder()
                .setText(fieldData)
                .setOptions(ChunkOptions.newBuilder()
                        .setLength(vectorConfig.getChunkSize())
                        .setOverlap(vectorConfig.getChunkOverlap())
                        .build())
                .build();
    }

    private void processInlineDocumentField(SolrInputDocument solrInputDocument, String fieldName, String fieldData, String origDocId) {
        if (fieldData == null) {
            log.warn("Field data for {} is null in document with id {}", fieldName, origDocId);
            return;
        }

        String vectorFieldName = fieldName + "_vector";
        EmbeddingsVectorReply embeddingsVectorReply = getEmbeddingsVectorReply(fieldData);
        solrInputDocument.addField(vectorFieldName, embeddingsVectorReply.getEmbeddingsList());
    }

    private List<Object> createChunkDocuments(String fieldName,
                                              List<EmbeddingsVectorReply> embeddingsList,
                                              List<String> chunksList,
                                              String origDocId,
                                              String crawlId,
                                              Date dateCreated,
                                              String parentCollection) {
        List<Object> chunkDocuments = new ArrayList<>(chunksList.size());

        for (int i = 0; i < chunksList.size(); i++) {
            String chunk = chunksList.get(i);
            EmbeddingsVectorReply embedding = embeddingsList.get(i);
            Collection<Float> vector = embedding.getEmbeddingsList();
            // Define the docId with left-padded sequence number
            String docId = origDocId + "#" + StringUtils.leftPad(""+i, 7, "0");
            AddVectorToSolrDocumentRequest request = new AddVectorToSolrDocumentRequest(
                    docId,
                    origDocId, // Assuming parentId is the original document ID
                    chunk,
                    i,
                    vector,
                    fieldName,
                    crawlId,
                    dateCreated,
                    parentCollection
            );
            chunkDocuments.add(request);
        }
        return chunkDocuments;
    }
    @Retryable(attempts = "5", delay = "500ms", includes = StatusRuntimeException.class)
    protected ChunkReply getChunks(ChunkRequest request) {
        return chunkServiceBlockingStub.chunk(request);
    }



    @Retryable(attempts = "5", delay = "500ms", includes = StatusRuntimeException.class)
    protected EmbeddingsVectorReply getEmbeddingsVectorReply(String fieldData) {
        return embeddingServiceBlockingStub.createEmbeddingsVector(EmbeddingsVectorRequest.newBuilder().setText(fieldData).build());
    }


    @Retryable(attempts = "5", delay = "500ms", includes = StatusRuntimeException.class)
    protected EmbeddingsVectorsReply getEmbeddingsVectorsReply(List<String> fieldDataList) {
        return embeddingServiceBlockingStub.createEmbeddingsVectors(EmbeddingsVectorsRequest.newBuilder().addAllText(fieldDataList).build());
    }


}
