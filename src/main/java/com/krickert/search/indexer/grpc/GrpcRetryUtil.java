package com.krickert.search.indexer.grpc;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;

import java.time.Duration;

public class GrpcRetryUtil {

    private static final RetryConfig config = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .build();

    private static final RetryRegistry registry = RetryRegistry.of(config);
    private static final Retry retry = registry.retry("grpcRetry");

    public static <T> T executeWithRetry(GrpcCall<T> grpcCall) {
        return io.github.resilience4j.retry.Retry.decorateCheckedSupplier(retry, grpcCall::call).get();
    }
}