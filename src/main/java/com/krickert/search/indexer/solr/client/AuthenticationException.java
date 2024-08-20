package com.krickert.search.indexer.solr.client;

public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
    public AuthenticationException(Throwable cause) {
        super(cause);
    }
}
