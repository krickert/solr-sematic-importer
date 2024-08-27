package com.krickert.search.indexer.service;

import com.krickert.search.service.ChunkServiceGrpc;
import com.krickert.search.service.EmbeddingServiceGrpc;
import com.krickert.search.service.HealthCheckReply;
import com.krickert.search.service.HealthCheckRequest;
import io.grpc.StatusRuntimeException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class HealthService {
    private static final Logger log = LoggerFactory.getLogger(HealthService.class);
    private final EmbeddingServiceGrpc.EmbeddingServiceBlockingStub embeddingService;
    private final ChunkServiceGrpc.ChunkServiceBlockingStub chunkService;

    @Inject
    public HealthService(EmbeddingServiceGrpc.EmbeddingServiceBlockingStub embeddingService,
                         ChunkServiceGrpc.ChunkServiceBlockingStub chunkService) {
        this.embeddingService = embeddingService;
        this.chunkService = chunkService;
        log.info("Set up the chunking and embedding service managed channels.");
    }

    public boolean checkVectorizerHealth() {
        try {
            log.info("Calling ping on: {}", embeddingService.getChannel().toString());
            HealthCheckReply reply = embeddingService.check(HealthCheckRequest.newBuilder().build());
            return reply != null && reply.isInitialized();
        } catch (StatusRuntimeException e) {
            log.error("vector service not running. semantic indexing is going to be disabled.");
            return false;
        }
    }

    public boolean checkChunkerHealth() {
        try {
            log.info("Calling ping on: {}", chunkService.getChannel().toString());
            HealthCheckReply reply = chunkService.check(HealthCheckRequest.newBuilder().build());
            return reply != null && reply.isInitialized();
        } catch (StatusRuntimeException e) {
            log.error("chunk service not running. semantic indexing is going to be disabled. {}", ExceptionUtils.getStackTrace(e));
            return false;
        }
    }

    public boolean isGrpcServiceAvailable() {
        return checkVectorizerHealth() && checkChunkerHealth();
    }
}