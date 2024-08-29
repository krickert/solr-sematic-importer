package com.krickert.search.indexer.solr.vector;

import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.config.VectorConfig;
import com.krickert.search.indexer.dto.SolrDocumentType;
import com.krickert.search.indexer.solr.index.SolrInputDocumentQueue;
import com.krickert.search.service.*;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
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
    private final SolrDestinationCollectionValidationService solrDestinationCollectionValidationService;

    @Inject
    public SolrVectorIndexingService(EmbeddingServiceGrpc.EmbeddingServiceBlockingStub embeddingServiceBlockingStub,
                                     IndexerConfiguration indexerConfiguration,
                                     SolrInputDocumentQueue solrInputDocumentQueue,
                                     ChunkServiceGrpc.ChunkServiceBlockingStub chunkServiceBlockingStub,
                                     SolrDestinationCollectionValidationService solrDestinationCollectionValidationService) {
        log.info("creating CreateVectorCollectionService");
        this.embeddingServiceBlockingStub = checkNotNull(embeddingServiceBlockingStub);
        this.indexerConfiguration = checkNotNull(indexerConfiguration);
        this.destinationCollectionName = indexerConfiguration.getDestinationSolrConfiguration().getCollection();
        this.solrInputDocumentQueue = checkNotNull(solrInputDocumentQueue);
        this.chunkServiceBlockingStub = checkNotNull(chunkServiceBlockingStub);
        this.solrDestinationCollectionValidationService = solrDestinationCollectionValidationService;
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

        solrInputDocumentQueue.addDocument(destinationCollectionName, solrInputDocument, SolrDocumentType.DOCUMENT);
    }


    private void processChunkField(String fieldName, VectorConfig vectorConfig,
                                   String fieldData, String origDocId,
                                   String crawlId, Date dateCreated) {
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
        solrInputDocumentQueue.addDocuments(vectorConfig.getDestinationCollection(),
                createChunkDocuments(fieldName, embeddingsVectorsReply.getEmbeddingsList(),
                        chunkerReply.getChunksList().stream().toList(),
                        origDocId, crawlId, dateCreated,
                        indexerConfiguration.getDestinationSolrConfiguration().getCollection(),
                        vectorConfig.getChunkFieldVectorName(),solrDestinationCollectionValidationService.getDimensionality()),
                SolrDocumentType.VECTOR);
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

        String vectorFieldName = indexerConfiguration.getVectorConfig().get(fieldName).getChunkFieldVectorName();
        EmbeddingsVectorReply embeddingsVectorReply = getEmbeddingsVectorReply(fieldData);
        solrInputDocument.addField(vectorFieldName, embeddingsVectorReply.getEmbeddingsList());
    }

    private List<SolrInputDocument> createChunkDocuments(String fieldName,
                                              List<EmbeddingsVectorReply> embeddingsList,
                                              List<String> chunksList,
                                              String origDocId,
                                              String crawlId,
                                              Date dateCreated,
                                              String parentCollection,
                                              String chunkVectorFieldName,
                                              Integer dimensionality) {
        List<SolrInputDocument> chunkDocuments = new ArrayList<>(chunksList.size());

        for (int i = 0; i < chunksList.size(); i++) {
            String chunk = chunksList.get(i);
            EmbeddingsVectorReply embedding = embeddingsList.get(i);
            Collection<Float> vector = embedding.getEmbeddingsList();
            // Define the docId with left-padded sequence number
            String docId = origDocId + "#" + StringUtils.leftPad(""+i, 7, "0");
            SolrInputDocument docToAdd = createSolrInputDocument(
                    docId,
                    origDocId, // Assuming parentId is the original document ID
                    chunk,
                    i,
                    vector,
                    fieldName,
                    crawlId,
                    dateCreated,
                    parentCollection,
                    chunkVectorFieldName
            );
            chunkDocuments.add(docToAdd);
        }
        return chunkDocuments;
    }

    @Retryable(attempts = "3", delay = "1s", multiplier = "2.0", includes = {io.grpc.StatusRuntimeException.class})
    protected ChunkReply getChunks(ChunkRequest request) {
        return chunkServiceBlockingStub.chunk(request);
    }

    @Retryable(attempts = "3", delay = "1s", multiplier = "2.0", includes = {io.grpc.StatusRuntimeException.class})
    protected EmbeddingsVectorReply getEmbeddingsVectorReply(String fieldData) {
        return embeddingServiceBlockingStub.createEmbeddingsVector(EmbeddingsVectorRequest.newBuilder().setText(fieldData).build());
    }


    @Retryable(attempts = "3", delay = "1s", multiplier = "2.0", includes = {io.grpc.StatusRuntimeException.class})
    protected EmbeddingsVectorsReply getEmbeddingsVectorsReply(List<String> fieldDataList) {
        return embeddingServiceBlockingStub.createEmbeddingsVectors(EmbeddingsVectorsRequest.newBuilder().addAllText(fieldDataList).build());
    }

    public static SolrInputDocument createSolrInputDocument(
            String docId,
            String parentId,
            String chunk,
            Integer chunkNumber,
            Collection<Float> vector,
            String parentFieldName,
            String crawlId,
            Date dateCreated,
            String parentCollection,
            String vectorFieldName)
    {

        SolrInputDocument document = new SolrInputDocument();
        document.addField("id", docId);
        document.addField("parentId", parentId);
        document.addField("chunk", chunk);
        document.addField("chunkNumber", chunkNumber);
        document.addField(vectorFieldName , vector);
        document.addField("parentFieldName", parentFieldName);
        document.addField("crawlId", crawlId);
        document.addField("dateCreated", dateCreated);
        document.addField("parentCollection", parentCollection);

        return document;
    }

}
