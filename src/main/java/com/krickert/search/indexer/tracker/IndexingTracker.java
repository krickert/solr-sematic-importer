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
    private final AtomicInteger chunksProcessed = new AtomicInteger(0);
    private LocalDateTime timeStarted;
    private final Integer maxHistorySize;

    @Inject
    public IndexingTracker(@Value("${indexer-manager.max-history-size}") Integer maxHistorySize) {
        this.maxHistorySize = maxHistorySize == null || maxHistorySize < 0 ? 100 : maxHistorySize;
    }

    public IndexingStatus getCurrentStatus() {
        return indexingStatus;
    }

    public void setTotalDocumentsFound(Long toalDocuments) {
        totalDocumentsFound.set(toalDocuments);
    }

    public void reset() {
        indexingStatus.setIndexingId(null);
        indexingStatus.setIndexerConfiguration(null);
        indexingStatus.setTotalDocumentsFound(0);
        indexingStatus.setTotalDocumentsProcessed(0);
        indexingStatus.setTotalDocumentsFailed(0);
        indexingStatus.setPercentComplete(0);
        indexingStatus.setChunksProcessed(0);
        indexingStatus.setChunksPerDocument(0);
        indexingStatus.setTimeStarted(null);
        indexingStatus.setEndTime(null);
        indexingStatus.setCurrentStatus(null);
        indexingStatus.setAverageDocsPerSecond(0);
        indexingStatus.setAverageChunksPerSecond(0);

        totalDocumentsFound.set(0);
        totalDocumentsProcessed.set(0);
        totalDocumentsFailed.set(0);
        chunksProcessed.set(0);

        timeStarted = null;
    }

    public void startTracking(String indexingId) {
        timeStarted = LocalDateTime.now();
        indexingStatus.setIndexingId(indexingId);
        indexingStatus.setTimeStarted(timeStarted);
        indexingStatus.setCurrentStatus("started");
    }

    public void updateProgress() {
        indexingStatus.setTotalDocumentsFound(totalDocumentsFound.get());
        indexingStatus.setTotalDocumentsProcessed(totalDocumentsProcessed.get());
        indexingStatus.setTotalDocumentsFailed(totalDocumentsFailed.get());
        indexingStatus.setChunksProcessed(chunksProcessed.get());

        long totalFound = totalDocumentsFound.get();
        int totalProcessed = totalDocumentsProcessed.get();
        indexingStatus.setPercentComplete(totalFound > 0 ? ((float) totalProcessed / totalFound) * 100 : 0);
        indexingStatus.setChunksPerDocument(totalProcessed > 0 ? (float) chunksProcessed.get() / totalProcessed : 0);
    }

    public void finalizeTracking() {
        indexingStatus.setEndTime(LocalDateTime.now());
        calculateFinalStatistics();
        recordHistory(indexingStatus);
    }

    private void calculateFinalStatistics() {
        if (indexingStatus.getEndTime() != null && indexingStatus.getTimeStarted() != null) {
            long durationInSeconds = Duration.between(indexingStatus.getTimeStarted(), indexingStatus.getEndTime()).getSeconds();
            if (durationInSeconds > 0) {
                indexingStatus.setAverageDocsPerSecond((float) totalDocumentsProcessed.get() / durationInSeconds);
                indexingStatus.setAverageChunksPerSecond((float) chunksProcessed.get() / durationInSeconds);
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
    }

    public void documentFailed() {
        totalDocumentsFailed.incrementAndGet();
    }

    public void chunkProcessed() {
        chunksProcessed.incrementAndGet();
    }
}