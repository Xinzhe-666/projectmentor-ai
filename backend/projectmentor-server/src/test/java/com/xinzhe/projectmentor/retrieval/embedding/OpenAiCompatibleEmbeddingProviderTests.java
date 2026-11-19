package com.xinzhe.projectmentor.retrieval.embedding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.xinzhe.projectmentor.config.EmbeddingProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiCompatibleEmbeddingProviderTests {
    private HttpServer server;

    @AfterEach void stop() { if (server != null) server.stop(0); }

    @Test
    void batchesAndPreservesProviderOrderByIndex() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/embeddings", exchange -> {
            calls.incrementAndGet();
            byte[] response = "{\"data\":[{\"index\":1,\"embedding\":[0.3,0.4]},{\"index\":0,\"embedding\":[0.1,0.2]}]}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        EmbeddingProperties props = props();
        props.setBatchSize(2);
        OpenAiCompatibleEmbeddingProvider provider = provider(props);

        List<float[]> vectors = provider.embed(List.of("one", "two", "three", "four"));

        assertThat(calls).hasValue(2);
        assertThat(vectors).hasSize(4);
        assertThat(vectors.get(0)).containsExactly(0.1f, 0.2f);
    }

    @Test
    void dimensionMismatchIsPermanent() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/embeddings", exchange -> {
            byte[] response = "{\"data\":[{\"index\":0,\"embedding\":[0.1]}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        OpenAiCompatibleEmbeddingProvider provider = provider(props());
        assertThatThrownBy(() -> provider.embed(List.of("one")))
                .isInstanceOfSatisfying(EmbeddingException.class, e -> {
                    assertThat(e.code()).isEqualTo("DIMENSION_MISMATCH");
                    assertThat(e.retryable()).isFalse();
                });
    }

    @Test
    void retriesRateLimitButNotUnauthorized() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/embeddings", exchange -> {
            int status = calls.incrementAndGet() == 1 ? 429 : 200;
            byte[] response = (status == 200
                    ? "{\"data\":[{\"index\":0,\"embedding\":[0.1,0.2]}]}" : "{}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        EmbeddingProperties props = props();
        props.setMaxRetries(1);
        assertThat(provider(props).embed(List.of("one"))).hasSize(1);
        assertThat(calls).hasValue(2);
    }

    private OpenAiCompatibleEmbeddingProvider provider(EmbeddingProperties props) {
        props.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
        return new OpenAiCompatibleEmbeddingProvider(props, new ObjectMapper(), new SimpleMeterRegistry());
    }

    private EmbeddingProperties props() {
        EmbeddingProperties props = new EmbeddingProperties();
        props.setApiKey("unit-test-placeholder");
        props.setModel("mock-embedding");
        props.setDimension(2);
        props.setBatchSize(4);
        props.setMaxRetries(0);
        props.setTimeoutSeconds(2);
        return props;
    }
}
