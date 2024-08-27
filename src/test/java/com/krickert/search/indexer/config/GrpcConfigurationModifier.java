package com.krickert.search.indexer.config;

import com.krickert.search.indexer.grpc.ClientGrpcTestContainers;
import com.krickert.search.indexer.grpc.GrpcClientConfig;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class GrpcConfigurationModifier {

    private static final Logger log = LoggerFactory.getLogger(GrpcConfigurationModifier.class);
    private final IndexerConfiguration originalConfiguration;
    private final ClientGrpcTestContainers clientGrpcTestContainers;

    @Inject
    public GrpcConfigurationModifier(IndexerConfiguration originalConfiguration, ClientGrpcTestContainers clientGrpcTestContainers) {
        this.originalConfiguration = originalConfiguration;
        this.clientGrpcTestContainers = clientGrpcTestContainers;
        setConfig();
    }

    public void setConfig() {
        GrpcClientConfig vectorizer = clientGrpcTestContainers.getGrpcClientConfigs().get("vectorizer");
        log.info("Setting test env for vectorizer to {}", vectorizer);
        originalConfiguration.getIndexerConfigurationProperties().setVectorGrpcChannel("localhost:" + vectorizer.getGrpcMappedPort());

        GrpcClientConfig chunker = clientGrpcTestContainers.getGrpcClientConfigs().get("chunker");
        log.info("Setting test env for chunker to {}", chunker);
        originalConfiguration.getIndexerConfigurationProperties().setChunkerGrpcChannel("localhost:" + chunker.getGrpcMappedPort());

        log.info("GRPC setting complete");
    }
}