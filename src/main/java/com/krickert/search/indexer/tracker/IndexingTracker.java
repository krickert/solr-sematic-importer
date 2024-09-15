package com.krickert.search.indexer.tracker;

import com.krickert.search.indexer.dto.IndexingStatus;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Singleton
public class IndexingTracker {

    private final IndexingStatus indexingStatus = new IndexingStatus();
    private final List<IndexingStatus> indexingHistory = new LinkedList<>();

    private final AtomicLong totalDocumentsFound = new AtomicLong(0);
    private final AtomicInteger totalDocumentsProcessed = new AtomicInteger(0);
    private final AtomicInteger totalDocumentsFailed = new AtomicInteger(0);
    private LocalDateTime timeStarted;
    private final Integer maxHistorySize;

    @Inject
    public IndexingTracker(@Value("${indexer-manager.max-history-size}") Integer maxHistorySize) {
        this.maxHistorySize = maxHistorySize == null || maxHistorySize < 0 ? 100 : maxHistorySize;
    }

    public IndexingStatus getCurrentStatus() {
        return indexingStatus;
    }

    public void setTotalDocumentsFound(Long totalDocuments) {
        totalDocumentsFound.set(totalDocuments);
        updateProgress();
    }

    public void reset() {
        indexingStatus.setIndexingId(null);
        indexingStatus.setIndexerConfiguration(null);
        indexingStatus.setTotalDocumentsFound(0);
        indexingStatus.setTotalDocumentsProcessed(0);
        indexingStatus.setTotalDocumentsFailed(0);
        indexingStatus.setPercentComplete(0);
        indexingStatus.setTimeStarted(null);
        indexingStatus.setEndTime(null);
        indexingStatus.setCurrentStatusMessage(null);
        indexingStatus.setAverageDocsPerSecond(0);

        totalDocumentsFound.set(0);
        totalDocumentsProcessed.set(0);
        totalDocumentsFailed.set(0);

        timeStarted = null;
    }

    public Integer getTotalDocumentsFound() {
        return totalDocumentsFound.intValue();
    }

    public Integer getTotalDocumentsProcessed() {
        return totalDocumentsProcessed.intValue();
    }

    public Integer getTotalDocumentsFailed() {
        return totalDocumentsFailed.intValue();
    }

    public void startTracking(String indexingId) {
        timeStarted = LocalDateTime.now();
        indexingStatus.setIndexingId(indexingId);
        indexingStatus.setTimeStarted(timeStarted);
        indexingStatus.setCurrentStatusMessage("started");
        indexingStatus.setOverallStatus(IndexingStatus.OverallStatus.RUNNING);
    }

    public void updateProgress() {
        if (indexingStatus.getOverallStatus() == IndexingStatus.OverallStatus.NOT_STARTED) {
            return;
        }
        indexingStatus.setTotalDocumentsFound(totalDocumentsFound.get());
        indexingStatus.setTotalDocumentsProcessed(totalDocumentsProcessed.get());
        indexingStatus.setTotalDocumentsFailed(totalDocumentsFailed.get());

        // Update percent complete
        long totalFound = totalDocumentsFound.get();
        int totalProcessed = totalDocumentsProcessed.get();
        indexingStatus.setPercentComplete(totalFound > 0 ? ((float) totalProcessed / totalFound) * 100 : 0);

        // Update average documents per second
        LocalDateTime endTime = indexingStatus.getEndTime() != null ? indexingStatus.getEndTime() : LocalDateTime.now();
        long durationInSeconds = Duration.between(indexingStatus.getTimeStarted(), endTime).getSeconds();
        float avgDocsPerSecond = durationInSeconds > 0 ? (float) totalDocumentsProcessed.get() / durationInSeconds : 0;
        indexingStatus.setAverageDocsPerSecond(avgDocsPerSecond);
    }

    public void finalizeTracking() {
        indexingStatus.setEndTime(LocalDateTime.now());
        indexingStatus.setCurrentStatusMessage("completed");
        indexingStatus.setOverallStatus(IndexingStatus.OverallStatus.COMPLETED);
        calculateFinalStatistics();
        recordHistory(indexingStatus);
    }

    private void calculateFinalStatistics() {
        if (indexingStatus.getEndTime() != null && indexingStatus.getTimeStarted() != null) {
            long durationInSeconds = Duration.between(indexingStatus.getTimeStarted(), indexingStatus.getEndTime()).getSeconds();
            if (durationInSeconds > 0) {
                indexingStatus.setAverageDocsPerSecond((float) totalDocumentsProcessed.get() / durationInSeconds);
            }
        }
    }

    private void recordHistory(IndexingStatus status) {
        if (indexingHistory.size() >= maxHistorySize) {
            indexingHistory.remove(0);
        }
        indexingHistory.add(status.clone());
    }

    public List<IndexingStatus> getHistory(int limit) {
        return indexingHistory.stream().limit(limit).collect(Collectors.toList());
    }

    // Methods for tracking document processing
    public void documentProcessed() {
        totalDocumentsProcessed.incrementAndGet();
        updateProgress();
        checkIfFinished();
    }

    public void documentFailed() {
        totalDocumentsFailed.incrementAndGet();
        updateProgress();
        checkIfFinished();
    }

    private void checkIfFinished() {
        if (totalDocumentsProcessed.get() + totalDocumentsFailed.get() == totalDocumentsFound.get()) {
            finalizeTracking();
        }
    }

    public void markIndexingAsFailed() {
        indexingStatus.setOverallStatus(IndexingStatus.OverallStatus.FAILED);
        indexingStatus.setCurrentStatusMessage("failed");
    }

    // Helper function to get percent complete
    public float getPercentComplete() {
        return indexingStatus.getPercentComplete();
    }

    // Helper function to get average documents processed per second.
    public float getAverageDocsPerSecond() {
        LocalDateTime endTime = indexingStatus.getEndTime() != null ? indexingStatus.getEndTime() : LocalDateTime.now();
        long durationInSeconds = Duration.between(indexingStatus.getTimeStarted(), endTime).getSeconds();
        return durationInSeconds > 0 ? (float) totalDocumentsProcessed.get() / durationInSeconds : 0;
    }
}