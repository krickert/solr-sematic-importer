package com.krickert.search.indexer.config;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.krickert.search.indexer.config.ConfigTestHelpers.testConnectionString;
import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class SolrConfigurationTest {

    @Inject
    Map<String, SolrConfiguration> configs;

    @Test
    void testSourceConfiguration() {
        assertNotNull(configs);
        assertTrue(configs.containsKey("source"));
        assertNotNull(configs.get("source"));
        SolrConfiguration source = configs.get("source");
        assertEquals("7.7.3", source.getVersion());
        assertEquals("source_collection", source.getCollection());

        SolrConfiguration.Connection sourceConnection = source.getConnection();
        assertNotNull(sourceConnection);
        testConnectionString(sourceConnection);
        assertFalse(sourceConnection.getAuthentication().isEnabled());
    }

    @Test
    void testDestinationConfiguration() {
        assertNotNull(configs);
        assertTrue(configs.containsKey("destination"));
        assertNotNull(configs.get("destination"));
        SolrConfiguration destination = configs.get("destination");
        assertNotNull(destination);
        assertEquals("9.6.1", destination.getVersion());
        assertEquals("destination_collection", destination.getCollection());

        SolrConfiguration.SolrCollectionCreationConfig collectionCreation = destination.getCollectionCreation();
        assertNotNull(collectionCreation);
        assertEquals("classpath:semantic_example.zip", collectionCreation.getCollectionConfigFile());
        assertEquals("semantic_example", collectionCreation.getCollectionConfigName());
        assertEquals(1, collectionCreation.getNumberOfShards());
        assertEquals(2, collectionCreation.getNumberOfReplicas());

        SolrConfiguration.Connection destinationConnection = destination.getConnection();
        assertNotNull(destinationConnection);
        testConnectionString(destinationConnection);
        assertEquals(300, destinationConnection.getQueueSize());
        assertEquals(3, destinationConnection.getThreadCount());

        SolrConfiguration.Connection.Authentication auth = destinationConnection.getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.isEnabled());
        assertEquals("jwt", auth.getType());
        assertEquals("my-client-secret", auth.getClientSecret());
        assertEquals("my-client-id", auth.getClientId());
        assertEquals("https://my-token-url.com/oauth2/some-token/v1/token", auth.getIssuer());
        assertEquals("issuer-auth-id", auth.getIssuerAuthId());
        assertEquals("your-subject", auth.getSubject());
    }
}