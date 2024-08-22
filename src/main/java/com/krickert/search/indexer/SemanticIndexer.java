package com.krickert.search.indexer;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.solr.JsonToSolrDocParser;
import com.krickert.search.indexer.solr.client.SolrAdminActions;
import com.krickert.search.indexer.solr.httpclient.select.HttpSolrSelectClient;
import com.krickert.search.indexer.solr.httpclient.select.HttpSolrSelectResponse;
import com.krickert.search.indexer.solr.vector.SolrDestinationCollectionValidationService;
import com.krickert.search.indexer.solr.vector.SolrVectorIndexingService;
import com.krickert.search.model.pipe.PipeDocument;
import io.micronaut.core.io.ResourceLoader;
import jakarta.inject.Inject;
import jakarta.inject.Named;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Singleton
public class SemanticIndexer {

    private static final Logger log = LoggerFactory.getLogger(SemanticIndexer.class);

    private final HttpSolrSelectClient httpSolrSelectClient;
    private final JsonToSolrDocParser jsonToSolrDoc;
    private final IndexerConfiguration indexerConfiguration;
    private final SolrDestinationCollectionValidationService solrDestinationCollectionValidationService;
    private final SolrVectorIndexingService solrIndexingService;
    private final SolrAdminActions solrAdminActions;
    ResourceLoader resourceLoader;


    @Inject
    public SemanticIndexer(HttpSolrSelectClient httpSolrSelectClient,
                           JsonToSolrDocParser jsonToSolrDoc,
                           IndexerConfiguration indexerConfiguration,
                           SolrDestinationCollectionValidationService solrDestinationCollectionValidationService,
                           SolrVectorIndexingService createVectorCollectionService,
                           ResourceLoader resourceLoader,
                           SolrAdminActions solrAdminActions) {
        this.httpSolrSelectClient = httpSolrSelectClient;
        this.jsonToSolrDoc = jsonToSolrDoc;
        this.indexerConfiguration = indexerConfiguration;
        this.solrDestinationCollectionValidationService = solrDestinationCollectionValidationService;
        this.solrIndexingService = createVectorCollectionService;
        this.resourceLoader = resourceLoader;
        this.solrAdminActions = solrAdminActions;
    }

    public List<Message> convertDescriptorsToMessages(Collection<PipeDocument> descriptors) {
        List<Message> messages = new ArrayList<>();

        for (PipeDocument descriptor : descriptors) {
            DynamicMessage message = DynamicMessage.newBuilder(descriptor).build();
            messages.add(message);
        }

        return messages;
    }

    public void exportSolrDocsFromExternalSolrCollection(Integer paginationSize) {
        exportSolrDocsFromExternalSolrCollection(
                indexerConfiguration.getSourceSolrConfiguration().getConnection().getUrl(),
                indexerConfiguration.getSourceSolrConfiguration().getCollection(),
                indexerConfiguration.getDestinationSolrConfiguration().getCollection(),
                paginationSize);
    }

    public void exportSolrDocsFromExternalSolrCollection(String solr7Host,
                                                         String solrSourceCollection,
                                                         String solrDestinationCollection,
                                                         Integer paginationSize) {
        // Validate the destination collection
        AtomicInteger pagesProcessed = new AtomicInteger(0);
        solrDestinationCollectionValidationService.validate();
        //create the crawler ID.  This will be saved in the collection and documents
        //that are not matching this crawler ID will be deleted
        UUID crawlId = UUID.randomUUID();

        if (paginationSize == null || paginationSize <= 0) {
            throw new IllegalArgumentException("paginationSize must be greater than 0");
        }

        long totalExpected = httpSolrSelectClient.getTotalNumberOfDocumentsForCollection(solr7Host, solrSourceCollection);
        assert totalExpected >= 0;
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
        log.info("*****PROCESSING COMPLETE.  {} pages processed.  {} documents exported to destination collection {}", pagesProcessed.get(), totalExpected, solrDestinationCollection);
        solrAdminActions.commit(solrDestinationCollection);
        //TODO delete document that do not have the crawler ID
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
        processDocuments(documents, crawlId);
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

    private void processDocuments(Collection<SolrInputDocument> documents, UUID crawlId) {
        documents.parallelStream().forEach(doc -> {
            insertCreationDate(doc);
            solrIndexingService.addVectorFieldsToSolr(doc, crawlId.toString(), Date.from(Instant.now()));
        });
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
                } catch (Exception e2) {
                    log.warn("creation_date exists but was not a Date value.  Giving up on conversion.  " +
                            "Value {} with message {}", doc.getFieldValue("creation_date"), e.getMessage());
                }
            }
        }
;
    }

    private static final DateTimeFormatter solrDateFormat = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    private static String convertToSolrDateString(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        return solrDateFormat.format(instant);
    }


}
