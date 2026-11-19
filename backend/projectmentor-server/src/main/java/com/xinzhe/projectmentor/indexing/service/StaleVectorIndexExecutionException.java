package com.xinzhe.projectmentor.indexing.service;

public class StaleVectorIndexExecutionException extends RuntimeException {
    public StaleVectorIndexExecutionException() {
        super("Vector index execution lost its lease or fencing token");
    }
}
