package com.krickert.search.indexer;

import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.dto.IndexingStatus;
import com.krickert.search.indexer.solr.SchemaConstants;
import com.krickert.search.indexer.solr.client.SolrClientService;
import com.krickert.search.indexer.solr.vector.event.SolrSourceDocumentPublisher;
import com.krickert.search.indexer.solr.vector.event.SubscriptionManager;
import com.krickert.search.indexer.solr.JsonToSolrDocParser;
import com.krickert.search.indexer.solr.client.SolrAdminActions;
import com.krickert.search.indexer.solr.httpclient.select.HttpSolrSelectClient;
import com.krickert.search.indexer.solr.httpclient.select.HttpSolrSelectResponse;
import com.krickert.search.indexer.solr.vector.SolrDestinationCollectionValidationService;
import com.krickert.search.indexer.tracker.IndexingTracker;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.krickert.search.indexer.tracker.IndexingTracker.TaskType.MAIN;
import static com.krickert.search.indexer.tracker.IndexingTracker.TaskType.VECTOR;
import static java.lang.Thread.sleep;

@Singleton
public class SolrSemanticIndexer implements SemanticIndexer {

    private static final Logger log = LoggerFactory.getLogger(SolrSemanticIndexer.class);

    private final HttpSolrSelectClient httpSolrSelectClient;
    private final JsonToSolrDocParser jsonToSolrDoc;
    private final IndexerConfiguration defaultIndexerConfiguration;
    private final SolrDestinationCollectionValidationService solrDestinationCollectionValidationService;
    private final SolrAdminActions solrAdminActions;
    private final IndexingTracker indexingTracker;
    private final SolrSourceDocumentPublisher solrSourceDocumentPublisher;

    @Inject
    public SolrSemanticIndexer(HttpSolrSelectClient httpSolrSelectClient,
                               JsonToSolrDocParser jsonToSolrDoc,
                               IndexerConfiguration defaultIndexerConfiguration,
                               SolrClientService solrClientService,
                               SolrDestinationCollectionValidationService solrDestinationCollectionValidationService,
                               SolrAdminActions solrAdminActions,
                               IndexingTracker indexingTracker,
                               SolrSourceDocumentPublisher solrSourceDocumentPublisher,
                               SubscriptionManager subscriptionManager) {
        log.info("creating SemanticIndexer");
        checkNotNull(solrClientService);
        checkNotNull(subscriptionManager);
        this.httpSolrSelectClient = checkNotNull(httpSolrSelectClient);
        this.jsonToSolrDoc = checkNotNull(jsonToSolrDoc);
        this.defaultIndexerConfiguration = checkNotNull(defaultIndexerConfiguration);
        this.solrDestinationCollectionValidationService = checkNotNull(solrDestinationCollectionValidationService);
        this.solrAdminActions = checkNotNull(solrAdminActions);
        this.indexingTracker = checkNotNull(indexingTracker);
        log.info("finished creating SemanticIndexer");
        this.solrSourceDocumentPublisher = solrSourceDocumentPublisher;
    }

