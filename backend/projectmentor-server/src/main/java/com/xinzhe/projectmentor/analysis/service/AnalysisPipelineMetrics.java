package com.xinzhe.projectmentor.analysis.service;

import com.xinzhe.projectmentor.analysis.mapper.AnalysisOutboxMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Component
public class AnalysisPipelineMetrics {

    private final MeterRegistry registry;
    private final AtomicInteger activeWorkers = new AtomicInteger();
    private final Timer executionDuration;

    public AnalysisPipelineMetrics(MeterRegistry registry, AnalysisOutboxMapper outboxMapper) {
        this.registry = registry;
        this.executionDuration = Timer.builder("pmai.analysis.execution.duration").register(registry);
        Gauge.builder("pmai.analysis.workers.active", activeWorkers, AtomicInteger::get).register(registry);
        Gauge.builder("pmai.analysis.outbox.backlog", outboxMapper, this::safeBacklog).register(registry);
    }

    public void taskSubmitted(String mode) {
        counter("pmai.analysis.task.submitted", "mode", mode).increment();
    }

    public void outbox(String result) {
        counter("pmai.analysis.outbox.events", "result", result).increment();
    }

    public void message(String result) {
        counter("pmai.analysis.messages", "result", result).increment();
    }

    public void lease(String result) {
        counter("pmai.analysis.leases", "result", result).increment();
    }

    public Timer.Sample startExecution() {
        activeWorkers.incrementAndGet();
        return Timer.start(registry);
    }

    public void stopExecution(Timer.Sample sample) {
        try {
            sample.stop(executionDuration);
        } finally {
            activeWorkers.decrementAndGet();
        }
    }

    private Counter counter(String name, String tagName, String tagValue) {
        return Counter.builder(name).tag(tagName, tagValue).register(registry);
    }

    private double safeBacklog(AnalysisOutboxMapper mapper) {
        try {
            return mapper.countBacklog();
        } catch (Exception ignored) {
            return Double.NaN;
        }
    }
}
