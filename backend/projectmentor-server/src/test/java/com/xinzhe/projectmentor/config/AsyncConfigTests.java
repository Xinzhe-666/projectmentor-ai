package com.xinzhe.projectmentor.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTests {

    @Test
    void conservativeDefaultsAreKept() {
        AnalysisExecutorProperties properties = new AnalysisExecutorProperties();

        assertThat(properties.getCorePoolSize()).isEqualTo(2);
        assertThat(properties.getMaxPoolSize()).isEqualTo(4);
        assertThat(properties.getQueueCapacity()).isEqualTo(50);
        assertThat(properties.getKeepAliveSeconds()).isEqualTo(60);
        assertThat(properties.getAwaitTerminationSeconds()).isEqualTo(30);
    }

    @Test
    void executorUsesExternalPropertiesBoundedQueueAndAbortPolicy() {
        AnalysisExecutorProperties properties = new AnalysisExecutorProperties();
        properties.setCorePoolSize(1);
        properties.setMaxPoolSize(3);
        properties.setQueueCapacity(7);
        properties.setKeepAliveSeconds(45);

        Executor configured = new AsyncConfig().analysisTaskExecutor(properties);
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) configured;
        try {
            ThreadPoolExecutor threadPool = executor.getThreadPoolExecutor();
            assertThat(threadPool.getCorePoolSize()).isEqualTo(1);
            assertThat(threadPool.getMaximumPoolSize()).isEqualTo(3);
            assertThat(threadPool.getKeepAliveTime(java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(45);
            assertThat(threadPool.getQueue().remainingCapacity()).isEqualTo(7);
            assertThat(threadPool.getRejectedExecutionHandler())
                    .isInstanceOf(ThreadPoolExecutor.AbortPolicy.class);
        } finally {
            executor.shutdown();
        }
    }
}