    @Override
    public void runDefaultExportJob() throws IndexingFailedExecption {
        IndexerConfiguration indexerConfiguration = defaultIndexerConfiguration;
        String solr7Host = indexerConfiguration.getSourceSolrConfiguration().getConnection().getUrl();
        String solrSourceCollection = indexerConfiguration.getSourceSolrConfiguration().getCollection();
        String solrDestinationCollection = indexerConfiguration.getDestinationSolrConfiguration().getCollection();
        final int paginationSize = indexerConfiguration.getSourceSolrConfiguration().getConnection().getPaginationSize() == null ? 100 : indexerConfiguration.getSourceSolrConfiguration().getConnection().getPaginationSize();

        // Validate the destination collection
        solrDestinationCollectionValidationService.validate();

        // Create the crawler ID. This will be saved in the collection and documents that are not matching this crawler ID will be deleted
        UUID crawlId = UUID.randomUUID();

        long totalExpected = httpSolrSelectClient.getTotalNumberOfDocumentsForCollection(solr7Host, solrSourceCollection);
        assert totalExpected >= 0;
        log.info("We queried host {} with collection {} and it returned {} documents. We will start tracking this crawl", solr7Host, solrSourceCollection, totalExpected);
        indexingTracker.reset();
        indexingTracker.startTracking(crawlId.toString());
        indexingTracker.setTotalDocumentsFound(totalExpected);
        long numOfPagesExpected = calculateNumOfPages(totalExpected, paginationSize);
        assert numOfPagesExpected >= 0;
        for (int currentPage = 0; currentPage < numOfPagesExpected; currentPage++) {
            processPages(solr7Host, solrSourceCollection, solrDestinationCollection, paginationSize, currentPage, crawlId);
        }
        log.info("*****PUBLISHING COMPLETE. {} documents were pushed and going to the {} collection", totalExpected, solrDestinationCollection);

        waitForIndexingCompletion(MAIN);
        solrAdminActions.commit(solrDestinationCollection);
        indexingTracker.finalizeTracking(IndexingTracker.TaskType.MAIN);
        waitForIndexingCompletion(VECTOR);
        indexingTracker.finalizeTracking(VECTOR);
        solrAdminActions.commitVectorCollections();
        if (indexingTracker.getMainTaskStatus().getOverallStatus() == IndexingStatus.OverallStatus.FAILED) {
            String errorMessage = String.format("Indexing job %s failed.  End status: \n%s", crawlId, indexingTracker.getMainTaskStatus());
            log.error(errorMessage);
            throw new IndexingFailedExecption(errorMessage);
        }
        //deleteOrphans(solrDestinationCollection, crawlId);
    }

