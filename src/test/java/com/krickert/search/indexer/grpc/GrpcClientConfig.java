package com.krickert.search.indexer.grpc;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.context.annotation.EachProperty;

@EachProperty("grpc-test-client-config")
@Introspected
public class GrpcClientConfig {

    private Integer grpcTestPort;


    private Integer grpcMappedPort;
    private Integer restTestPort;
    private Integer restMappedPort;
    private String dockerImageName;

    public Integer getGrpcMappedPort() {
        return grpcMappedPort;
    }

    public void setGrpcMappedPort(Integer grpcMappedPort) {
        this.grpcMappedPort = grpcMappedPort;
    }

    public Integer getRestMappedPort() {
        return restMappedPort;
    }

    public void setRestMappedPort(Integer restMappedPort) {
        this.restMappedPort = restMappedPort;
    }

    public Integer getGrpcTestPort() {
        return grpcTestPort;
    }

    public void setGrpcTestPort(Integer grpcTestPort) {
        this.grpcTestPort = grpcTestPort;
    }

    public Integer getRestTestPort() {
        return restTestPort;
    }

    public void setRestTestPort(Integer restTestPort) {
        this.restTestPort = restTestPort;
    }

    public String getDockerImageName() {
        return dockerImageName;
    }

    public void setDockerImageName(String dockerImageName) {
        this.dockerImageName = dockerImageName;
    }

    @Override
    public String toString() {
        return "GrpcClientConfig{" +
                "grpcTestPort=" + grpcTestPort +
                ", grpcMappedPort=" + grpcMappedPort +
                ", restTestPort=" + restTestPort +
                ", restMappedPort=" + restMappedPort +
                ", dockerImageName='" + dockerImageName + '\'' +
                '}';
    }
}