package com.krickert.search.indexer.service;

import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.dto.IndexingStatus;
import com.krickert.search.indexer.SemanticIndexer;
import com.krickert.search.indexer.tracker.IndexingTracker;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Singleton
public class IndexerService {

    private final SemanticIndexer semanticIndexer;
    private final IndexingTracker indexingTracker;
    private final HealthService healthService;
    private final Map<String, IndexerConfiguration> configurations = new HashMap<>();
    private final Lock startIndexingLock = new ReentrantLock();

    @Inject
    public IndexerService(SemanticIndexer semanticIndexer,
                          IndexingTracker indexingTracker,
                          HealthService healthService) {
        this.semanticIndexer = semanticIndexer;
        this.indexingTracker = indexingTracker;
        this.healthService = healthService;
    }

    public String startIndexing(IndexerConfiguration config) {
        startIndexingLock.lock();
        try {
            if (!isIndexerReady()) {
                return "Indexer is not ready. Please check the status of vectorizer and chunker services.";
            }

            IndexingStatus currentStatus = indexingTracker.getCurrentStatus();
            if (IndexingStatus.OverallStatus.RUNNING.equals(currentStatus.getOverallStatus())) {
                return "Indexing job is already in progress.";
            }

            indexingTracker.reset();
            String indexingId = generateIndexingId();
            indexingTracker.startTracking(indexingId);
            IndexingStatus indexingStatus = indexingTracker.getCurrentStatus();
            indexingStatus.setIndexerConfiguration(config);
            indexingStatus.setOverallStatus(IndexingStatus.OverallStatus.RUNNING); // Set overall status to RUNNING

            new Thread(() -> {
                try {
                    semanticIndexer.runExportJob(config);
                    indexingStatus.setCurrentStatus("completed");
                    indexingStatus.setOverallStatus(IndexingStatus.OverallStatus.COMPLETED); // Set to COMPLETED
                } catch (Exception e) {
                    indexingStatus.setCurrentStatus("errored");
                    indexingStatus.setOverallStatus(IndexingStatus.OverallStatus.NOT_STARTED); // Set to NOT_STARTED on error
                } finally {
                    indexingStatus.setLastRun(LocalDateTime.now()); // Set the last run time
                    indexingTracker.finalizeTracking();
                }
            }).start();

            return "Indexing job started with ID: " + indexingId;
        } finally {
            startIndexingLock.unlock();
        }
    }

    public String startIndexingByName(String configName) {
        IndexerConfiguration config = configurations.get(configName);
        if (config == null) {
            return "Configuration not found: " + configName;
        }
        return startIndexing(config);
    }

    public boolean isIndexerReady() {
        return healthService.checkVectorizerHealth() && healthService.checkChunkerHealth() && !isIndexingInProgress();
    }

    public boolean isIndexingInProgress() {
        IndexingStatus currentStatus = getCurrentStatus();
        return IndexingStatus.OverallStatus.RUNNING.equals(currentStatus.getOverallStatus());
    }

    @Scheduled(fixedRate = "1s", initialDelay = "10s")
    public void updateProgress() {
        indexingTracker.updateProgress();
    }

    public IndexingStatus getCurrentStatus() {
        IndexingStatus status = indexingTracker.getCurrentStatus();
        if (status == null) { // If no status is available, set it as NONE_AVAILABLE
            status = new IndexingStatus();
            status.setOverallStatus(IndexingStatus.OverallStatus.NONE_AVAILABLE);
        }
        return status;
    }

    private String generateIndexingId() {
        return "indexingId-" + LocalDateTime.now(); // Simplified for example
    }

    public List<IndexingStatus> getHistory(int limit) {
        return indexingTracker.getHistory(limit);
    }

    public void registerConfiguration(IndexerConfiguration config) {
        configurations.put(config.getName(), config);
    }

    public void documentProcessed() {
        indexingTracker.documentProcessed();
    }

    public void documentFailed() {
        indexingTracker.documentFailed();
    }

    public void chunkProcessed() {
        indexingTracker.chunkProcessed();
    }
}