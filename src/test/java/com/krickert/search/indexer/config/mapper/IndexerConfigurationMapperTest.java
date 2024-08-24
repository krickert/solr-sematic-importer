package com.krickert.search.indexer.config.mapper;

import com.krickert.search.indexer.config.IndexerConfigurationProperties;
import com.krickert.search.indexer.grpc.IndexerConfiguration;
import com.krickert.search.indexer.grpc.SourceSeedData;
import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class IndexerConfigurationMapperTest {

    @Inject
    ApplicationContext applicationContext;

    @Inject
    IndexerConfigurationProperties indexerConfigurationProperties;

    @Test
    void testMappingToProtobuf() {
        IndexerConfiguration protoConfig = IndexerConfigurationMapper.toProtobuf(indexerConfigurationProperties);

        assertNotNull(protoConfig);
        assertEquals(indexerConfigurationProperties.getVectorGrpcChannel(), protoConfig.getVectorGrpcChannel());
        assertEquals(indexerConfigurationProperties.getChunkerGrpcChannel(), protoConfig.getChunkerGrpcChannel());

        if (indexerConfigurationProperties.getSourceSeedData() != null) {
            SourceSeedData protoSourceSeedData = protoConfig.getSourceSeedData();
            IndexerConfigurationProperties.SourceSeedData javaSourceSeedData = indexerConfigurationProperties.getSourceSeedData();

            assertEquals(javaSourceSeedData.isEnabled(), protoSourceSeedData.getEnabled());
            assertEquals(javaSourceSeedData.getSeedJsonFile(), protoSourceSeedData.getSeedJsonFile());
        }
    }

    @Test
    void testMappingFromProtobuf() {
        IndexerConfiguration protoConfig = IndexerConfigurationMapper.toProtobuf(indexerConfigurationProperties);
        IndexerConfigurationProperties javaConfig = IndexerConfigurationMapper.toJava(protoConfig);

        assertNotNull(javaConfig);
        assertEquals(indexerConfigurationProperties.getVectorGrpcChannel(), javaConfig.getVectorGrpcChannel());
        assertEquals(indexerConfigurationProperties.getChunkerGrpcChannel(), javaConfig.getChunkerGrpcChannel());

        if (indexerConfigurationProperties.getSourceSeedData() != null) {
            IndexerConfigurationProperties.SourceSeedData javaSourceSeedData = javaConfig.getSourceSeedData();
            IndexerConfigurationProperties.SourceSeedData originalSourceSeedData = indexerConfigurationProperties.getSourceSeedData();

            assertEquals(originalSourceSeedData.isEnabled(), javaSourceSeedData.isEnabled());
            assertEquals(originalSourceSeedData.getSeedJsonFile(), javaSourceSeedData.getSeedJsonFile());
        }
    }

    @Test
    void testLogicalEquivalency() {
        // Convert to Protobuf
        IndexerConfiguration protoConfig = IndexerConfigurationMapper.toProtobuf(indexerConfigurationProperties);

        // Convert back to Java
        IndexerConfigurationProperties convertedConfig = IndexerConfigurationMapper.toJava(protoConfig);

        // Check for logical equivalency
        assertTrue(areEquivalent(indexerConfigurationProperties, convertedConfig));
    }

    private boolean areEquivalent(IndexerConfigurationProperties original, IndexerConfigurationProperties converted) {
        boolean result = true;

        result &= logComparison("VectorGrpcChannel", original.getVectorGrpcChannel(), converted.getVectorGrpcChannel());
        result &= logComparison("ChunkerGrpcChannel", original.getChunkerGrpcChannel(), converted.getChunkerGrpcChannel());

        if (original.getSourceSeedData() == null && converted.getSourceSeedData() == null) return result;
        if (original.getSourceSeedData() != null && converted.getSourceSeedData() != null) {
            result &= logComparison("SourceSeedData.enabled", original.getSourceSeedData().isEnabled(), converted.getSourceSeedData().isEnabled());
            result &= logComparison("SourceSeedData.seedJsonFile", original.getSourceSeedData().getSeedJsonFile(), converted.getSourceSeedData().getSeedJsonFile());
        } else {
            result = false;  // One is null and the other is not
        }

        return result;
    }

    private <T> boolean logComparison(String fieldName, T originalValue, T convertedValue) {
        boolean isEqual = (originalValue == null && convertedValue == null) || (originalValue != null && originalValue.equals(convertedValue));
        if (!isEqual) {
            System.err.println("Mismatch in field '" + fieldName + "': original=" + originalValue + ", converted=" + convertedValue);
        }
        return isEqual;
    }
}