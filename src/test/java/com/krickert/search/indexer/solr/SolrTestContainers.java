package com.krickert.search.indexer.solr;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Requires(notEnv = Environment.DEVELOPMENT)
@Singleton
public class SolrTestContainers {
    private static final Logger log = LoggerFactory.getLogger(SolrTestContainers.class);
    private final SolrContainer container7;
    private final SolrContainer container9;

    public SolrTestContainers(
            @Value("${solr7-test-port}") Integer solr7Port,
            @Value("${solr9-test-port}") Integer solr9Port) {

        DockerImageName SOLR7_IMAGE = DockerImageName.parse("solr:7.7.3");
        this.container7 = createContainer(SOLR7_IMAGE, solr7Port);
        DockerImageName SOLR9_IMAGE = DockerImageName.parse("solr:9.6.1");
        this.container9 = createContainer(SOLR9_IMAGE, solr9Port);
    }

    private SolrContainer createContainer(DockerImageName image, Integer port) {
        // Create the solr container.
        SolrContainer container = new SolrContainer(image)
                .withZookeeper(true)
                .withAccessToHost(true);
        // Start the container. This step might take some time...
        container.start();
        assertTrue(container.isRunning());
        log.info("started solr image: {}, port: {}", image, container.getSolrPort());
        return container;
    }

    @PreDestroy
    void destoryContainers() {
        container7.stop();
        container9.stop();
    }
    public SolrContainer getContainer7() {
        return container7;
    }

    public SolrContainer getContainer9() {
        return container9;
    }

    @Override
    public String toString() {
        return "SolrTestContainers{" +
                "container7=" + container7 +
                ", container9=" + container9 +
                '}';
    }
}
