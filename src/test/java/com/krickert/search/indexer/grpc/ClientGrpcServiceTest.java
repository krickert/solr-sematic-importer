package com.krickert.search.indexer.grpc;

import com.krickert.search.service.ChunkServiceGrpc;
import com.krickert.search.service.EmbeddingServiceGrpc;
import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@MicronautTest(environments = "test")
class ClientGrpcServiceTest {

    @Inject
    ClientTestContainers clientTestContainers;

    @Inject
    ApplicationContext context;

    @Inject
    @Named("inlineEmbeddingService")
    EmbeddingServiceGrpc.EmbeddingServiceBlockingStub inlineEmbeddingService;

    @Inject
    @Named("vectorEmbeddingService")
    EmbeddingServiceGrpc.EmbeddingServiceBlockingStub vectorEmbeddingService;

    private ChunkServiceGrpc.ChunkServiceBlockingStub chunkServiceBlockingStub;

    @BeforeEach
    void setup() {
        this.chunkServiceBlockingStub = context.getBean(ChunkServiceGrpc.ChunkServiceBlockingStub.class);
    }

    @Test
    void testInlineEmbeddingService() {
        assertNotNull(inlineEmbeddingService, "InlineEmbeddingService should not be null");
    }

    @Test
    void testVectorEmbeddingService() {
        assertNotNull(vectorEmbeddingService, "VectorEmbeddingService should not be null");
    }

    @Test
    void testChunkService() {
        assertNotNull(chunkServiceBlockingStub, "ChunkServiceBlockingStub should not be null");
    }
}