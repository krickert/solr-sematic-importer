package com.krickert.search.indexer.grpc;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.ExposedPort;
import com.google.common.collect.Lists;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;

@Singleton
public class ClientGrpcTestContainers {

    private static final Logger log = LoggerFactory.getLogger(ClientGrpcTestContainers.class);

    private final Map<String, GrpcClientConfig> grpcClientConfigs;
    private final List<GenericContainer<?>> containers = Lists.newArrayList();

    @Inject
    public ClientGrpcTestContainers(Map<String, GrpcClientConfig> grpcClientConfigs) {
        this.grpcClientConfigs = grpcClientConfigs;
        containers.add(createContainer(grpcClientConfigs.get("vectorizer")));
        containers.add(createContainer(grpcClientConfigs.get("chunker")));

    }

    private GenericContainer<?> createContainer(GrpcClientConfig config) {
        DockerImageName imageName = DockerImageName.parse(config.getDockerImageName());

        GenericContainer<?> container = new GenericContainer<>(imageName)
                .withExposedPorts(config.getGrpcMappedPort(), config.getRestMappedPort())
                .withEnv("MY_ENV_VAR", "my-value")
                .withEnv("JAVA_OPTS", "-Xmx5g")
                .withEnv("MICRONAUT_SERVER_NETTY_THREADS", "1000") // Set Netty event loop threads
                .withEnv("MICRONAUT_EXECUTORS_DEFAULT_THREADS", "500")
                .withCreateContainerCmdModifier(cmd -> configureContainer(cmd, config));

        try {
            container.start();
            Integer grpcPort = container.getMappedPort(config.getGrpcMappedPort());
            Integer restPort = container.getMappedPort(config.getRestMappedPort());

            assert container.isCreated();
            assert container.isRunning();

            log.info("Container {} started with gRPC port: {}, REST port: {}",
                    config.getDockerImageName(), grpcPort, restPort);

        } catch (Exception e) {
            log.error("Error when starting container for image: " + config.getDockerImageName(), e);
            container.stop();
            throw e;
        }
        return container;
    }

    private void configureContainer(CreateContainerCmd cmd, GrpcClientConfig config) {
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withMemory(1024 * 1024 * 1024L)
                .withMemorySwap(1024 * 1024 * 1024L)
                .withPortBindings(
                        new PortBinding(Ports.Binding.bindPort(config.getGrpcTestPort()),
                                new ExposedPort(config.getGrpcMappedPort())),
                        new PortBinding(Ports.Binding.bindPort(config.getRestTestPort()),
                                new ExposedPort(config.getRestMappedPort()))
                );
        cmd.withHostConfig(hostConfig);
    }

    @PreDestroy
    public void stopContainers() {
        for (GenericContainer<?> container : containers) {
            container.stop();
        }
    }

    public List<GenericContainer<?>> getContainers() {
        return containers;
    }


    public Map<String, GrpcClientConfig> getGrpcClientConfigs() {
        return grpcClientConfigs;
    }
}