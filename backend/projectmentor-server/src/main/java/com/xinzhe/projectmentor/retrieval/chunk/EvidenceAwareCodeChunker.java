package com.xinzhe.projectmentor.retrieval.chunk;

import com.xinzhe.projectmentor.config.VectorIndexProperties;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class EvidenceAwareCodeChunker {
    public static final String CHUNKER_VERSION = "evidence-heuristic-v1";
    private static final Pattern CAMEL = Pattern.compile("(?<=[a-z0-9])(?=[A-Z])");
    private static final Pattern SYMBOL = Pattern.compile(
            "(?i)^\\s*(?:public|private|protected|static|final|async|export|default|abstract|synchronized|def|class|interface|enum|record|function|CREATE\\s+(?:TABLE|VIEW|INDEX)|ALTER\\s+TABLE)?\\s*" +
                    "(?:class|interface|enum|record|def|function|fun|void|[A-Za-z_$][\\w$<>?,.\\[\\]]*)?\\s*([A-Za-z_$][\\w$-]*)\\s*(?:\\(|\\{|:|=)?");
    private static final Pattern STRUCTURAL = Pattern.compile(
            "(?i)^\\s*(?:@(?:Rest)?Controller|@Service|@Configuration|class\\s+|interface\\s+|enum\\s+|record\\s+|" +
                    "(?:public|private|protected|static|async|export|def|function)\\s+|CREATE\\s+|ALTER\\s+|DROP\\s+|" +
                    "services:|volumes:|dependencies:|<template>|<script|#{1,6}\\s+)");

    private final VectorIndexProperties properties;
    private final SensitiveContentRedactor redactor;
    private final IndexableFilePolicy filePolicy;

    public List<CodeChunk> chunk(long projectId, ProjectFile file, String embeddingModel) {
        if (!filePolicy.isIndexable(file)) {
            return List.of();
        }
        String normalized = normalizeNewlines(file.getContent());
        String[] lines = normalized.split("\\n", -1);
        String language = detectLanguage(file.getFilePath());
        String fileHash = sha256(normalized);
        List<CodeChunk> chunks = new ArrayList<>();
        int start = 0;
        while (start < lines.length) {
            int hardEnd = Math.min(lines.length, start + properties.getChunkMaxLines());
            int end = chooseBoundary(lines, start, hardEnd);
            if (end <= start) {
                end = hardEnd;
            }
            while (joinedLength(lines, start, end) > properties.getChunkMaxChars() && end > start + 1) {
                end--;
            }
            String raw = join(lines, start, end);
            if (!raw.isBlank()) {
                SensitiveContentRedactor.RedactionResult safe = redactor.redact(raw);
                String symbol = findSymbol(lines, start, end);
                String chunkType = detectChunkType(file.getFilePath(), language, raw);
                int startLine = start + 1;
                int endLine = end;
                String chunkId = stableChunkId(projectId, file, fileHash, startLine, endLine, embeddingModel);
                String searchText = buildSearchText(file.getFilePath(), language, symbol, chunkType, safe.content());
                String embeddingText = "Path: " + file.getFilePath() + "\nLanguage: " + language
                        + "\nSymbol: " + symbol + "\nChunk Type: " + chunkType + "\nContent:\n" + safe.content();
                chunks.add(new CodeChunk(chunkId, file.getId(), file.getFilePath(), language, chunkType,
                        symbol, startLine, endLine, fileHash, raw, searchText, embeddingText));
            }
            if (end >= lines.length) {
                break;
            }
            int overlap = Math.min(properties.getChunkOverlapLines(), Math.max(0, end - start - 1));
            start = end - overlap;
        }
        return chunks;
    }

    private int chooseBoundary(String[] lines, int start, int hardEnd) {
        if (hardEnd == lines.length) return hardEnd;
        int lower = Math.max(start + Math.max(10, properties.getChunkMaxLines() / 2), start + 1);
        for (int i = hardEnd; i > lower; i--) {
            String previous = lines[i - 1].trim();
            String next = i < lines.length ? lines[i].trim() : "";
            if (previous.isEmpty() || next.isEmpty() || STRUCTURAL.matcher(next).find()) return i;
        }
        return hardEnd;
    }

    private String findSymbol(String[] lines, int start, int end) {
        for (int i = start; i < end; i++) {
            if (!STRUCTURAL.matcher(lines[i]).find()) continue;
            Matcher matcher = SYMBOL.matcher(lines[i]);
            if (matcher.find() && matcher.group(1) != null) return matcher.group(1);
        }
        return "file-scope";
    }

    private String buildSearchText(String path, String language, String symbol, String type, String safeContent) {
        String tokens = (path + " " + symbol).replace('_', ' ').replace('-', ' ').replace('/', ' ');
        StringBuilder expanded = new StringBuilder();
        for (String token : tokens.split("\\s+")) {
            expanded.append(token).append(' ');
            for (String part : CAMEL.split(token)) expanded.append(part).append(' ');
        }
        return "path " + path + " language " + language + " symbol " + symbol + " type " + type
                + " tokens " + expanded.toString().trim() + " content " + safeContent;
    }

    private String stableChunkId(long projectId, ProjectFile file, String contentHash,
                                 int startLine, int endLine, String embeddingModel) {
        return sha256(projectId + "\n" + (file.getId() == null ? "" : file.getId()) + "\n"
                + file.getFilePath() + "\n" + contentHash + "\n" + startLine + "\n" + endLine + "\n"
                + CHUNKER_VERSION + "\n" + embeddingModel);
    }

    private String detectChunkType(String path, String language, String content) {
        String lowerPath = path.toLowerCase(Locale.ROOT);
        if ("sql".equals(language)) return "SQL";
        if (List.of("yaml", "json", "properties", "xml", "dockerfile").contains(language)) return "CONFIG";
        if (lowerPath.contains("deploy") || lowerPath.contains("docker") || lowerPath.contains("nginx")) return "DEPLOY";
        if ("markdown".equals(language)) return "DOC";
        if (content.matches("(?s).*\\b(class|interface|record|enum)\\b.*")) return "TYPE";
        if (content.matches("(?s).*\\b(def|function|public|private|protected|async)\\b.*\\(.*")) return "FUNCTION";
        return "CODE";
    }

    public static String detectLanguage(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".java")) return "java";
        if (lower.endsWith(".ts") || lower.endsWith(".tsx")) return "typescript";
        if (lower.endsWith(".js") || lower.endsWith(".jsx")) return "javascript";
        if (lower.endsWith(".vue")) return "vue";
        if (lower.endsWith(".py")) return "python";
        if (lower.endsWith(".sql")) return "sql";
        if (lower.endsWith(".yml") || lower.endsWith(".yaml")) return "yaml";
        if (lower.endsWith(".json")) return "json";
        if (lower.endsWith(".md")) return "markdown";
        if (lower.endsWith(".xml")) return "xml";
        if (lower.endsWith(".properties")) return "properties";
        if (lower.endsWith(".sh")) return "shell";
        if (lower.endsWith("dockerfile") || lower.endsWith("/dockerfile")) return "dockerfile";
        return "text";
    }

    private static String normalizeNewlines(String value) {
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static String join(String[] lines, int start, int end) {
        return String.join("\n", java.util.Arrays.copyOfRange(lines, start, end));
    }

    private static int joinedLength(String[] lines, int start, int end) {
        int length = Math.max(0, end - start - 1);
        for (int i = start; i < end; i++) length += lines[i].length();
        return length;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
