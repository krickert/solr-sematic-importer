package com.krickert.search.indexer.solr.client;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.solr.client.solrj.impl.HttpListenerFactory;

@Singleton
public class OktaAuthenticatedHttpListenerFactory implements HttpListenerFactory {

    private final OktaAuth oktaAuth;
    private final boolean isEnabled;

    @Inject
    public OktaAuthenticatedHttpListenerFactory(OktaAuth oktaAuth, @Value("${solr-config.destination.connection.authentication.enabled}") boolean isEnabled) {
        this.oktaAuth = oktaAuth;
        this.isEnabled = isEnabled;
    }

    @Override
    public RequestResponseListener get() {
        return new OktaAuthenticatedRequestResponseListener(oktaAuth, isEnabled);
    }
}