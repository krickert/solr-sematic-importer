package com.krickert.search.indexer.controller;

import com.krickert.search.indexer.SemanticIndexer;
import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.dto.IndexingStatus;
import com.krickert.search.indexer.service.HealthService;
import com.krickert.search.indexer.service.IndexerService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Inject;

import java.util.*;

@Controller("/indexer")
public class IndexerController {

    private final IndexerService indexerService;
    private final HealthService healthService;
    private final SemanticIndexer semanticIndexer;

    @Inject
    public IndexerController(IndexerService indexerService, HealthService healthService, SemanticIndexer semanticIndexer) {
        this.indexerService = indexerService;
        this.healthService = healthService;
        this.semanticIndexer = semanticIndexer;
    }

    @Post("/start")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<String> startIndexing(IndexerConfiguration config) {
        String result = indexerService.startIndexing(config);
        if (result.startsWith("Indexing job started")) {
            return HttpResponse.ok(result);
        } else {
            return HttpResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
        }
    }

    @Get("/start")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<String> startIndexing(@QueryValue Optional<String> configName) {
        if (configName.isPresent() && !configName.get().isEmpty()) {
            String config = configName.get();
            if ("default".equals(config)) {
                semanticIndexer.runDefaultExportJob();
                return HttpResponse.ok("Indexing job started");
            }
            String result = indexerService.startIndexingByName(config);
            return createResponseBasedOnResult(result);
        }

        semanticIndexer.runDefaultExportJob();
        return HttpResponse.ok("Indexing job started");
    }

    private HttpResponse<String> createResponseBasedOnResult(String result) {
        if (result.startsWith("Indexing job started")) {
            return HttpResponse.ok(result);
        } else {
            return HttpResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
        }
    }

    @Post("/registerConfig")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<String> registerConfig(IndexerConfiguration config) {
        try {
            indexerService.registerConfiguration(config);
            return HttpResponse.ok("Configuration registered: " + config.getName());
        } catch (Exception e) {
            return HttpResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
        }
    }

    @Get("/status")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<IndexingStatus> getStatus() {
        try {
            IndexingStatus status = indexerService.getCurrentStatus();
            return HttpResponse.ok(status);
        } catch (Exception e) {
            IndexingStatus errorStatus = new IndexingStatus();
            errorStatus.setCurrentStatus("Error retrieving status: " + e.getMessage());
            errorStatus.setOverallStatus(IndexingStatus.OverallStatus.NOT_STARTED);
            return HttpResponse.serverError(errorStatus);
        }
    }

    @Get("/history")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<List<IndexingStatus>> getHistory(@QueryValue Optional<Integer> limit) {
        List<IndexingStatus> history;
        try {
            history = indexerService.getHistory(limit.orElse(10));
            if (history.isEmpty()) {
                IndexingStatus noHistoryStatus = new IndexingStatus("No history available");
                noHistoryStatus.setOverallStatus(IndexingStatus.OverallStatus.NONE_AVAILABLE);
                return HttpResponse.ok(Collections.singletonList(noHistoryStatus));
            }
            return HttpResponse.ok(history);
        } catch (Exception e) {
            IndexingStatus errorStatus = new IndexingStatus();
            errorStatus.setCurrentStatus("Error retrieving history: " + e.getMessage());
            return HttpResponse.serverError(Collections.singletonList(errorStatus));
        }
    }

    @Get("/health")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<Map<String, String>> checkHealth() {
        Map<String, String> healthStatus = new HashMap<>();
        try {
            boolean isVectorizerHealthy = healthService.checkVectorizerHealth();
            boolean isChunkerHealthy = healthService.checkChunkerHealth();
            healthStatus.put("vectorizer", isVectorizerHealthy ? "available" : "unavailable");
            healthStatus.put("chunker", isChunkerHealthy ? "available" : "unavailable");
            return HttpResponse.ok(healthStatus);
        } catch (Exception e) {
            healthStatus.put("status", "Error checking health: " + e.getMessage());
            return HttpResponse.serverError(healthStatus);
        }
    }
}