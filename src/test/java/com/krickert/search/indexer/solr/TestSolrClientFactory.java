package com.krickert.search.indexer.solr;

import com.krickert.search.indexer.config.IndexerConfiguration;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateHttp2SolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.testcontainers.containers.SolrContainer;

import java.util.concurrent.TimeUnit;

@Requires(notEnv = Environment.DEVELOPMENT)
@Factory
public class TestSolrClientFactory {

    @Inject
    SolrTestContainers solrTestContainers;

    @Inject
    IndexerConfiguration indexerConfiguration;



    @Singleton
    @Replaces(SolrClient.class) // Replace the existing SolrClient bean
    public SolrClient customSolrClient() {
        SolrContainer container9 = solrTestContainers.getContainer9();
        return new Http2SolrClient.Builder("http://" + container9.getHost() + ":" + container9.getSolrPort() + "/solr")
                .build();
    }

    @Singleton
    @Replaces(ConcurrentUpdateHttp2SolrClient.class)
    public ConcurrentUpdateHttp2SolrClient createConcurrentUpdateSolrClient(
            Http2SolrClient solrClient,
            @Value("${solr-config.destination.connection.queue-size}") Integer queueSize,
            @Value("${solr-config.destination.connection.thread-count}") Integer threadCount) {
        SolrContainer container9 = solrTestContainers.getContainer9();
        return new ConcurrentUpdateHttp2SolrClient.Builder("http://" + container9.getHost() + ":" + container9.getSolrPort() + "/solr", solrClient, false)
                .withQueueSize(queueSize)
                .withThreadCount(threadCount)
                .build();
    }
}