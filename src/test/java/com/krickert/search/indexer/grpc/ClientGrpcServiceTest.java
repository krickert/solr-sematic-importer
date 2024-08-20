package com.krickert.search.indexer.grpc;

import com.krickert.search.service.ChunkServiceGrpc;
import com.krickert.search.service.EmbeddingServiceGrpc;
import com.krickert.search.service.PipeServiceGrpc;
import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@MicronautTest(environments = "test")
class ClientGrpcServiceTest {

    @Inject
    ClientGrpcTestContainers clientGrpcTestContainers;

    @Inject
    ApplicationContext context;

    private PipeServiceGrpc.PipeServiceBlockingStub pipeServiceBlockingStub;
    private EmbeddingServiceGrpc.EmbeddingServiceBlockingStub embeddingServiceBlockingStub;
    private ChunkServiceGrpc.ChunkServiceBlockingStub chunkServiceBlockingStub;

    @BeforeEach
    void setup() {
        this.pipeServiceBlockingStub = context.getBean(PipeServiceGrpc.PipeServiceBlockingStub.class);
        this.embeddingServiceBlockingStub = context.getBean(EmbeddingServiceGrpc.EmbeddingServiceBlockingStub.class);
        this.chunkServiceBlockingStub = context.getBean(ChunkServiceGrpc.ChunkServiceBlockingStub.class);
    }

    @Test
    void testVectorizerService() {
        assertNotNull(pipeServiceBlockingStub, "PipeServiceBlockingStub should not be null");
        assertNotNull(embeddingServiceBlockingStub, "EmbeddingServiceBlockingStub should not be null");


    }

    @Test
    void testChunkService() {
        assertNotNull(chunkServiceBlockingStub, "ChunkServiceBlockingStub should not be null");


    }
}