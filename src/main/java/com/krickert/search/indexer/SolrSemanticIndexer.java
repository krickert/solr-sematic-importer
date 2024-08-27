package com.krickert.search.indexer;

import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.dto.SolrDocumentType;
import com.krickert.search.indexer.solr.JsonToSolrDocParser;
import com.krickert.search.indexer.solr.client.SolrAdminActions;
import com.krickert.search.indexer.solr.httpclient.select.HttpSolrSelectClient;
import com.krickert.search.indexer.solr.httpclient.select.HttpSolrSelectResponse;
import com.krickert.search.indexer.solr.index.SolrInputDocumentQueue;
import com.krickert.search.indexer.solr.vector.SolrDestinationCollectionValidationService;
import com.krickert.search.indexer.solr.vector.SolrVectorIndexingService;
import com.krickert.search.indexer.tracker.IndexingTracker;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class SolrSemanticIndexer implements SemanticIndexer {

    private static final Logger log = LoggerFactory.getLogger(SolrSemanticIndexer.class);

    private final HttpSolrSelectClient httpSolrSelectClient;
    private final JsonToSolrDocParser jsonToSolrDoc;
    private final IndexerConfiguration defaultIndexerConfiguration;
    private final SolrDestinationCollectionValidationService solrDestinationCollectionValidationService;
    private final SolrVectorIndexingService solrIndexingService;
    private final SolrAdminActions solrAdminActions;
    private final SolrInputDocumentQueue solrInputDocumentQueue;
    private final IndexingTracker indexingTracker;

    @Inject
    public SolrSemanticIndexer(HttpSolrSelectClient httpSolrSelectClient,
                               JsonToSolrDocParser jsonToSolrDoc,
                               IndexerConfiguration defaultIndexerConfiguration,
                               SolrDestinationCollectionValidationService solrDestinationCollectionValidationService,
                               SolrVectorIndexingService solrIndexingService,
                               SolrAdminActions solrAdminActions,
                               SolrInputDocumentQueue solrInputDocumentQueue,
                               IndexingTracker indexingTracker) {
        log.info("creating SemanticIndexer");
        this.httpSolrSelectClient = checkNotNull(httpSolrSelectClient);
        this.jsonToSolrDoc = checkNotNull(jsonToSolrDoc);
        this.defaultIndexerConfiguration = checkNotNull(defaultIndexerConfiguration);
        this.solrDestinationCollectionValidationService = checkNotNull(solrDestinationCollectionValidationService);
        this.solrIndexingService = checkNotNull(solrIndexingService);
        this.solrAdminActions = checkNotNull(solrAdminActions);
        this.solrInputDocumentQueue = checkNotNull(solrInputDocumentQueue);
        this.indexingTracker = checkNotNull(indexingTracker);
        log.info("finished creating SemanticIndexer");
    }

    @Override
    public void runDefaultExportJob() {
        runExportJob(defaultIndexerConfiguration);
    }

    @Override
    public void runExportJob(IndexerConfiguration indexerConfiguration) {
        String solr7Host = indexerConfiguration.getSourceSolrConfiguration().getConnection().getUrl();
        String solrSourceCollection = indexerConfiguration.getSourceSolrConfiguration().getCollection();
        String solrDestinationCollection = indexerConfiguration.getDestinationSolrConfiguration().getCollection();
        final int paginationSize = indexerConfiguration.getSourceSolrConfiguration().getConnection().getPaginationSize() == null ? 100 : indexerConfiguration.getSourceSolrConfiguration().getConnection().getPaginationSize();

        // Validate the destination collection
        AtomicInteger pagesProcessed = new AtomicInteger(0);
        solrDestinationCollectionValidationService.validate();

        // Create the crawler ID. This will be saved in the collection and documents that are not matching this crawler ID will be deleted
        UUID crawlId = UUID.randomUUID();

        long totalExpected = httpSolrSelectClient.getTotalNumberOfDocumentsForCollection(solr7Host, solrSourceCollection);
        assert totalExpected >= 0;
        indexingTracker.setTotalDocumentsFound(totalExpected);
        long numOfPagesExpected = calculateNumOfPages(totalExpected, paginationSize);
        assert numOfPagesExpected >= 0;
        List<CompletableFuture<Void>> futures = IntStream.range(0, (int) numOfPagesExpected)
                .mapToObj(currentPage -> CompletableFuture.runAsync(
                        () -> processPages(solr7Host, solrSourceCollection, solrDestinationCollection, paginationSize, currentPage, crawlId, pagesProcessed))
                )
                .toList();

        // Wait for all pages to be processed
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        try {
            allOf.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        log.info("*****PROCESSING COMPLETE. {} pages processed. {} documents exported to destination collection {}", pagesProcessed.get(), totalExpected, solrDestinationCollection);
        solrAdminActions.commit(solrDestinationCollection);
        indexingTracker.finalizeTracking();
        // TODO delete documents that do not have the crawler ID
    }

    public void processPages(String solr7Host, String solrSourceCollection, String solrDestinationCollection, Integer paginationSize, int currentPage, UUID crawlId, AtomicInteger pagesProcessed) {
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
        processDocuments(documents, solrDestinationCollection, crawlId);
        pagesProcessed.incrementAndGet();
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

    private void processDocuments(Collection<SolrInputDocument> documents, String solrDestinationCollection, UUID crawlId) {
        documents.parallelStream().forEach(doc -> {
            insertCreationDate(doc);
            try {
                solrIndexingService.addVectorFieldsToSolr(doc, crawlId.toString(), Date.from(Instant.now()));
                solrInputDocumentQueue.addDocument(solrDestinationCollection, doc, SolrDocumentType.DOCUMENT);
            } catch (Exception e) {
                indexingTracker.documentFailed();
                log.error("Failed to process document with ID: {}", doc.getFieldValue("id"), e);
            }
        });
        documents.forEach(doc -> indexingTracker.documentProcessed());
    }

    private static void insertCreationDate(SolrInputDocument doc) {
        if (doc.containsKey("creation_date")) {
            // Retrieve the creation_date field
            try {
                Long creationDate = (Long) doc.getFieldValue("creation_date");
                // Update the Solr document with the converted date strings
                doc.setField("creation_date", convertToSolrDateString(creationDate));
            } catch (Exception e) {
                log.warn("creation_date exists but was not a Long value {}", e.getMessage());
                try {
                    Date creationDate = (Date) doc.getFieldValue("creation_date");
                    doc.setField("creation_date", convertToSolrDateString(creationDate.getTime()));
                } catch (Exception e2) {
                    log.warn("creation_date exists but was not a Date value. Giving up on conversion. Value {} with message {}", doc.getFieldValue("creation_date"), e.getMessage());
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