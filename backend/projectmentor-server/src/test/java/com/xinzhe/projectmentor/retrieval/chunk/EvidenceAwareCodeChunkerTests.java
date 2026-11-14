package com.xinzhe.projectmentor.retrieval.chunk;

import com.xinzhe.projectmentor.config.VectorIndexProperties;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceAwareCodeChunkerTests {
    private final VectorIndexProperties properties = properties();
    private final SensitiveContentRedactor redactor = new SensitiveContentRedactor();
    private final EvidenceAwareCodeChunker chunker = new EvidenceAwareCodeChunker(
            properties, redactor, new IndexableFilePolicy());

    @Test
    void chunkIdsAndLineRangesAreStableAndOverlap() {
        ProjectFile file = file("src/JwtAuthenticationFilter.java", String.join("\n",
                "public class JwtAuthenticationFilter {", "  private int one;", "", "  public void first() {",
                "    one++;", "  }", "", "  public void second() {", "    one++;", "  }", "}"));

        List<CodeChunk> first = chunker.chunk(42L, file, "mock-v1");
        List<CodeChunk> second = chunker.chunk(42L, file, "mock-v1");

        assertThat(first).extracting(CodeChunk::chunkId).containsExactlyElementsOf(
                second.stream().map(CodeChunk::chunkId).toList());
        assertThat(first).allSatisfy(chunk -> {
            assertThat(chunk.startLine()).isPositive();
            assertThat(chunk.endLine()).isGreaterThanOrEqualTo(chunk.startLine());
        });
        assertThat(first).hasSizeGreaterThan(1);
        assertThat(first.get(1).startLine()).isLessThanOrEqualTo(first.get(0).endLine());
        assertThat(first.get(0).searchText()).contains("Jwt Authentication Filter");
    }

    @Test
    void searchTextExpandsSnakeCaseAndEmbeddingTextRedactsSecrets() {
        ProjectFile file = file("config/auth_config.yml", "auth_config:\n  api_key: sk-not-a-real-secret-value-123456\n");
        CodeChunk chunk = chunker.chunk(7L, file, "mock-v1").get(0);
        assertThat(chunk.searchText()).contains("auth config").doesNotContain("sk-not-a-real-secret");
        assertThat(chunk.embeddingText()).contains("[REDACTED]").doesNotContain("secret-value-123456");
        assertThat(chunk.content()).contains("sk-not-a-real-secret-value-123456");
    }

    @Test
    void rejectsSensitiveAndGeneratedPaths() {
        assertThat(chunker.chunk(1L, file(".env", "API_KEY=secret"), "m")).isEmpty();
        assertThat(chunker.chunk(1L, file("node_modules/a.js", "export default 1"), "m")).isEmpty();
        assertThat(chunker.chunk(1L, file("target/A.java", "class A {}"), "m")).isEmpty();
    }

    private VectorIndexProperties properties() {
        VectorIndexProperties value = new VectorIndexProperties();
        value.setChunkMaxLines(6);
        value.setChunkOverlapLines(2);
        value.setChunkMaxChars(10000);
        return value;
    }

    private ProjectFile file(String path, String content) {
        ProjectFile file = new ProjectFile();
        file.setId(99L);
        file.setProjectId(42L);
        file.setFilePath(path);
        file.setContent(content);
        return file;
    }
}
