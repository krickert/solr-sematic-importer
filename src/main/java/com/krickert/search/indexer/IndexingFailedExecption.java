package com.krickert.search.indexer;

public class IndexingFailedExecption extends Exception {
    private static final long serialVersionUID = 1L;
    public IndexingFailedExecption(String message) {
        super(message);
    }
    public IndexingFailedExecption(String message, Throwable cause) {
        super(message, cause);
    }
}
