package com.krickert.search.indexer.grpc;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@Property(name = "micronaut.config.files")
public class GrpcClientConfigTest {

    @Inject
    Map<String, GrpcClientConfig> grpcClientConfigs;

    @Test
    void testChunkerConfiguration() {
        GrpcClientConfig chunkerConfig = grpcClientConfigs.get("chunker");
        assertNotNull(chunkerConfig);
        assertNotNull(chunkerConfig.getGrpcTestPort());
        assertTrue(chunkerConfig.getGrpcTestPort() > 0);
        assertEquals(50403, chunkerConfig.getGrpcMappedPort());
        assertNotNull(chunkerConfig.getRestTestPort());
        assertTrue(chunkerConfig.getRestTestPort() > 0);
        assertEquals(60403, chunkerConfig.getRestMappedPort());
        assertEquals("krickert/chunker:1.0-SNAPSHOT", chunkerConfig.getDockerImageName());
    }

    @Test
    void testVectorizerConfiguration() {
        GrpcClientConfig vectorizerConfig = grpcClientConfigs.get("vectorizer");
        assertNotNull(vectorizerConfig);
        assertNotNull(vectorizerConfig.getGrpcTestPort());
        assertTrue(vectorizerConfig.getGrpcTestPort() > 0);
        assertNotNull(vectorizerConfig.getRestTestPort());
        assertTrue(vectorizerConfig.getRestTestPort() > 0);
        assertEquals("krickert/vectorizer:1.0-SNAPSHOT", vectorizerConfig.getDockerImageName());
    }
}