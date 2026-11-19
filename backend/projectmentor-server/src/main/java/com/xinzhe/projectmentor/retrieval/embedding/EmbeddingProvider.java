package com.xinzhe.projectmentor.retrieval.embedding;

import java.util.List;

public interface EmbeddingProvider {
    List<float[]> embed(List<String> inputs);
    String model();
    int dimension();
}
