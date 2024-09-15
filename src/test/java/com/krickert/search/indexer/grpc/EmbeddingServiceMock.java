package com.krickert.search.indexer.grpc;

import com.krickert.search.service.*;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.Mockito.*;

public class EmbeddingServiceMock {
    public static EmbeddingServiceGrpc.EmbeddingServiceBlockingStub createMock() {
        EmbeddingServiceGrpc.EmbeddingServiceBlockingStub mockStub = mock(EmbeddingServiceGrpc.EmbeddingServiceBlockingStub.class);

        // Mock createEmbeddingsVector with random floats
        EmbeddingsVectorReply embeddingsVectorReply = EmbeddingsVectorReply.newBuilder()
                .addAllEmbeddings(generateRandomFloats(300, -1.0f, 1.0f))
                .build();
        when(mockStub.createEmbeddingsVector(any(EmbeddingsVectorRequest.class))).thenReturn(embeddingsVectorReply);

        // Mock createEmbeddingsVectors to match the number of chunks sent in the request
        when(mockStub.createEmbeddingsVectors(any(EmbeddingsVectorsRequest.class)))
                .thenAnswer(invocation -> {
                    EmbeddingsVectorsRequest request = invocation.getArgument(0);
                    EmbeddingsVectorsReply.Builder replyBuilder = EmbeddingsVectorsReply.newBuilder();
                    for (String text : request.getTextList()) {
                        EmbeddingsVectorReply vectorReply = EmbeddingsVectorReply.newBuilder()
                                .addAllEmbeddings(generateRandomFloats(300, -1.0f, 1.0f))
                                .build();
                        replyBuilder.addEmbeddings(vectorReply);
                    }
                    return replyBuilder.build();
                });

        // Mock check (health check)
        HealthCheckReply healthCheckReply = HealthCheckReply.newBuilder()
                .setStatus("RUNNING")
                .setTimerRunning(7777777)
                .setServerName("localhost-mock")
                .build();
        when(mockStub.check(any(HealthCheckRequest.class))).thenReturn(healthCheckReply);

        return mockStub;
    }

    // Generate random floats between min and max
    private static List<Float> generateRandomFloats(int count, float min, float max) {
        Random random = new Random();
        return IntStream.range(0, count)
                .mapToObj(i -> min + random.nextFloat() * (max - min))
                .collect(Collectors.toList());
    }
}