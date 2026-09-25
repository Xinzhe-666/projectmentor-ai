package com.xinzhe.projectmentor.analysis.service;

public record RabbitPublishResult(Status status, String reason) {
    public enum Status {
        ACK,
        NACK,
        RETURNED,
        TIMEOUT,
        ERROR
    }

    public boolean reliable() {
        return status == Status.ACK;
    }
}
