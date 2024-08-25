package com.krickert.search.indexer;

import io.micronaut.configuration.picocli.PicocliRunner;

import io.micronaut.runtime.Micronaut;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;

@Command(name = "solr-semantic-importer", description = "...",
        mixinStandardHelpOptions = true)
public class SolrSemanticImporterCommand {

    @Option(names = {"-v", "--verbose"}, description = "...")
    boolean verbose;

    @Option(names = {"-e", "--enabled"}, description = "skip the indexing and exit")
    boolean enabled = false;

    @Inject
    SemanticIndexer semanticIndexer;


    public static void main(String[] args) {
        Micronaut.run(SolrSemanticImporterCommand.class, args);
    }
}
