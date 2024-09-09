package com.krickert.search.indexer.grpc;

import com.krickert.search.service.ChunkServiceGrpc;
import io.grpc.ManagedChannel;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.grpc.annotation.GrpcChannel;
import jakarta.inject.Inject;

@Factory
public class ClientFactory {
    @Inject
    ClientGrpcTestContainers clientGrpcTestContainers;

    @Bean
    ChunkServiceGrpc.ChunkServiceBlockingStub chunkServiceBlockingStub(
            @GrpcChannel("${indexer.chunker-grpc-channel}")
            ManagedChannel channel) {
        return ChunkServiceGrpc.newBlockingStub(
                channel
        );
    }

}
