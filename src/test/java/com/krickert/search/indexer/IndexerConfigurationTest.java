package com.krickert.search.indexer;

import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.config.IndexerConfigurationProperties;
import com.krickert.search.indexer.config.SolrConfiguration;
import com.krickert.search.indexer.config.VectorConfig;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Value;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.Assert;
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
        Assertions.assertNull(sourceConnection.getQueueSize());
        Assertions.assertNull(sourceConnection.getThreadCount());
        SolrConfiguration destConfig = indexerConfiguration.getDestinationSolrConfiguration();
        Assertions.assertNotNull(destConfig);
        Assertions.assertEquals("9.6.1", destConfig.getVersion());
        Assertions.assertEquals("destination_collection", destConfig.getCollection());

        Assertions.assertEquals("classpath:semantic_example.zip", destConfig.getCollectionCreation().getCollectionConfigFile());
        Assertions.assertEquals("vector_config", destConfig.getCollectionCreation().getCollectionConfigName());
        Assertions.assertEquals(1, destConfig.getCollectionCreation().getNumberOfShards());
        Assertions.assertEquals(2, destConfig.getCollectionCreation().getNumberOfReplicas());

        SolrConfiguration.Connection destConnection = destConfig.getConnection();
        Assertions.assertNotNull(destConnection);
        testConnectionString(destConnection.getUrl(), destConnection);
        Assertions.assertNotNull(destConnection.getQueueSize());
        Assertions.assertEquals(300, destConnection.getQueueSize());
        Assertions.assertNotNull(destConnection.getThreadCount());
        Assertions.assertEquals(3, destConnection.getThreadCount());
        Assertions.assertNotNull(destConnection.getAuthentication());
        Assertions.assertEquals(1, destConfig.getCollectionCreation().getNumberOfShards());
        Assertions.assertEquals(2, destConfig.getCollectionCreation().getNumberOfReplicas());
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
        //Titles don't chunk and nor should you
        Assertions.assertFalse(titleConfig.getChunkField());
        Assertions.assertNull(titleConfig.getChunkOverlap());
        Assertions.assertNull(titleConfig.getChunkSize());
        Assertions.assertEquals("mini-LM", titleConfig.getModel());
           Assertions.assertFalse(titleConfig.getChunkField());
        VectorConfig bodyConfig = vectorConfigMap.get("body");
        Assertions.assertNotNull(bodyConfig);
        Assertions.assertTrue(bodyConfig.getChunkField());
        Assertions.assertEquals(30, bodyConfig.getChunkOverlap());
        Assertions.assertEquals(300, bodyConfig.getChunkSize());
        Assertions.assertEquals("mini-LM", bodyConfig.getModel());
        Assertions.assertEquals("body_vectors", bodyConfig.getDestinationCollection());
        Assertions.assertEquals("classpath:default_base_config.zip", bodyConfig.getCollectionCreation().getCollectionConfigFile());
        Assertions.assertEquals("vector_config", bodyConfig.getCollectionCreation().getCollectionConfigName());
        Assertions.assertEquals(1, bodyConfig.getCollectionCreation().getNumberOfShards());
        Assertions.assertEquals(2, bodyConfig.getCollectionCreation().getNumberOfReplicas());
    }
    private static void testConnectionString(String connectionUrl, SolrConfiguration.Connection sourceConnection) {
        Assertions.assertTrue(connectionUrl.startsWith("http://localhost:"));
        Assertions.assertTrue(connectionUrl.endsWith("/solr"));
        String port = sourceConnection.getUrl().replaceAll(".*:(\\d+).*", "$1");
        Assertions.assertEquals("http://localhost:" + Integer.parseInt(port) + "/solr", connectionUrl);
    }
}