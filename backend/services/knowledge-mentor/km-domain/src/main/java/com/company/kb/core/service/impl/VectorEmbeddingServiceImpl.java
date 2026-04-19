package com.company.kb.core.service.impl;

import com.company.kb.core.service.VectorEmbeddingService;
import com.company.kb.infra.ai.embedding.EmbeddingProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Vector embedding service implementation
 * Integrates with AI embedding provider (Qwen) through infrastructure layer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorEmbeddingServiceImpl implements VectorEmbeddingService {

    private final EmbeddingProvider embeddingProvider;

    @Override
    public float[] generateEmbedding(String text) throws Exception {
        log.debug("Generating embedding for text length: {}", text.length());

        float[] embedding = embeddingProvider.generateEmbedding(text);

        log.debug("Embedding generated successfully: dimensions={}", embedding.length);
        return embedding;
    }

    @Override
    public List<float[]> generateEmbeddingsBatch(List<String> texts) throws Exception {
        log.debug("Generating embeddings for {} texts", texts.size());

        List<float[]> embeddings = embeddingProvider.generateEmbeddingsBatch(texts);

        log.debug("Batch embeddings generated successfully: count={}, dimensions={}",
            embeddings.size(), embeddings.isEmpty() ? 0 : embeddings.get(0).length);

        return embeddings;
    }
}
