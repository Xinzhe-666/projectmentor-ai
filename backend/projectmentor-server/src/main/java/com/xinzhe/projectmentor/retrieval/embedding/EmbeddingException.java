package com.xinzhe.projectmentor.retrieval.embedding;

public class EmbeddingException extends RuntimeException {
    private final String code;
    private final boolean retryable;

    public EmbeddingException(String code, boolean retryable, String message) {
        super(message);
        this.code = code;
        this.retryable = retryable;
    }

    public EmbeddingException(String code, boolean retryable, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.retryable = retryable;
    }

    public String code() { return code; }
    public boolean retryable() { return retryable; }
}
