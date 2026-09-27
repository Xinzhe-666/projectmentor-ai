package com.xinzhe.projectmentor.analysis.service;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.UUID;

@Component
public class AnalysisInstanceIdentity {

    private final String instanceId = resolveHost() + "-" + UUID.randomUUID();

    public String instanceId() {
        return instanceId;
    }

    public String newWorkerId() {
        return instanceId + ":" + UUID.randomUUID();
    }

    private static String resolveHost() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            return "pmai";
        }
    }
}
