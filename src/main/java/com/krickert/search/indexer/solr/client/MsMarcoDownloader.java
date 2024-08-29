package com.krickert.search.indexer.solr.client;


import io.micronaut.context.annotation.Value;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Singleton;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.solr.common.SolrInputDocument;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPInputStream;

@Singleton
public class MsMarcoDownloader {

    @Value("${tgz.url}")
    private String tgzUrl;

    @Value("${tgz.proxy.host}")
    private String proxyHost;

    @Value("${tgz.proxy.port}")
    private int proxyPort;

    @Value("${tgz.proxy.username:#{null}}")
    private String proxyUsername;

    @Value("${tgz.proxy.password:#{null}}")
    private String proxyPassword;


    private BlockingQueue<SolrInputDocument> solrQueue = new LinkedBlockingQueue<>(10_000_000);

    @Async
    public void downloadAndProcessTgz() {
        try {
            downloadTgz();
            processTgz();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadTgz() throws IOException {
        try (InputStream in = new URL(tgzUrl).openStream()) {
            Files.copy(in, Paths.get("data.tgz"));
        }
    }

    private void processTgz() throws IOException {
        try (FileInputStream fis = new FileInputStream("data.tgz");
             GzipCompressorInputStream gzis = new GzipCompressorInputStream(fis);
             TarArchiveInputStream tis = new TarArchiveInputStream(gzis)) {

            TarArchiveEntry entry;
            while ((entry = tis.getNextTarEntry()) != null) {
                if (entry.isFile() && entry.getName().endsWith(".tsv")) {
                    processTsv(tis);
                }
            }
        }
    }

    private void processTsv(InputStream tsvInputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(tsvInputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split("\t");

                if (columns.length == 4) {
                    SolrInputDocument doc = new SolrInputDocument();
                    doc.addField("docid", columns[0]);
                    doc.addField("url", columns[1]);
                    doc.addField("title", columns[2]);
                    doc.addField("body", columns[3]);

                    solrQueue.put(doc);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Thread interrupted while processing TSV", e);
        }
    }

    public BlockingQueue<SolrInputDocument> getSolrQueue() {
        return solrQueue;
    }
}
