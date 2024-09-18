package com.krickert.search.indexer.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.serde.annotation.Serdeable;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;

@Serdeable
@EachProperty("solr-config")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SolrConfiguration {

    @JsonProperty("name")
    private String name;

    @JsonProperty("version")
    private String version;

    @JsonProperty("collection")
    private String collection;

    @JsonProperty("filters")
    private Collection<String> filters = Collections.emptyList();

    @JsonProperty("start")
    private Long start = 0L;

    @JsonProperty("collectionCreation")
    private SolrCollectionCreationConfig collectionCreation;

    @JsonProperty("connection")
    private Connection connection;

    public SolrConfiguration(@Parameter("name") String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public Collection<String> getFilters() {
        return filters;
    }

    public void setFilters(Collection<String> filters) {
        this.filters = filters;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }
    public SolrCollectionCreationConfig getCollectionCreation() {
        return collectionCreation;
    }

    public void setCollectionCreation(SolrCollectionCreationConfig collectionCreation) {
        this.collectionCreation = collectionCreation;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("version", version)
                .add("collection", collection)
                .add("filters", filters)
                .add("start", start)
                .add("collectionCreation", collectionCreation)
                .add("connection", connection)
                .toString();
    }

    @Serdeable
    @ConfigurationProperties("collection-creation")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SolrCollectionCreationConfig {

        @JsonProperty("collectionConfigFile")
        private String collectionConfigFile;

        @JsonProperty("collectionConfigName")
        private String collectionConfigName;

        @JsonProperty("numberOfShards")
        private int numberOfShards;

        @JsonProperty("numberOfReplicas")
        private int numberOfReplicas;

        public String getCollectionConfigFile() {
            return collectionConfigFile;
        }

        public void setCollectionConfigFile(String collectionConfigFile) {
            this.collectionConfigFile = collectionConfigFile;
        }

        public String getCollectionConfigName() {
            return collectionConfigName;
        }

        public void setCollectionConfigName(String collectionConfigName) {
            this.collectionConfigName = collectionConfigName;
        }

        public int getNumberOfShards() {
            return numberOfShards;
        }

        public void setNumberOfShards(int numberOfShards) {
            this.numberOfShards = numberOfShards;
        }

        public int getNumberOfReplicas() {
            return numberOfReplicas;
        }

        public void setNumberOfReplicas(int numberOfReplicas) {
            this.numberOfReplicas = numberOfReplicas;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("collectionConfigFile", collectionConfigFile)
                    .add("collectionConfigName", collectionConfigName)
                    .add("numberOfShards", numberOfShards)
                    .add("numberOfReplicas", numberOfReplicas)
                    .toString();
        }
    }

    @Serdeable
    @ConfigurationProperties("connection")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Connection {

        @JsonProperty("url")
        private String url;

        @JsonProperty("authentication")
        private Authentication authentication;

        @JsonProperty("queue-size")
        private Integer queueSize;

        @JsonProperty("thread-count")
        private Integer threadCount;

        @JsonProperty("pagination-size")
        private Integer paginationSize;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Authentication getAuthentication() {
            return authentication;
        }

        public void setAuthentication(Authentication authentication) {
            this.authentication = authentication;
        }

        public Integer getQueueSize() {
            return queueSize;
        }

        public void setQueueSize(Integer queueSize) {
            this.queueSize = queueSize;
        }

        public Integer getThreadCount() {
            return threadCount;
        }

        public void setThreadCount(Integer threadCount) {
            this.threadCount = threadCount;
        }

        public Integer getPaginationSize() {
            return paginationSize;
        }

        public void setPaginationSize(Integer paginationSize) {
            this.paginationSize = paginationSize;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("url", url)
                    .add("authentication", authentication)
                    .add("queueSize", queueSize)
                    .add("threadCount", threadCount)
                    .add("paginationSize", paginationSize)
                    .toString();
        }

        @Serdeable
        @ConfigurationProperties("authentication")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Authentication {

            @JsonProperty("enabled")
            private boolean enabled;

            @JsonProperty("type")
            private String type;

            @JsonProperty("clientSecret")
            private String clientSecret;

            @JsonProperty("clientId")
            private String clientId;

            @JsonProperty("issuer")
            private String issuer;

            @JsonProperty("issuerAuthId")
            private String issuerAuthId;

            @JsonProperty("subject")
            private String subject;

            @JsonProperty("user=name")
            private String userName;

            @JsonProperty("password")
            private String password;

            @JsonProperty("scope")
            private String scope;

            @JsonProperty("require=dpop")
            private boolean requireDpop = false;

            @JsonProperty(value = "proxy-settings", required = false)
            private ProxySettings proxySettings;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public String getClientSecret() {
                return clientSecret;
            }

            public void setClientSecret(String clientSecret) {
                this.clientSecret = clientSecret;
            }

            public String getClientId() {
                return clientId;
            }

            public void setClientId(String clientId) {
                this.clientId = clientId;
            }

            public String getIssuer() {
                return issuer;
            }

            public void setIssuer(String issuer) {
                this.issuer = issuer;
            }

            public String getIssuerAuthId() {
                return issuerAuthId;
            }

            public void setIssuerAuthId(String issuerAuthId) {
                this.issuerAuthId = issuerAuthId;
            }

            public String getSubject() {
                return subject;
            }

            public void setSubject(String subject) {
                this.subject = subject;
            }

            public String getUserName() {
                return userName;
            }

            public void setUserName(String userName) {
                this.userName = userName;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }

            public String getScope() {
                return scope;
            }

            public void setScope(String scope) {
                this.scope = scope;
            }

            public boolean isRequireDpop() {
                return requireDpop;
            }

            public void setRequireDpop(boolean requireDpop) {
                this.requireDpop = requireDpop;
            }

            public ProxySettings getProxySettings() {
                return proxySettings;
            }
            public void setProxySettings(ProxySettings proxySettings) {
                this.proxySettings = proxySettings;
            }

            @Override
            public String toString() {
                return MoreObjects.toStringHelper(this)
                        .add("enabled", enabled)
                        .add("type", type)
                        .add("clientSecret", clientSecret)
                        .add("clientId", clientId)
                        .add("issuer", issuer)
                        .add("issuerAuthId", issuerAuthId)
                        .add("subject", subject)
                        .add("userName", userName)
                        .add("password", password)
                        .add("scope", scope)
                        .add("requireDpop", requireDpop)
                        .add("proxySettings", proxySettings)
                        .toString();
            }
            @Serdeable
            @ConfigurationProperties("proxy-settings")
            public static class ProxySettings {
                @JsonProperty("enabled")
                public boolean enabled;
                @JsonProperty("host")
                private String host;
                @JsonProperty("port")
                private int port;

                public String getHost() {
                    return host;
                }
                public void setHost(String host) {
                    this.host = host;
                }
                public int getPort() {
                    return port;
                }

                public void setPort(int port) {
                    this.port = port;
                }
                public boolean isEnabled() {
                    return enabled;
                }
                public void setEnabled(boolean enabled) {
                    this.enabled = enabled;
                }

                @Override
                public String toString() {
                    return "ProxySettings{" +
                            "enabled=" + enabled +
                            ", host='" + host + '\'' +
                            ", port=" + port +
                            '}';
                }
            }
            public Proxy createProxy() {
                if (proxySettings == null || !proxySettings.isEnabled()) {
                    return Proxy.NO_PROXY;
                }
                assert proxySettings.getHost() != null;
                assert proxySettings.getPort() >= 0;

                SocketAddress sa = new InetSocketAddress(proxySettings.getHost(), proxySettings.getPort());
                return new Proxy(Proxy.Type.HTTP, sa);

            }
        }
    }
}