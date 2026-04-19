package com.company.kb.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Document Chunk Entity
 *
 * Unified chunk entity that replaces separate ParentChunk and ChildChunk entities.
 * Supports both parent (large context) and child (small embedding) chunks in a single table.
 *
 * @author Knowledge Base Team
 */
@Entity
@Table(name = "document_chunks", indexes = {
    @Index(name = "idx_document_chunks_document_id", columnList = "document_id"),
    @Index(name = "idx_document_chunks_chunk_index", columnList = "chunk_index")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Document this chunk belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    @JsonIgnore
    private Document document;

    @Column(name = "document_id", nullable = false, insertable = false, updatable = false)
    private Long documentId;

    /**
     * Sequential index of this chunk within the document
     */
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    /**
     * Chunk content
     */
    @Column(name = "text_content", nullable = false, columnDefinition = "TEXT")
    private String textContent;

    /**
     * Elasticsearch document ID for this chunk
     */
    @Column(name = "es_doc_id", length = 100)
    private String esDocId;

    /**
     * Embedding vector (1536 dimensions for Qwen)
     * Stored as byte array for PostgreSQL efficiency
     */
    @Lob
    @Column(name = "embedding_vector")
    private byte[] embeddingVector;

    /**
     * Embedding model version (e.g., "text-embedding-v4")
     */
    @Column(name = "embedding_model", length = 50)
    private String embeddingModel;

    /**
     * Vector dimensions
     */
    @Column(name = "vector_dimensions")
    private Integer vectorDimensions;

    /**
     * Character count in this chunk
     */
    @Column(name = "char_count")
    private Integer charCount;

    /**
     * When the chunk was created
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Check if this chunk has an embedding vector
     */
    public boolean hasEmbedding() {
        return embeddingVector != null && embeddingVector.length > 0;
    }

    /**
     * Get embedding vector size in bytes
     */
    public int getEmbeddingSize() {
        return hasEmbedding() ? embeddingVector.length : 0;
    }

    /**
     * Set embedding vector from float array
     *
     * @param vector Float array from embedding API
     */
    public void setEmbeddingFromFloatArray(float[] vector) {
        if (vector == null || vector.length == 0) {
            this.embeddingVector = null;
            this.vectorDimensions = 0;
            return;
        }

        // Convert float array to byte array
        // Each float = 4 bytes
        this.embeddingVector = new byte[vector.length * 4];
        this.vectorDimensions = vector.length;

        for (int i = 0; i < vector.length; i++) {
            int bits = Float.floatToIntBits(vector[i]);
            this.embeddingVector[i * 4] = (byte) (bits >> 24);
            this.embeddingVector[i * 4 + 1] = (byte) (bits >> 16);
            this.embeddingVector[i * 4 + 2] = (byte) (bits >> 8);
            this.embeddingVector[i * 4 + 3] = (byte) bits;
        }
    }

    /**
     * Get embedding vector as float array
     *
     * @return Float array for Elasticsearch indexing
     */
    public float[] getEmbeddingAsFloatArray() {
        if (!hasEmbedding() || vectorDimensions == null) {
            return new float[0];
        }

        float[] vector = new float[vectorDimensions];
        for (int i = 0; i < vectorDimensions; i++) {
            int bits = ((embeddingVector[i * 4] & 0xFF) << 24) |
                       ((embeddingVector[i * 4 + 1] & 0xFF) << 16) |
                       ((embeddingVector[i * 4 + 2] & 0xFF) << 8) |
                       (embeddingVector[i * 4 + 3] & 0xFF);
            vector[i] = Float.intBitsToFloat(bits);
        }

        return vector;
    }
}