    private void waitForIndexingCompletion(IndexingTracker.TaskType taskType) throws IndexingFailedExecption {
        int maxWarnings = 3;
        int warningCount = 0;
        long previousProcessedCount = 0;
        int waitTimeInSeconds = 10; // Time to wait between checks for new documents
        IndexingStatus taskStatus = getStatusByTaskType(taskType); // Getting the respective task status.
        long totalExpected = taskStatus.getTotalDocumentsFound();

        while (true) {
            // Get the current status
            long totalProcessed = taskStatus.getTotalDocumentsProcessed();
            long totalFailed = taskStatus.getTotalDocumentsFailed();
            long totalProcessedOrFailed = totalProcessed + totalFailed;

            // Check if the total processed or failed documents meets the expected total
            if (totalProcessedOrFailed >= totalExpected) {
                log.info("All documents processed for {} task. Marking indexing as complete.", taskType);
                indexingTracker.finalizeTracking(taskType);
                break;
            } else {
                log.info("***** INDEXING STILL IN PROGRESS for {} task: Expecting {} documents. {}", taskType, totalExpected, taskStatus);
            }

            // Wait for some time before checking again
            try {
                Thread.sleep(waitTimeInSeconds * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IndexingFailedExecption("Waiting for indexing completion was interrupted", e);
            }

            // Check if the count of processed documents has not increased
            if (totalProcessedOrFailed == previousProcessedCount) {
                warningCount++;
                log.warn("Potential hanging crawl detected for {} task. No progress in last {} seconds. Warning {}/{}", taskType, waitTimeInSeconds, warningCount, maxWarnings);

                // Exit the loop and log an error if max warnings are reached
                if (warningCount >= maxWarnings) {
                    log.error("Max warnings reached for {} task. Hanging crawl detected. Exiting wait loop.", taskType);
                    indexingTracker.markIndexingAsFailed(taskType);
                    break;
                }
            } else {
                // Reset warning count if progress is made
                warningCount = 0;
            }

            // Update the previousProcessedCount
            previousProcessedCount = totalProcessedOrFailed;
        }
    }

    private IndexingStatus getStatusByTaskType(IndexingTracker.TaskType taskType) {
        switch (taskType) {
            case MAIN:
                return indexingTracker.getMainTaskStatus();
            case VECTOR:
                return indexingTracker.getVectorTaskStatus();
            default:
                throw new IllegalArgumentException("Unknown task type: " + taskType);
        }
    }

    private void deleteOrphans(String solrDestinationCollection, UUID crawlId) {
        log.info("Deleting orphans from collection {}.  All documents without UUID {} will be deleted", solrDestinationCollection, crawlId);
        solrAdminActions.deleteOrphansAfterIndexing(solrDestinationCollection, crawlId.toString());
        Collection<String> vectorCollections = solrDestinationCollectionValidationService.getVectorDestinationCollections();
        log.info("Deleting orphans from vector collections {}.  All documents without UUID {} will be deleted", vectorCollections, crawlId);
        for (String vectorCollection : vectorCollections) {
            solrAdminActions.deleteOrphansAfterIndexing(vectorCollection, crawlId.toString());
        }
    }

    public void processPages(String solr7Host, String solrSourceCollection, String solrDestinationCollection, Integer paginationSize, int currentPage, UUID crawlId) {
        String solrDocs = fetchSolrDocuments(solr7Host, solrSourceCollection, paginationSize, currentPage);
        HttpSolrSelectResponse response = jsonToSolrDoc.parseSolrDocuments(solrDocs);

        if (isEmptyResponse(response)) {
            log.info("No solr documents in source collection. Breaking.");
            return;
        }
        Collection<SolrInputDocument> documents = response.getDocs();
        if (documents.isEmpty()) {
            return;
        }
        log.info("Exporting {} documents from source collection {} to destination collection {}", documents.size(), solrSourceCollection, solrDestinationCollection);
        processDocuments(documents, crawlId);
    }

    private long calculateNumOfPages(long totalDocuments, int paginationSize) {
        return (totalDocuments == -1) ? -1 : (totalDocuments / paginationSize) + 1;
    }

    private String fetchSolrDocuments(String solr7Host, String solrSourceCollection, int paginationSize, int currentPage) {
        return httpSolrSelectClient.getSolrDocs(solr7Host, solrSourceCollection, paginationSize, currentPage);
    }

    private boolean isEmptyResponse(HttpSolrSelectResponse response) {
        return response.getNumFound() == 0;
    }

    private void processDocuments(Collection<SolrInputDocument> documents, UUID crawlId) {
        documents.forEach(doc -> {
            insertDates(doc);
            insertCrawlId(doc, crawlId);
            solrSourceDocumentPublisher.publishDocument(doc);
        });
    }

    private static void insertCrawlId(SolrInputDocument doc, UUID crawlId) {
        doc.setField(SchemaConstants.CRAWL_ID, crawlId.toString());
    }

    private static void insertDates(SolrInputDocument doc) {
        checkDateField(doc, "creation_date");
        doc.addField(SchemaConstants.CRAWL_DATE, convertToSolrDateString(System.currentTimeMillis()));
    }

    private static void checkDateField(SolrInputDocument doc, String date) {
        if (doc.containsKey(date)) {
            // Retrieve the creation_date field
            try {
                Long creationDate = (Long) doc.getFieldValue(date);
                // Update the Solr document with the converted date strings
                doc.setField(date, convertToSolrDateString(creationDate));
            } catch (Exception e) {
                log.warn("{} exists but was not a Long value {}", date, e.getMessage());
                try {
                    Date creationDate = (Date) doc.getFieldValue(date);
                    doc.setField(date, convertToSolrDateString(creationDate.getTime()));
                } catch (Exception e2) {
                    log.warn("{} exists but was not a Date value. Giving up on conversion. Value {} with message {}",
                            date, doc.getFieldValue(date), e.getMessage());
                }
            }
        }
    }

    private static final DateTimeFormatter solrDateFormat = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    private static String convertToSolrDateString(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        return solrDateFormat.format(instant);
    }
}