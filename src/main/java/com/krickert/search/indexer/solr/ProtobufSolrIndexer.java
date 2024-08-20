package com.krickert.search.indexer.solr;

import com.google.protobuf.Message;
import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.enhancers.ProtobufToSolrDocument;
import jakarta.inject.Singleton;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class ProtobufSolrIndexer {
    private static final Logger log = LoggerFactory.getLogger(ProtobufSolrIndexer.class);

    private final ProtobufToSolrDocument protobufToSolrDocument;
    private final IndexerConfiguration indexerConfiguration;

    public ProtobufSolrIndexer(ProtobufToSolrDocument protobufToSolrDocument,
                               IndexerConfiguration indexerConfiguration) {
        this.protobufToSolrDocument = protobufToSolrDocument;
        this.indexerConfiguration = indexerConfiguration;
    }

    public void exportProtobufToSolr(Collection<Message> protos) {
        List<SolrInputDocument> solrDocuments = protos.stream().map(protobufToSolrDocument::convertProtobufToSolrDocument).collect(Collectors.toList());

        try (SolrClient solrClient = createSolr9Client()) {
            String collection = indexerConfiguration.getDestinationSolrConfiguration().getCollection();
            try {
                solrClient.add(collection, solrDocuments);
                solrClient.commit(collection);
            } catch (SolrServerException | IOException e) {
                log.error("Commit solr failed for collection {}", collection, e);
            }
        } catch (IOException e) {
            log.error("Couldn't insert {}", protos, e);
        }
    }

    protected SolrClient createSolr9Client() {
        String baseUrl = indexerConfiguration.getDestinationSolrConfiguration().getConnection().getUrl();
        log.info("Base Solr URL: {}", baseUrl);
        return new Http2SolrClient.Builder(baseUrl).withDefaultCollection(indexerConfiguration.getDestinationSolrConfiguration().getCollection()).build();
    }

}
