package com.krickert.search.indexer.config.mapper;

import com.krickert.search.indexer.config.SolrConfiguration;
import com.krickert.search.indexer.grpc.*;

import java.util.Map;
import java.util.stream.Collectors;

public class SolrConfigurationMapper {
    public static SolrConfigurationMap toProtobuf(Map<String, SolrConfiguration> configs) {
        Map<String, SolrConfig> protoConfigs = configs.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> toProtobuf(entry.getValue())));
        return SolrConfigurationMap.newBuilder().putAllConfigs(protoConfigs).build();
    }

    private static SolrConfig toProtobuf(SolrConfiguration config) {
        SolrConfig.Builder builder = SolrConfig.newBuilder()
                .setName(config.getName() != null ? config.getName() : "")
                .setVersion(config.getVersion() != null ? config.getVersion() : "")
                .setCollection(config.getCollection() != null ? config.getCollection() : "")
                .setConnection(toProtobuf(config.getConnection()));

        if (config.getCollectionCreation() != null) {
            builder.setCollectionCreation(toProtobuf(config.getCollectionCreation()));
        }
        return builder.build();
    }

    private static SolrCollectionCreationConfig toProtobuf(SolrConfiguration.SolrCollectionCreationConfig config) {
        return SolrCollectionCreationConfig.newBuilder()
                .setCollectionConfigFile(config.getCollectionConfigFile() != null ? config.getCollectionConfigFile() : "")
                .setCollectionConfigName(config.getCollectionConfigName() != null ? config.getCollectionConfigName() : "")
                .setNumberOfShards(config.getNumberOfShards())
                .setNumberOfReplicas(config.getNumberOfReplicas())
                .build();
    }

    private static Connection toProtobuf(SolrConfiguration.Connection config) {
        Connection.Builder builder = Connection.newBuilder()
                .setUrl(config.getUrl() != null ? config.getUrl() : "")
                .setQueueSize(config.getQueueSize() != null ? config.getQueueSize() : 0)
                .setThreadCount(config.getThreadCount() != null ? config.getThreadCount() : 0);

        if (config.getAuthentication() != null) {
            builder.setAuthentication(toProtobuf(config.getAuthentication()));
        } else {
            builder.setAuthentication(Authentication.getDefaultInstance());
        }

        return builder.build();
    }

    private static Authentication toProtobuf(SolrConfiguration.Connection.Authentication auth) {
        Authentication.Builder builder = Authentication.newBuilder()
                .setEnabled(auth.isEnabled())
                .setType(auth.getType() != null ? auth.getType() : "")
                .setClientSecret(auth.getClientSecret() != null ? auth.getClientSecret() : "")
                .setClientId(auth.getClientId() != null ? auth.getClientId() : "")
                .setIssuer(auth.getIssuer() != null ? auth.getIssuer() : "")
                .setIssuerAuthId(auth.getIssuerAuthId() != null ? auth.getIssuerAuthId() : "")
                .setSubject(auth.getSubject() != null ? auth.getSubject() : "");

        return builder.build();
    }

    public static Map<String, SolrConfiguration> fromProtobuf(SolrConfigurationMap protoMap) {
        return protoMap.getConfigsMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> fromProtobuf(entry.getValue())));
    }

    private static SolrConfiguration fromProtobuf(SolrConfig proto) {
        SolrConfiguration config = new SolrConfiguration(proto.getName());
        config.setVersion(proto.getVersion().isEmpty() ? null : proto.getVersion());
        config.setCollection(proto.getCollection().isEmpty() ? null : proto.getCollection());
        config.setConnection(fromProtobuf(proto.getConnection()));

        if (proto.hasCollectionCreation()) {
            config.setCollectionCreation(fromProtobuf(proto.getCollectionCreation()));
        }
        return config;
    }

    private static SolrConfiguration.SolrCollectionCreationConfig fromProtobuf(SolrCollectionCreationConfig proto) {
        SolrConfiguration.SolrCollectionCreationConfig config = new SolrConfiguration.SolrCollectionCreationConfig();
        config.setCollectionConfigFile(proto.getCollectionConfigFile().isEmpty() ? null : proto.getCollectionConfigFile());
        config.setCollectionConfigName(proto.getCollectionConfigName().isEmpty() ? null : proto.getCollectionConfigName());
        config.setNumberOfShards(proto.getNumberOfShards());
        config.setNumberOfReplicas(proto.getNumberOfReplicas());
        return config;
    }

    private static SolrConfiguration.Connection fromProtobuf(Connection proto) {
        SolrConfiguration.Connection config = new SolrConfiguration.Connection();
        config.setUrl(proto.getUrl().isEmpty() ? null : proto.getUrl());
        config.setQueueSize(proto.getQueueSize() == 0 ? null : proto.getQueueSize());
        config.setThreadCount(proto.getThreadCount() == 0 ? null : proto.getThreadCount());
        config.setAuthentication(fromProtobuf(proto.getAuthentication()));
        return config;
    }

    private static SolrConfiguration.Connection.Authentication fromProtobuf(Authentication proto) {
        SolrConfiguration.Connection.Authentication auth = new SolrConfiguration.Connection.Authentication();
        auth.setEnabled(proto.getEnabled());
        auth.setType(proto.getType().isEmpty() ? null : proto.getType());
        auth.setClientSecret(proto.getClientSecret().isEmpty() ? null : proto.getClientSecret());
        auth.setClientId(proto.getClientId().isEmpty() ? null : proto.getClientId());
        auth.setIssuer(proto.getIssuer().isEmpty() ? null : proto.getIssuer());
        auth.setIssuerAuthId(proto.getIssuerAuthId().isEmpty() ? null : proto.getIssuerAuthId());
        auth.setSubject(proto.getSubject().isEmpty() ? null : proto.getSubject());
        return auth;
    }
}