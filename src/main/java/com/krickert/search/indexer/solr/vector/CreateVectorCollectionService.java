package com.krickert.search.indexer.solr.vector;

import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.service.ChunkServiceGrpc;
import com.krickert.search.service.EmbeddingServiceGrpc;
import com.krickert.search.service.EmbeddingsVectorReply;
import com.krickert.search.service.EmbeddingsVectorRequest;
import io.grpc.StatusRuntimeException;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Service that takes in the indexing configuration and determines if the collections for creating vectors
 *
 */
@Singleton
public class CreateVectorCollectionService {

    private static final Logger log = LoggerFactory.getLogger(CreateVectorCollectionService.class);
    private final EmbeddingServiceGrpc.EmbeddingServiceBlockingStub embeddingServiceBlockingStub;
    private final ChunkServiceGrpc.ChunkServiceBlockingStub chunkServiceBlockingStub;
    private final IndexerConfiguration indexerConfiguration;
    private final SolrClient solrClient;

    @Inject
    public CreateVectorCollectionService(EmbeddingServiceGrpc.EmbeddingServiceBlockingStub embeddingServiceBlockingStub,
                                         IndexerConfiguration indexerConfiguration, SolrClient solrClient,
                                         ChunkServiceGrpc.ChunkServiceBlockingStub chunkServiceBlockingStub) {
        this.embeddingServiceBlockingStub = checkNotNull(embeddingServiceBlockingStub);
        this.indexerConfiguration = checkNotNull(indexerConfiguration);
        this.solrClient = checkNotNull(solrClient);
        this.chunkServiceBlockingStub = checkNotNull(chunkServiceBlockingStub);
        log.info("CreateVectorCollectionService created");
    }


    public SolrInputDocument addVectorFieldToDocument(SolrInputDocument solrInputDocument, String fieldNameToVector) {
        String vectorFieldName = fieldNameToVector + "_vector";
        String fieldData = solrInputDocument.getFieldValue(fieldNameToVector).toString();
        EmbeddingsVectorReply embeddingsVectorReply = getEmbeddingsVectorReply(fieldData);
        solrInputDocument.addField(vectorFieldName, embeddingsVectorReply.getEmbeddingsList());
        return solrInputDocument;
    }

    @Retryable(attempts = "5", delay = "500ms", includes = StatusRuntimeException.class)
    private EmbeddingsVectorReply getEmbeddingsVectorReply(String fieldData) {
        EmbeddingsVectorReply embeddingsVectorReply = embeddingServiceBlockingStub.createEmbeddingsVector(EmbeddingsVectorRequest.newBuilder()
                .setText(fieldData).build());
        return embeddingsVectorReply;
    }


    public void addVectorDocumentToSolr(AddVectorToSolrDocumentRequest request) {
        try {
            solrClient.addBean(getVectorCollectionName(request), request);
        } catch (IOException | SolrServerException e) {
            log.error("Problem adding request chunk id {} for vector collection {}", request, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String getVectorCollectionName(AddVectorToSolrDocumentRequest request) {
        return request.parentCollection() + "_" + request.docFieldName();
    }


    record AddVectorToSolrDocumentRequest(
            @Field String docId,
            @Field String parentId,
            @Field String chunk,
            @Field Integer chunkNumber,
            @Field Collection<Float> vector,
            @Field String docFieldName,
            @Field String crawlId,
            @Field Date dateCreated,
            @Field String parentCollection
    ) {}


}
