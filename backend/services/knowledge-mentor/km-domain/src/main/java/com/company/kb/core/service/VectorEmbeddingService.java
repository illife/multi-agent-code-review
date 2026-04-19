package com.company.kb.core.service;

import java.util.List;

/**
 * Text Vector Embedding Service
 * Responsible for converting text to vector representations for semantic search and similarity calculation
 */
public interface VectorEmbeddingService {

    /**
     * Generate text vector embedding (single)
     * @param text Input text
     * @return Vector array
     * @throws Exception Thrown when generation fails
     */
    float[] generateEmbedding(String text) throws Exception;

    /**
     * Generate text vector embeddings (batch)
     *
     * <p>Batch processing provides ~6x performance improvement by reducing API calls.</p>
     *
     * @param texts Input text list
     * @return List of vector arrays
     * @throws Exception Thrown when generation fails
     */
    List<float[]> generateEmbeddingsBatch(List<String> texts) throws Exception;
}
