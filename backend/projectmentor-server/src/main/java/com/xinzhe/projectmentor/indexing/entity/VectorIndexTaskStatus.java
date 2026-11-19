package com.xinzhe.projectmentor.indexing.entity;

import java.util.Set;

/** Shared delivery-time classification; active task claiming remains database-fenced. */
public final class VectorIndexTaskStatus {
    private static final Set<String> TERMINAL = Set.of("SUCCEEDED", "FAILED", "CANCELLED", "SUPERSEDED");

    private VectorIndexTaskStatus() { }

    public static boolean isTerminal(String status) {
        return status != null && TERMINAL.contains(status);
    }
}
