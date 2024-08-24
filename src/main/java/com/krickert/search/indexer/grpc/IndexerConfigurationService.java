package com.krickert.search.indexer.grpc;

import com.google.protobuf.Empty;
import com.krickert.search.indexer.grpc.IndexerConfigurationServiceGrpc.IndexerConfigurationServiceImplBase;
import io.grpc.stub.StreamObserver;
import io.micronaut.context.annotation.Property;
import io.micronaut.grpc.annotation.GrpcService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@GrpcService
public class IndexerConfigurationService extends IndexerConfigurationServiceImplBase {

    private final Map<String, IndexerConfiguration> configurations = new ConcurrentHashMap<>();

    @Property(name = "indexer.default.vector-grpc-channel")
    private String defaultVectorGrpcChannel;

    @Property(name = "indexer.default.chunker-grpc-channel")
    private String defaultChunkerGrpcChannel;

    @Property(name = "indexer.default.source-seed-data.enabled")
    private boolean defaultSourceSeedDataEnabled;

    @Property(name = "indexer.default.source-seed-data.seed-json-file")
    private String defaultSeedJsonFile;

    @Override
    public void getIndexerConfiguration(GetIndexerConfigurationRequest request, StreamObserver<IndexerConfiguration> responseObserver) {
        IndexerConfiguration configuration = configurations.getOrDefault(request.getId(), 
            IndexerConfiguration.newBuilder()
                .setVectorGrpcChannel(defaultVectorGrpcChannel)
                .setChunkerGrpcChannel(defaultChunkerGrpcChannel)
                .setSourceSeedData(SourceSeedData.newBuilder()
                    .setEnabled(defaultSourceSeedDataEnabled)
                    .setSeedJsonFile(defaultSeedJsonFile)
                    .build())
                .build());

        responseObserver.onNext(configuration);
        responseObserver.onCompleted();
    }

    @Override
    public void updateIndexerConfiguration(UpdateIndexerConfigurationRequest request, StreamObserver<Empty> responseObserver) {
        configurations.put(request.getId(), request.getConfiguration());

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}