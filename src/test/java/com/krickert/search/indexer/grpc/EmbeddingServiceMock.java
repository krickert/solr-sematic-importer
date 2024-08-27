package com.krickert.search.indexer.grpc;
import com.krickert.search.service.*;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import static org.mockito.Mockito.*;

import java.util.Arrays;

@Factory
public class EmbeddingServiceMock {

    @Bean
    public EmbeddingServiceGrpc.EmbeddingServiceBlockingStub createMock() {
        EmbeddingServiceGrpc.EmbeddingServiceBlockingStub mockStub = mock(EmbeddingServiceGrpc.EmbeddingServiceBlockingStub.class);

        // Mock createEmbeddingsVector
        EmbeddingsVectorReply embeddingsVectorReply = EmbeddingsVectorReply.newBuilder()
                .addAllEmbeddings(Arrays.asList(0.1f, 0.2f, 0.3f))
                .build();
        when(mockStub.createEmbeddingsVector(any(EmbeddingsVectorRequest.class)))
                .thenReturn(embeddingsVectorReply);

        // Mock createEmbeddingsVectors
        EmbeddingsVectorsReply embeddingsVectorsReply = EmbeddingsVectorsReply.newBuilder()
                .addEmbeddings(embeddingsVectorReply)
                .addEmbeddings(embeddingsVectorReply)
                .build();
        when(mockStub.createEmbeddingsVectors(any(EmbeddingsVectorsRequest.class)))
                .thenReturn(embeddingsVectorsReply);

        // Mock check (health check)
        HealthCheckReply healthCheckReply = HealthCheckReply.newBuilder()
                .setStatus("RUNNING").setTimerRunning(7777777).setServerName("localhost-mock")
                .build();
        when(mockStub.check(any(HealthCheckRequest.class))).thenReturn(healthCheckReply);

        return mockStub;
    }



}