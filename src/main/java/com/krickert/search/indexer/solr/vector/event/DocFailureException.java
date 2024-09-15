package com.krickert.search.indexer.solr.vector.event;

public class DocFailureException extends RuntimeException {
    public DocFailureException(String message) {
        super(message);
    }
    public DocFailureException(String message, Throwable cause) {super(message, cause);}
}
