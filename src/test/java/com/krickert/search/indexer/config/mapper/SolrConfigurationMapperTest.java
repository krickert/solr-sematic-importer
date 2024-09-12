package com.krickert.search.indexer.config.mapper;

import com.krickert.search.indexer.config.SolrConfiguration;
import com.krickert.search.indexer.grpc.*;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class SolrConfigurationMapperTest {

    private static final Logger LOGGER = Logger.getLogger(SolrConfigurationMapperTest.class.getName());

    @Inject
    Map<String, SolrConfiguration> configs;

    @Test
    void testMappingToProtobuf() {
        SolrConfigurationMap protoMap = SolrConfigurationMapper.toProtobuf(configs);
        assertNotNull(protoMap);

        // Ensure the map contains both source and destination configurations
        assertTrue(protoMap.getConfigsMap().containsKey("source"));
        assertTrue(protoMap.getConfigsMap().containsKey("destination"));

        // Validate source configuration
        SolrConfig sourceProto = protoMap.getConfigsOrThrow("source");
        SolrConfiguration sourceConfig = configs.get("source");
        assertEquals(sourceConfig.getVersion(), sourceProto.getVersion());
        assertEquals(sourceConfig.getCollection(), sourceProto.getCollection());
        assertEquals(sourceConfig.getConnection().getUrl(), sourceProto.getConnection().getUrl());
        Authentication so = sourceProto.getConnection().getAuthentication();
        assertTrue(so.getEnabled());
        assertEquals("basic", so.getType());
        assertEquals("dummy_user", so.getUserName());
        assertEquals("dummy_password", so.getPassword());
        // Validate destination configuration
        SolrConfig destinationProto = protoMap.getConfigsOrThrow("destination");
        SolrConfiguration destinationConfig = configs.get("destination");
        assertEquals(destinationConfig.getVersion(), destinationProto.getVersion());
        assertEquals(destinationConfig.getCollection(), destinationProto.getCollection());

        SolrCollectionCreationConfig collectionCreationProto = destinationProto.getCollectionCreation();
        SolrConfiguration.SolrCollectionCreationConfig collectionCreationConfig = destinationConfig.getCollectionCreation();
        assertNotNull(collectionCreationProto);
        assertEquals(collectionCreationConfig.getCollectionConfigFile(), collectionCreationProto.getCollectionConfigFile());
        assertEquals(collectionCreationConfig.getCollectionConfigName(), collectionCreationProto.getCollectionConfigName());
        assertEquals(collectionCreationConfig.getNumberOfShards(), collectionCreationProto.getNumberOfShards());
        assertEquals(collectionCreationConfig.getNumberOfReplicas(), collectionCreationProto.getNumberOfReplicas());

        Connection destinationConnectionProto = destinationProto.getConnection();
        SolrConfiguration.Connection destinationConnectionConfig = destinationConfig.getConnection();
        assertEquals(destinationConnectionConfig.getUrl(), destinationConnectionProto.getUrl());
        assertEquals(destinationConnectionConfig.getQueueSize(), destinationConnectionProto.getQueueSize());
        assertEquals(destinationConnectionConfig.getThreadCount(), destinationConnectionProto.getThreadCount());
    }

    @Test
    void testMappingFromProtobuf() {
        SolrConfigurationMap protoMap = SolrConfigurationMapper.toProtobuf(configs);
        Map<String, SolrConfiguration> configMap = SolrConfigurationMapper.fromProtobuf(protoMap);

        // Ensure the map contains both source and destination configurations
        assertTrue(configMap.containsKey("source"));
        assertTrue(configMap.containsKey("destination"));

        // Validate source configuration
        SolrConfiguration sourceConfig = configMap.get("source");
        SolrConfiguration originalSourceConfig = configs.get("source");
        assertEquals(originalSourceConfig.getVersion(), sourceConfig.getVersion());
        assertEquals(originalSourceConfig.getCollection(), sourceConfig.getCollection());
        assertEquals(originalSourceConfig.getConnection().getUrl(), sourceConfig.getConnection().getUrl());
        assertEquals(originalSourceConfig.getConnection().getAuthentication().isEnabled(), sourceConfig.getConnection().getAuthentication().isEnabled());
        assertEquals(originalSourceConfig.getConnection().getAuthentication().getUserName(), sourceConfig.getConnection().getAuthentication().getUserName());
        assertEquals(originalSourceConfig.getConnection().getAuthentication().getPassword(), sourceConfig.getConnection().getAuthentication().getPassword());
        assertEquals(originalSourceConfig.getConnection().getAuthentication().getType(), sourceConfig.getConnection().getAuthentication().getType());

        // Validate destination configuration
        SolrConfiguration destinationConfig = configMap.get("destination");
        SolrConfiguration originalDestinationConfig = configs.get("destination");
        assertEquals(originalDestinationConfig.getVersion(), destinationConfig.getVersion());
        assertEquals(originalDestinationConfig.getCollection(), destinationConfig.getCollection());

        SolrConfiguration.SolrCollectionCreationConfig collectionCreationConfig = destinationConfig.getCollectionCreation();
        SolrConfiguration.SolrCollectionCreationConfig originalCollectionCreationConfig = originalDestinationConfig.getCollectionCreation();
        assertNotNull(collectionCreationConfig);
        assertEquals(originalCollectionCreationConfig.getCollectionConfigFile(), collectionCreationConfig.getCollectionConfigFile());
        assertEquals(originalCollectionCreationConfig.getCollectionConfigName(), collectionCreationConfig.getCollectionConfigName());
        assertEquals(originalCollectionCreationConfig.getNumberOfShards(), collectionCreationConfig.getNumberOfShards());
        assertEquals(originalCollectionCreationConfig.getNumberOfReplicas(), collectionCreationConfig.getNumberOfReplicas());

        SolrConfiguration.Connection destinationConnection = destinationConfig.getConnection();
        SolrConfiguration.Connection originalDestinationConnection = originalDestinationConfig.getConnection();
        assertEquals(originalDestinationConnection.getUrl(), destinationConnection.getUrl());
        assertEquals(originalDestinationConnection.getQueueSize(), destinationConnection.getQueueSize());
        assertEquals(originalDestinationConnection.getThreadCount(), destinationConnection.getThreadCount());
    }

    @Test
    void testLogicalEquivalency() {
        // Convert to Protobuf
        SolrConfigurationMap protoMap = SolrConfigurationMapper.toProtobuf(configs);

        // Convert back to the original objects
        Map<String, SolrConfiguration> configMap = SolrConfigurationMapper.fromProtobuf(protoMap);

        // Ensure all configurations are logically equivalent
        for (Map.Entry<String, SolrConfiguration> entry : configs.entrySet()) {
            String key = entry.getKey();
            SolrConfiguration originalConfig = entry.getValue();

            SolrConfig convertedProto = protoMap.getConfigsMap().get(key);
            SolrConfiguration convertedConfig = configMap.get(key);

            assertTrue(areEquivalent(originalConfig, convertedProto),
                    "Protobuf and original configurations for key " + key + " are not logically equivalent.");

            assertTrue(compareConfigurations(originalConfig, convertedConfig),
                    "Original and converted configurations for key " + key + " are not logically equivalent.");
        }
    }

    private boolean areEquivalent(SolrConfiguration config, SolrConfig proto) {
        if (config == null && proto == null) return true;
        if (config == null || proto == null) return false;

        boolean result = true;

        result &= logComparison("Name", config.getName(), proto.getName());
        result &= logComparison("Version", config.getVersion(), proto.getVersion());
        result &= logComparison("Collection", config.getCollection(), proto.getCollection());

        result &= areEquivalent(config.getCollectionCreation(), proto.hasCollectionCreation() ? proto.getCollectionCreation() : null);
        result &= areEquivalent(config.getConnection(), proto.getConnection());

        return result;
    }

    private boolean areEquivalent(SolrConfiguration.SolrCollectionCreationConfig config, SolrCollectionCreationConfig proto) {
        if (config == null && proto == null) return true;
        if (config == null || proto == null) return false;

        boolean result = true;

        result &= logComparison("CollectionConfigFile", config.getCollectionConfigFile(), proto.getCollectionConfigFile());
        result &= logComparison("CollectionConfigName", config.getCollectionConfigName(), proto.getCollectionConfigName());
        result &= logComparison("NumberOfShards", config.getNumberOfShards(), proto.getNumberOfShards());
        result &= logComparison("NumberOfReplicas", config.getNumberOfReplicas(), proto.getNumberOfReplicas());

        return result;
    }

    private boolean areEquivalent(SolrConfiguration.Connection config, Connection proto) {
        if (config == null && proto == null) return true;
        if (config == null || proto == null) return false;

        boolean result = true;

        result &= logComparison("URL", config.getUrl(), proto.getUrl());

        // Note: Proto defaults int fields to 0, so we treat 0 as null equivalent
        result &= logComparison("QueueSize", config.getQueueSize() == null ? 0 : config.getQueueSize(), proto.getQueueSize());
        result &= logComparison("ThreadCount", config.getThreadCount() == null ? 0 : config.getThreadCount(), proto.getThreadCount());

        result &= areEquivalent(config.getAuthentication(), proto.getAuthentication());

        return result;
    }

    private boolean areEquivalent(SolrConfiguration.Connection.Authentication config, Authentication proto) {
        if (config == null && proto == null) return true;
        if (config == null || proto == null) return false;

        boolean result = true;

        result &= logComparison("Enabled", config.isEnabled(), proto.getEnabled());
        result &= logComparison("Type", config.getType(), proto.getType().isEmpty() ? null : proto.getType());
        result &= logComparison("ClientSecret", config.getClientSecret(), proto.getClientSecret().isEmpty() ? null : proto.getClientSecret());
        result &= logComparison("ClientId", config.getClientId(), proto.getClientId().isEmpty() ? null : proto.getClientId());
        result &= logComparison("Issuer", config.getIssuer(), proto.getIssuer().isEmpty() ? null : proto.getIssuer());
        result &= logComparison("IssuerAuthId", config.getIssuerAuthId(), proto.getIssuerAuthId().isEmpty() ? null : proto.getIssuerAuthId());
        result &= logComparison("Subject", config.getSubject(), proto.getSubject().isEmpty() ? null : proto.getSubject());
        result &= logComparison("UserName", config.getUserName(), proto.getUserName().isEmpty() ? null : proto.getUserName());
        result &= logComparison("Password", config.getPassword(), proto.getPassword().isEmpty() ? null : proto.getPassword());
        return result;
    }

    private boolean compareConfigurations(SolrConfiguration original, SolrConfiguration converted) {
        if (original == null && converted == null) return true;
        if (original == null || converted == null) return false;

        boolean result = true;

        result &= logComparison("Name", original.getName(), converted.getName());
        result &= logComparison("Version", original.getVersion(), converted.getVersion());
        result &= logComparison("Collection", original.getCollection(), converted.getCollection());

        result &= compareCollectionCreation(original.getCollectionCreation(), converted.getCollectionCreation());
        result &= compareConnection(original.getConnection(), converted.getConnection());

        return result;
    }

    private boolean compareCollectionCreation(SolrConfiguration.SolrCollectionCreationConfig original, SolrConfiguration.SolrCollectionCreationConfig converted) {
        if (original == null && converted == null) return true;
        if (original == null || converted == null) return false;

        boolean result = true;

        result &= logComparison("CollectionConfigFile", original.getCollectionConfigFile(), converted.getCollectionConfigFile());
        result &= logComparison("CollectionConfigName", original.getCollectionConfigName(), converted.getCollectionConfigName());
        result &= logComparison("NumberOfShards", original.getNumberOfShards(), converted.getNumberOfShards());
        result &= logComparison("NumberOfReplicas", original.getNumberOfReplicas(), converted.getNumberOfReplicas());

        return result;
    }

    private boolean compareConnection(SolrConfiguration.Connection original, SolrConfiguration.Connection converted) {
        if (original == null && converted == null) return true;
        if (original == null || converted == null) return false;

        boolean result = true;

        result &= logComparison("URL", original.getUrl(), converted.getUrl());

        // Note: handle nulls for integers
        result &= logComparison("QueueSize", original.getQueueSize() == null ? 0 : original.getQueueSize(), converted.getQueueSize() == null ? 0 : converted.getQueueSize());
        result &= logComparison("ThreadCount", original.getThreadCount() == null ? 0 : original.getThreadCount(), converted.getThreadCount() == null ? 0 : converted.getThreadCount());

        result &= compareAuthentication(original.getAuthentication(), converted.getAuthentication());

        return result;
    }

    private boolean compareAuthentication(SolrConfiguration.Connection.Authentication original, SolrConfiguration.Connection.Authentication converted) {
        if (original == null && converted == null) return true;
        if (original == null || converted == null) return false;

        boolean result = true;

        result &= logComparison("Enabled", original.isEnabled(), converted.isEnabled());
        result &= logComparison("Type", original.getType(), converted.getType());
        result &= logComparison("ClientSecret", original.getClientSecret(), converted.getClientSecret());
        result &= logComparison("ClientId", original.getClientId(), converted.getClientId());
        result &= logComparison("Issuer", original.getIssuer(), converted.getIssuer());
        result &= logComparison("IssuerAuthId", original.getIssuerAuthId(), converted.getIssuerAuthId());
        result &= logComparison("Subject", original.getSubject(), converted.getSubject());

        return result;
    }

    private <T> boolean logComparison(String fieldName, T originalValue, T convertedValue) {
        boolean isEqual = (originalValue == null && convertedValue == null) || (originalValue != null && originalValue.equals(convertedValue));
        if (!isEqual) {
            LOGGER.warning("Mismatch in field '" + fieldName + "': original=" + originalValue + ", converted=" + convertedValue);
        }
        return isEqual;
    }
}