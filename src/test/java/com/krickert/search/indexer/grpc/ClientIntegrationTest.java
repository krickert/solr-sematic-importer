package com.krickert.search.indexer.grpc;

import com.krickert.search.service.ChunkServiceGrpc;
import com.krickert.search.service.EmbeddingServiceGrpc;
import com.krickert.search.service.PipeServiceGrpc;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@MicronautTest
@Testcontainers
public class ClientIntegrationTest {

    @Inject
    ClientGrpcTestContainers clientGrpcTestContainers;

    @Inject
    EmbeddingServiceGrpc.EmbeddingServiceBlockingStub embeddingServiceBlockingStub;

    @Inject
    ChunkServiceGrpc.ChunkServiceBlockingStub chunkServiceBlockingStub;

    @Inject
    PipeServiceGrpc.PipeServiceBlockingStub pipeServiceBlockingStub;


    @Test
    void test() {
        Assertions.assertNotNull(clientGrpcTestContainers);
        for(GenericContainer<?> container : clientGrpcTestContainers.getContainers()) {
            Assertions.assertNotNull(container);
            Assertions.assertTrue(container.isRunning());
            Assertions.assertTrue(container.isCreated());
            Assertions.assertNotNull(container.getContainerId());
        }
    }

}
