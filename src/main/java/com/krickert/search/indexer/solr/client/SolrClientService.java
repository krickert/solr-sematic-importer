package com.krickert.search.indexer.solr.client;

import com.krickert.search.indexer.config.IndexerConfiguration;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateHttp2SolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Factory
public class SolrClientService {
    private static final Logger log = LoggerFactory.getLogger(SolrClientService.class);

    private final OktaAuthenticatedHttpListenerFactory authenticatedRequestResponseListener;

    @Inject
    public SolrClientService(OktaAuthenticatedHttpListenerFactory authenticatedRequestResponseListener) {
        this.authenticatedRequestResponseListener = authenticatedRequestResponseListener;
    }

    @Bean
    @Named("solrClient")
    public Http2SolrClient createSolrClient(IndexerConfiguration indexerConfiguration) {
        log.info("Creating destination solr client");
        String solrUrl = indexerConfiguration.getDestinationSolrConfiguration().getConnection().getUrl();
        String collection = indexerConfiguration.getDestinationSolrConfiguration().getCollection();
        Http2SolrClient client = new Http2SolrClient.Builder(solrUrl)
                .withDefaultCollection(collection)
                .withFollowRedirects(true)
                .build();
        client.addListenerFactory(authenticatedRequestResponseListener);
        log.info("Destination solr client created.");
        return client;
    }

    @Bean
    @Named("concurrentClient")
    public ConcurrentUpdateHttp2SolrClient createConcurrentUpdateSolrClient(IndexerConfiguration indexerConfiguration, @Named("solrClient") Http2SolrClient solrClient) {
        String solrUrl = indexerConfiguration.getDestinationSolrConfiguration().getConnection().getUrl();
        return new ConcurrentUpdateHttp2SolrClient.Builder(solrUrl, solrClient, false)
                .withQueueSize(indexerConfiguration.getDestinationSolrConfiguration().getConnection().getQueueSize())
                .withThreadCount(indexerConfiguration.getDestinationSolrConfiguration().getConnection().getThreadCount())
                .build();
    }

}