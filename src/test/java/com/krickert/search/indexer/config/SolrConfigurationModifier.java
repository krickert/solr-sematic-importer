package com.krickert.search.indexer.config;

import com.krickert.search.indexer.solr.SolrTestContainers;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import jakarta.inject.Named;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateHttp2SolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Factory
public class SolrConfigurationModifier {

    private static final Logger log = LoggerFactory.getLogger(SolrConfigurationModifier.class);
    private final IndexerConfiguration originalConfiguration;
    private final String solr9Url;

    public SolrConfigurationModifier(IndexerConfiguration originalConfiguration, SolrTestContainers solrTestContainers) {
        this.originalConfiguration = originalConfiguration;
        String solr7Url = "http://" + solrTestContainers.getContainer7().getHost() + ":" + solrTestContainers.getContainer7().getSolrPort() + "/solr";
        this.solr9Url = "http://" + solrTestContainers.getContainer9().getHost() + ":" + solrTestContainers.getContainer9().getSolrPort() + "/solr";
        log.info("setting the configuration so it has:\n\tsolr7Url: {}\n\tsolr9Url: {}", solr7Url, solr9Url);
        originalConfiguration.getSourceSolrConfiguration().getConnection().setUrl(solr7Url);
        originalConfiguration.getDestinationSolrConfiguration().getConnection().setUrl(solr9Url);
        log.info("Solr testing property setting complete");
    }

    @Replaces(Http2SolrClient.class)
    @Bean
    @Named("solrClient")
    public Http2SolrClient createSolrClient() {
        return new Http2SolrClient.Builder(solr9Url)
                .withDefaultCollection(originalConfiguration.getDestinationSolrConfiguration().getCollection())
                .withFollowRedirects(true)
                .build();
    }

    @Replaces(ConcurrentUpdateHttp2SolrClient.class)
    @Bean
    @Named("concurrentClient")
    public ConcurrentUpdateHttp2SolrClient createConcurrentUpdateSolrClient() {
        Http2SolrClient solrClient = createSolrClient();
        return new ConcurrentUpdateHttp2SolrClient.Builder(solr9Url, solrClient, false)
                .withQueueSize(originalConfiguration.getDestinationSolrConfiguration().getConnection().getQueueSize())
                .withThreadCount(originalConfiguration.getDestinationSolrConfiguration().getConnection().getThreadCount())
                .build();
    }


}