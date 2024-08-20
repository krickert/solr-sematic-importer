package com.krickert.search.indexer;

import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.config.IndexerConfigurationProperties;
import com.krickert.search.indexer.config.SolrConfiguration;
import com.krickert.search.indexer.config.VectorConfig;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Value;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

@MicronautTest
public class IndexerConfigurationTest {

    @Inject
    ApplicationContext applicationContext;

    @Inject
    IndexerConfiguration indexerConfiguration;

    @Value("${grpc-test-client-config.chunker.grpc-test-port}")
    String chunkerPort;

    @Value("${grpc-test-client-config.vectorizer.grpc-test-port}")
    String vectorizerPort;

    @Test
    void testIndexerConfigurationPropertiesLoaded() {
        // Assertions for Indexer Configuration Properties
        IndexerConfigurationProperties properties = indexerConfiguration.getIndexerConfigurationProperties();
        Assertions.assertNotNull(properties);
        Assertions.assertNotNull(properties.getVectorGrpcChannel());
        Assertions.assertTrue(properties.getVectorGrpcChannel().startsWith("localhost"));
        Assertions.assertTrue(properties.getVectorGrpcChannel().endsWith(vectorizerPort));
        Assertions.assertNotNull(properties.getChunkerGrpcChannel());
        Assertions.assertTrue(properties.getChunkerGrpcChannel().endsWith(chunkerPort));
        
        // Solr Configuration Assertions
        SolrConfiguration sourceConfig = indexerConfiguration.getSourceSolrConfiguration();
        Assertions.assertNotNull(sourceConfig);
        Assertions.assertEquals("7.7.3", sourceConfig.getVersion());
        Assertions.assertEquals("source_collection", sourceConfig.getCollection());

        SolrConfiguration.Connection sourceConnection = sourceConfig.getConnection();
        Assertions.assertNotNull(sourceConnection);
        String connectionUrl = sourceConnection.getUrl();
        testConnectionString(connectionUrl, sourceConnection);
        Assertions.assertNotNull(sourceConnection.getAuthentication());
        Assertions.assertFalse(sourceConnection.getAuthentication().isEnabled());

        SolrConfiguration destConfig = indexerConfiguration.getDestinationSolrConfiguration();
        Assertions.assertNotNull(destConfig);
        Assertions.assertEquals("9.6.1", destConfig.getVersion());
        Assertions.assertEquals("destination_collection", destConfig.getCollection());
        Assertions.assertEquals("classpath:semantic_example.zip", destConfig.getCollectionConfigFile());

        SolrConfiguration.Connection destConnection = destConfig.getConnection();
        Assertions.assertNotNull(destConnection);
        testConnectionString(destConnection.getUrl(), destConnection);
        Assertions.assertNotNull(destConnection.getAuthentication());
        
        SolrConfiguration.Connection.Authentication destAuth = destConnection.getAuthentication();
        Assertions.assertTrue(destAuth.isEnabled());
        Assertions.assertEquals("jwt", destAuth.getType());
        Assertions.assertEquals("my-client-secret", destAuth.getClientSecret());
        Assertions.assertEquals("my-client-id", destAuth.getClientId());
        Assertions.assertEquals("https://my-token-url.com/oauth2/some-token/v1/token", destAuth.getIssuer());
        Assertions.assertEquals("issuer-auth-id", destAuth.getIssuerAuthId());
        Assertions.assertEquals("your-subject", destAuth.getSubject());

        // Vector Configuration Assertions
        Map<String, VectorConfig> vectorConfigMap = indexerConfiguration.getVectorConfig();
        Assertions.assertNotNull(vectorConfigMap);

        VectorConfig titleConfig = vectorConfigMap.get("title");
        Assertions.assertNotNull(titleConfig);
        Assertions.assertEquals(30, titleConfig.getChunkOverlap());
        Assertions.assertEquals(300, titleConfig.getChunkSize());
        Assertions.assertEquals("mini-LM", titleConfig.getModel());
        Assertions.assertEquals(384, titleConfig.getDimensions());
        Assertions.assertEquals("title_vector", titleConfig.getDestinationCollection());
        Assertions.assertTrue(titleConfig.isDestinationCollectionCreate());
        Assertions.assertEquals("title_mini_lm", titleConfig.getDestinationCollectionVectorFieldName());

        VectorConfig bodyConfig = vectorConfigMap.get("body");
        Assertions.assertNotNull(bodyConfig);
        Assertions.assertEquals(30, bodyConfig.getChunkOverlap());
        Assertions.assertEquals(300, bodyConfig.getChunkSize());
        Assertions.assertEquals("mini-LM", bodyConfig.getModel());
        Assertions.assertEquals(384, bodyConfig.getDimensions());
        Assertions.assertEquals("body_vector", bodyConfig.getDestinationCollection());
        Assertions.assertTrue(bodyConfig.isDestinationCollectionCreate());
        Assertions.assertEquals("title_mini_lm", bodyConfig.getDestinationCollectionVectorFieldName());
    }

    private static void testConnectionString(String connectionUrl, SolrConfiguration.Connection sourceConnection) {
        Assertions.assertTrue(connectionUrl.startsWith("http://localhost:"));
        Assertions.assertTrue(connectionUrl.endsWith("/solr"));
        String port = sourceConnection.getUrl().replaceAll(".*:(\\d+).*", "$1");
        Assertions.assertEquals("http://localhost:" + Integer.parseInt(port) + "/solr", connectionUrl);
    }
}