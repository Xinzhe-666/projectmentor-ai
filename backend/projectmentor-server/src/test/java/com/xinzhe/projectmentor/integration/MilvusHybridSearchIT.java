package com.xinzhe.projectmentor.integration;

import com.xinzhe.projectmentor.config.EmbeddingProperties;
import com.xinzhe.projectmentor.config.MilvusProperties;
import com.xinzhe.projectmentor.config.RetrievalProperties;
import com.xinzhe.projectmentor.retrieval.chunk.CodeChunk;
import com.xinzhe.projectmentor.retrieval.chunk.SensitiveContentRedactor;
import com.xinzhe.projectmentor.retrieval.milvus.MilvusChunkStore;
import com.xinzhe.projectmentor.retrieval.milvus.MilvusCollectionManager;
import com.xinzhe.projectmentor.retrieval.search.RetrievalCandidate;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class MilvusHybridSearchIT {
    private static MilvusClientV2 client;
    private static MilvusChunkStore store;
    private static String uri;
    private static String token;
    private static String database;
    private static String collection;
    private static final long USER_A = 7001L;
    private static final long USER_B = 7002L;
    private static final long PROJECT = 8001L;
    private static final long OTHER_PROJECT = 8002L;
    private static final long GENERATION = 11L;
    private static final String BUILD = "milvus-it-build";

    @BeforeAll
    static void setup() {
        uri = System.getenv("MILVUS_IT_URI");
        assumeTrue(uri != null && !uri.isBlank(), "MILVUS_IT_URI not configured");
        token = System.getenv("MILVUS_IT_TOKEN");
        database = env("MILVUS_DATABASE", "pmai");
        collection = env("MILVUS_COLLECTION", "pmai_code_chunks_v1");
        client = newClient();
        MilvusProperties milvus = new MilvusProperties();
        milvus.setEnabled(true); milvus.setUri(uri); milvus.setToken(token);
        milvus.setDatabase(database); milvus.setCollection(collection); milvus.setTimeoutSeconds(30);
        EmbeddingProperties embedding = new EmbeddingProperties();
        embedding.setEnabled(true); embedding.setDimension(4); embedding.setModel("ci-mock-embedding");
        new MilvusCollectionManager(client, milvus, embedding).ensureCollection();
        store = newStore(client);

        List<CodeChunk> a = List.of(
                chunk("a-auth", 1L, "src/AuthService.java", "java", "TYPE", "AuthService", "hash-a",
                        "class AuthService { void issueJwtToken() {} }", "Auth Service issue Jwt Token authentication"),
                chunk("a-config", 2L, "application.yml", "yaml", "CONFIG", "security", "hash-b",
                        "security:\n  jwt-expire: 3600", "application yml security jwt expire")
        );
        assertThat(store.insert(USER_A, PROJECT, GENERATION, BUILD, a,
                List.of(new float[]{1, 0, 0, 0}, new float[]{0.8f, 0.2f, 0, 0}))).isEqualTo(2);
        assertThat(store.insert(USER_B, PROJECT, GENERATION, BUILD,
                List.of(chunk("b-secret", 3L, "src/Other.java", "java", "CODE", "Other", "hash-c",
                        "class Other { void jwt() {} }", "jwt other tenant")),
                List.of(new float[]{1, 0, 0, 0}))).isEqualTo(1);
        assertThat(store.insert(USER_A, OTHER_PROJECT, GENERATION, BUILD,
                List.of(chunk("a-other-project", 4L, "src/CrossProject.java", "java", "CODE",
                        "CrossProject", "hash-d", "class CrossProject { void issueJwtToken() {} }",
                        "Cross Project issue Jwt Token authentication")),
                List.of(new float[]{1, 0, 0, 0}))).isEqualTo(1);
    }

    @AfterAll
    static void cleanup() {
        if (store != null) {
            store.deleteBuild(USER_A, PROJECT, GENERATION, BUILD);
            store.deleteBuild(USER_B, PROJECT, GENERATION, BUILD);
            store.deleteBuild(USER_A, OTHER_PROJECT, GENERATION, BUILD);
        }
        if (client != null) client.close();
    }

    @Test
    void denseBm25HybridRrfAndTenantGenerationFiltersAreReal() {
        List<RetrievalCandidate> hits = store.hybridSearch(USER_A, PROJECT, GENERATION, BUILD,
                "AuthService jwt-expire", new float[]{1, 0, 0, 0});
        assertThat(hits).isNotEmpty();
        assertThat(hits).allSatisfy(hit -> assertThat(hit.filePath())
                .doesNotContain("Other.java", "CrossProject.java"));
        assertThat(hits).anySatisfy(hit -> {
            assertThat(hit.filePath()).isEqualTo("src/AuthService.java");
            assertThat(hit.denseRank()).isNotNull();
            assertThat(hit.bm25Rank()).isNotNull();
            assertThat(hit.rrfRank()).isPositive();
        });
        assertThat(store.hybridSearch(USER_A, PROJECT, GENERATION + 1, BUILD,
                "AuthService", new float[]{1, 0, 0, 0})).isEmpty();

        MilvusClientV2 reconnected = newClient();
        try {
            assertThat(newStore(reconnected).hybridSearch(USER_A, PROJECT, GENERATION, BUILD,
                    "AuthService", new float[]{1, 0, 0, 0})).isNotEmpty();
        } finally {
            reconnected.close();
        }

        assertThat(store.insert(USER_A, PROJECT, GENERATION, BUILD,
                List.of(chunk("a-auth", 1L, "src/AuthService.java", "java", "TYPE", "AuthService",
                        "hash-a2", "class AuthService { void issueJwtTokenV2() {} }",
                        "Auth Service issue Jwt Token V2 authentication")),
                List.of(new float[]{1, 0, 0, 0}))).isEqualTo(1);
        assertThat(store.hybridSearch(USER_A, PROJECT, GENERATION, BUILD,
                "issueJwtTokenV2", new float[]{1, 0, 0, 0}))
                .anySatisfy(hit -> assertThat(hit.content()).contains("issueJwtTokenV2"));

        assertThat(store.hybridSearch(USER_A, OTHER_PROJECT, GENERATION, BUILD,
                "CrossProject", new float[]{1, 0, 0, 0})).isNotEmpty();
        store.deleteProject(USER_A, OTHER_PROJECT);
        assertThat(store.hybridSearch(USER_A, OTHER_PROJECT, GENERATION, BUILD,
                "CrossProject", new float[]{1, 0, 0, 0})).isEmpty();

        store.deleteBuild(USER_A, PROJECT, GENERATION, BUILD);
        assertThat(store.hybridSearch(USER_A, PROJECT, GENERATION, BUILD,
                "AuthService", new float[]{1, 0, 0, 0})).isEmpty();
    }

    private static MilvusClientV2 newClient() {
        return new MilvusClientV2(ConnectConfig.builder().uri(uri).token(token).dbName(database)
                .connectTimeoutMs(10000).rpcDeadlineMs(30000).build());
    }

    private static MilvusChunkStore newStore(MilvusClientV2 targetClient) {
        MilvusProperties milvus = new MilvusProperties();
        milvus.setEnabled(true); milvus.setUri(uri); milvus.setToken(token);
        milvus.setDatabase(database); milvus.setCollection(collection); milvus.setTimeoutSeconds(30);
        RetrievalProperties retrieval = new RetrievalProperties();
        retrieval.setCandidateLimit(10); retrieval.setResultLimit(5); retrieval.setRrfK(60);
        retrieval.setPerPathDiagnosticsEnabled(true);
        return new MilvusChunkStore(targetClient, milvus, retrieval,
                new SensitiveContentRedactor(), new SimpleMeterRegistry());
    }

    private static CodeChunk chunk(String id, long fileId, String path, String language, String type,
                                   String symbol, String hash, String content, String search) {
        return new CodeChunk(id, fileId, path, language, type, symbol, 1, 3, hash, content, search, search);
    }
    private static String env(String key, String fallback) {
        String value = System.getenv(key); return value == null || value.isBlank() ? fallback : value;
    }
}
