//package com.krickert.search.indexer.grpc;
//
//
//import com.krickert.search.service.*;
//import org.junit.jupiter.api.Test;
//
//
//public class EmbeddingServiceMockTest {
//
//    @Test
//    void testMock() {
//            EmbeddingServiceGrpc.EmbeddingServiceBlockingStub mockStub = new EmbeddingServiceMock().createMock();
//
//            // Test createEmbeddingsVector
//            EmbeddingsVectorRequest vectorRequest = EmbeddingsVectorRequest.newBuilder()
//                    .setText("dummy text")
//                    .build();
//            EmbeddingsVectorReply vectorReply = mockStub.createEmbeddingsVector(vectorRequest);
//            System.out.println("createEmbeddingsVector: " + vectorReply.getEmbeddingsList());
//
//            // Test createEmbeddingsVectors
//            EmbeddingsVectorsRequest vectorsRequest = EmbeddingsVectorsRequest.newBuilder()
//                    .addText("text one")
//                    .addText("text two")
//                    .build();
//            EmbeddingsVectorsReply vectorsReply = mockStub.createEmbeddingsVectors(vectorsRequest);
//            System.out.println("createEmbeddingsVectors: " + vectorsReply.getEmbeddingsList());
//
//            // Test check (health check)
//            HealthCheckRequest healthRequest = HealthCheckRequest.newBuilder().build();
//            HealthCheckReply healthReply = mockStub.check(healthRequest);
//            System.out.println("check: " + healthReply.getStatus());
//    }
//}
