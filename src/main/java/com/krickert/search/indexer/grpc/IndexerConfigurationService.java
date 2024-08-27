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

    private final String defaultVectorGrpcChannel;

    private final String defaultChunkerGrpcChannel;

    private final boolean defaultSourceSeedDataEnabled;

    private final String defaultSeedJsonFile;

    public IndexerConfigurationService(com.krickert.search.indexer.config.IndexerConfiguration indexerConfiguration) {
        this.defaultVectorGrpcChannel = indexerConfiguration.getIndexerConfigurationProperties().getVectorGrpcChannel();
        this.defaultChunkerGrpcChannel = indexerConfiguration.getIndexerConfigurationProperties().getChunkerGrpcChannel();
        this.defaultSourceSeedDataEnabled =
                indexerConfiguration.getIndexerConfigurationProperties().getSourceSeedData() != null
                        && indexerConfiguration.getIndexerConfigurationProperties().getSourceSeedData().isEnabled();
        this.defaultSeedJsonFile = indexerConfiguration.getIndexerConfigurationProperties().getSourceSeedData() != null ? indexerConfiguration.getIndexerConfigurationProperties().getSourceSeedData().getSeedJsonFile() : null;
    }

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