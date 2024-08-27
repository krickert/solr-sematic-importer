package com.krickert.search.indexer.grpc;

import com.krickert.search.service.*;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunkServiceMockTest {

    private static final Logger log = LoggerFactory.getLogger(ChunkServiceMockTest.class);

    @Test
    void testMock() {
        ChunkServiceGrpc.ChunkServiceBlockingStub mockStub = new ChunkServiceMock().createMock();

        // Test chunk
        ChunkRequest chunkRequest = ChunkRequest.newBuilder()
                .setText("Some long text that needs to be chunked.")
                .setOptions(ChunkOptions.newBuilder().setLength(10).setOverlap(2).build())
                .build();
        ChunkReply chunkReply = mockStub.chunk(chunkRequest);
        log.info("chunk: {}", chunkReply.getChunksList());
        chunkReply = mockStub.chunk(chunkRequest);
        log.info("chunk: {}", chunkReply.getChunksList());

        // Test check (health check)
        HealthCheckRequest healthRequest = HealthCheckRequest.newBuilder().build();
        HealthCheckReply healthReply = mockStub.check(healthRequest);
        log.info("check: {}", healthReply.getStatus());
    }
}