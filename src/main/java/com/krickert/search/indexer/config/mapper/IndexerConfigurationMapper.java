package com.krickert.search.indexer.config.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.krickert.search.indexer.config.IndexerConfigurationProperties;
import com.krickert.search.indexer.grpc.IndexerConfiguration;
import com.krickert.search.indexer.grpc.SourceSeedData;

public class IndexerConfigurationMapper {


    // Convert Java to Protobuf
    public static IndexerConfiguration toProtobuf(IndexerConfigurationProperties javaConfig) {
        IndexerConfiguration.Builder protoBuilder = IndexerConfiguration.newBuilder();

        protoBuilder.setVectorGrpcChannel(javaConfig.getVectorGrpcChannel());
        protoBuilder.setChunkerGrpcChannel(javaConfig.getChunkerGrpcChannel());

        if (javaConfig.getSourceSeedData() != null) {
            protoBuilder.setSourceSeedData(toProtobuf(javaConfig.getSourceSeedData()));
        }

        return protoBuilder.build();
    }

    public static SourceSeedData toProtobuf(IndexerConfigurationProperties.SourceSeedData javaSourceSeedData) {
        SourceSeedData.Builder protoBuilder = SourceSeedData.newBuilder();

        protoBuilder.setEnabled(javaSourceSeedData.isEnabled());
        protoBuilder.setSeedJsonFile(javaSourceSeedData.getSeedJsonFile());

        return protoBuilder.build();
    }

    // Convert Protobuf to Java
    public static IndexerConfigurationProperties toJava(IndexerConfiguration protoConfig) {
        IndexerConfigurationProperties javaConfig = new IndexerConfigurationProperties();

        javaConfig.setVectorGrpcChannel(protoConfig.getVectorGrpcChannel());
        javaConfig.setChunkerGrpcChannel(protoConfig.getChunkerGrpcChannel());

        if (protoConfig.hasSourceSeedData()) {
            javaConfig.setSourceSeedData(toJava(protoConfig.getSourceSeedData()));
        }

        return javaConfig;
    }

    public static IndexerConfigurationProperties.SourceSeedData toJava(SourceSeedData protoSourceSeedData) {
        IndexerConfigurationProperties.SourceSeedData javaSourceSeedData = new IndexerConfigurationProperties.SourceSeedData();

        javaSourceSeedData.setEnabled(protoSourceSeedData.getEnabled());
        javaSourceSeedData.setSeedJsonFile(protoSourceSeedData.getSeedJsonFile());

        return javaSourceSeedData;
    }


}