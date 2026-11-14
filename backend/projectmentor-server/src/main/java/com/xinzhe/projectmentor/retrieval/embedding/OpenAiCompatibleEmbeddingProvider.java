package com.xinzhe.projectmentor.retrieval.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinzhe.projectmentor.config.EmbeddingProperties;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "projectmentor.embedding", name = "enabled", havingValue = "true")
public class OpenAiCompatibleEmbeddingProvider implements EmbeddingProvider {
    private final EmbeddingProperties properties;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final HttpClient httpClient;

    public OpenAiCompatibleEmbeddingProvider(EmbeddingProperties properties,
                                             ObjectMapper objectMapper,
                                             MeterRegistry meterRegistry) {
        this(properties, objectMapper, meterRegistry, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds())).build());
    }

    OpenAiCompatibleEmbeddingProvider(EmbeddingProperties properties,
                                      ObjectMapper objectMapper,
                                      MeterRegistry meterRegistry,
                                      HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.httpClient = httpClient;
        validateConfiguration();
    }

    @Override
    public List<float[]> embed(List<String> inputs) {
        if (inputs == null || inputs.isEmpty()) return List.of();
        List<float[]> result = new ArrayList<>(inputs.size());
        for (int from = 0; from < inputs.size(); from += properties.getBatchSize()) {
            int to = Math.min(inputs.size(), from + properties.getBatchSize());
            List<String> batch = inputs.subList(from, to).stream().map(this::bounded).toList();
            result.addAll(executeWithRetry(batch));
        }
        return result;
    }

    private List<float[]> executeWithRetry(List<String> batch) {
        EmbeddingException last = null;
        for (int attempt = 0; attempt <= properties.getMaxRetries(); attempt++) {
            long started = System.nanoTime();
            try {
                List<float[]> vectors = execute(batch);
                meterRegistry.counter("pmai.embedding.batch", "result", "success").increment();
                meterRegistry.timer("pmai.embedding.latency", "result", "success")
                        .record(System.nanoTime() - started, TimeUnit.NANOSECONDS);
                return vectors;
            } catch (EmbeddingException e) {
                last = e;
                meterRegistry.counter("pmai.embedding.error", "code", lowCardinalityCode(e.code())).increment();
                if (!e.retryable() || attempt >= properties.getMaxRetries()) throw e;
                backoff(attempt);
            }
        }
        throw last == null ? new EmbeddingException("UNKNOWN", false, "Embedding failed") : last;
    }

    private List<float[]> execute(List<String> batch) {
        try {
            byte[] body = objectMapper.writeValueAsBytes(Map.of(
                    "model", properties.getModel(),
                    "input", batch,
                    "dimensions", properties.getDimension()
            ));
            HttpRequest request = HttpRequest.newBuilder(embeddingUri())
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                boolean retryable = response.statusCode() == 408 || response.statusCode() == 429
                        || response.statusCode() >= 500;
                throw new EmbeddingException("HTTP_" + response.statusCode(), retryable,
                        "Embedding provider returned HTTP " + response.statusCode());
            }
            return parseVectors(response.body(), batch.size());
        } catch (EmbeddingException e) {
            throw e;
        } catch (java.net.http.HttpTimeoutException e) {
            throw new EmbeddingException("TIMEOUT", true, "Embedding request timed out", e);
        } catch (IOException e) {
            throw new EmbeddingException("IO_ERROR", true, "Embedding provider unavailable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EmbeddingException("INTERRUPTED", false, "Embedding request interrupted", e);
        }
    }

    private List<float[]> parseVectors(byte[] body, int expectedCount) {
        try {
            JsonNode data = objectMapper.readTree(body).path("data");
            if (!data.isArray() || data.size() != expectedCount) {
                throw new EmbeddingException("COUNT_MISMATCH", false, "Embedding result count mismatch");
            }
            List<IndexedVector> indexed = new ArrayList<>(expectedCount);
            for (int item = 0; item < data.size(); item++) {
                JsonNode row = data.get(item);
                int index = row.path("index").isInt() ? row.path("index").asInt() : item;
                JsonNode embedding = row.path("embedding");
                if (!embedding.isArray() || embedding.size() != properties.getDimension()) {
                    throw new EmbeddingException("DIMENSION_MISMATCH", false,
                            "Embedding vector dimension mismatch");
                }
                float[] vector = new float[properties.getDimension()];
                for (int i = 0; i < vector.length; i++) {
                    double value = embedding.get(i).asDouble(Double.NaN);
                    if (!Double.isFinite(value)) {
                        throw new EmbeddingException("NON_FINITE_VECTOR", false,
                                "Embedding vector contains a non-finite value");
                    }
                    vector[i] = (float) value;
                }
                indexed.add(new IndexedVector(index, vector));
            }
            indexed.sort(Comparator.comparingInt(IndexedVector::index));
            for (int i = 0; i < indexed.size(); i++) {
                if (indexed.get(i).index() != i) {
                    throw new EmbeddingException("INDEX_MISMATCH", false, "Embedding result index mismatch");
                }
            }
            return indexed.stream().map(IndexedVector::vector).toList();
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            throw new EmbeddingException("INVALID_RESPONSE", false, "Embedding response was invalid", e);
        }
    }

    private URI embeddingUri() {
        String base = properties.getBaseUrl().replaceAll("/+$", "");
        return URI.create(base.endsWith("/embeddings") ? base : base + "/embeddings");
    }

    private String bounded(String value) {
        if (value == null) return "";
        return value.length() <= properties.getMaxInputChars()
                ? value : value.substring(0, properties.getMaxInputChars());
    }

    private void validateConfiguration() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("EMBEDDING_API_KEY is required when embedding is enabled");
        }
        if (properties.getBaseUrl() == null || properties.getBaseUrl().isBlank()) {
            throw new IllegalStateException("EMBEDDING_BASE_URL is required when embedding is enabled");
        }
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(Math.min(2000L, 200L * (1L << Math.min(attempt, 3))));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EmbeddingException("INTERRUPTED", false, "Embedding retry interrupted", e);
        }
    }

    private String lowCardinalityCode(String code) {
        if (code == null) return "unknown";
        if (code.startsWith("HTTP_5")) return "http_5xx";
        if (code.equals("HTTP_429")) return "rate_limit";
        if (code.startsWith("HTTP_4")) return "http_4xx";
        return code.toLowerCase();
    }

    @Override public String model() { return properties.getModel(); }
    @Override public int dimension() { return properties.getDimension(); }
    private record IndexedVector(int index, float[] vector) {}
}
