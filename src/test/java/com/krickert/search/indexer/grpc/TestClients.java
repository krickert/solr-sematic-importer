package com.krickert.search.indexer.grpc;

import com.krickert.search.indexer.solr.client.OktaAuth;
import com.krickert.search.service.ChunkServiceGrpc;
import com.krickert.search.service.EmbeddingServiceGrpc;
import io.grpc.ManagedChannel;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.grpc.annotation.GrpcChannel;
import jakarta.inject.Named;

import java.io.IOException;

import static com.krickert.search.indexer.grpc.EmbeddingServiceMock.createMock;

@Factory

@Requires(env = Environment.TEST)
public class TestClients {

    private final EmbeddingServiceGrpc.EmbeddingServiceBlockingStub embeddingServiceBlockingStub;

    public TestClients() {
        this.embeddingServiceBlockingStub = createMock();
    }

    @Bean
    @Named("inlineEmbeddingService")
    public EmbeddingServiceGrpc.EmbeddingServiceBlockingStub inlineEmbeddingService() {
        return embeddingServiceBlockingStub;
    }

    @Bean
    @Named("vectorEmbeddingService")
    public EmbeddingServiceGrpc.EmbeddingServiceBlockingStub vectorEmbeddingService() {
        return embeddingServiceBlockingStub;
    }

    @Bean
    @Named("chunkService")
    ChunkServiceGrpc.ChunkServiceBlockingStub chunkServiceBlockingStub(
            @GrpcChannel("${indexer.chunker-grpc-channel}")
            ManagedChannel channel) {
        return ChunkServiceGrpc.newBlockingStub(
                channel
        );
    }

    @Bean
    @Named("OktaAuth")
    OktaAuth oktaAuth() {
        return new MockOktaAuth();
    }

    public static class MockOktaAuth implements OktaAuth {

        @Override
        public String getAccessToken() throws IOException {
            return "access-toke-fake";
        }
    }
}
