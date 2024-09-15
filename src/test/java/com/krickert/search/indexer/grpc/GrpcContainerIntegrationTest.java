package com.krickert.search.indexer.grpc;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@MicronautTest
@Testcontainers
public class GrpcContainerIntegrationTest {

    @Inject
    ClientTestContainers clientTestContainers;

    @Test
    void test() {
        Assertions.assertNotNull(clientTestContainers);
        for (GenericContainer<?> container : clientTestContainers.getContainers()) {
            Assertions.assertNotNull(container);
            Assertions.assertTrue(container.isRunning());
            Assertions.assertTrue(container.isCreated());
            Assertions.assertNotNull(container.getContainerId());
        }
    }

}
