package com.xinzhe.projectmentor.retrieval.chunk;

public record CodeChunk(
        String chunkId,
        Long fileId,
        String filePath,
        String language,
        String chunkType,
        String symbolName,
        int startLine,
        int endLine,
        String contentHash,
        String content,
        String searchText,
        String embeddingText
) {
}
