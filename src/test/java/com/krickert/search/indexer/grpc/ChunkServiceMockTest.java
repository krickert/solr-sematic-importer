package com.krickert.search.indexer.grpc;

import com.krickert.search.service.*;
import org.junit.jupiter.api.Test;

public class ChunkServiceMockTest {

    @Test
    void testMock() {
        ChunkServiceGrpc.ChunkServiceBlockingStub mockStub = new ChunkServiceMock().createMock();

        // Test chunk
        ChunkRequest chunkRequest = ChunkRequest.newBuilder()
                .setText("Some long text that needs to be chunked.")
                .setOptions(ChunkOptions.newBuilder().setLength(10).setOverlap(2).build())
                .build();
        ChunkReply chunkReply = mockStub.chunk(chunkRequest);
        System.out.println("chunk: " + chunkReply.getChunksList());

        // Test check (health check)
        HealthCheckRequest healthRequest = HealthCheckRequest.newBuilder().build();
        HealthCheckReply healthReply = mockStub.check(healthRequest);
        System.out.println("check: " + healthReply.getStatus());
    }
}