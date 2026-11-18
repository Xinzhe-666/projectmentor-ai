package com.xinzhe.projectmentor.indexing.service;

import com.xinzhe.projectmentor.indexing.mapper.VectorIndexOutboxMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexTaskMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class VectorIndexOperationalMetrics {
    public VectorIndexOperationalMetrics(MeterRegistry registry, VectorIndexOutboxMapper outbox,
                                         VectorIndexTaskMapper tasks) {
        Gauge.builder("pmai.index.outbox.backlog", outbox, mapper -> safe(mapper::countBacklog))
                .description("Durable vector outbox rows awaiting publication or local processing")
                .register(registry);
        Gauge.builder("pmai.index.active.tasks", tasks, mapper -> safe(mapper::countActiveTasks))
                .description("Currently active vector index and cleanup tasks")
                .register(registry);
        Gauge.builder("pmai.index.duplicate.active.projects", tasks,
                        mapper -> safe(mapper::countDuplicateActiveProjects))
                .description("Projects with more than one active vector task")
                .register(registry);
    }

    private static double safe(LongSupplier query) {
        try { return query.getAsLong(); }
        catch (RuntimeException unavailable) { return Double.NaN; }
    }

    @FunctionalInterface
    private interface LongSupplier { long getAsLong(); }
}
