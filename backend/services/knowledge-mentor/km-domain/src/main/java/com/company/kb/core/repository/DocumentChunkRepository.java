package com.company.kb.core.repository;

import com.company.kb.core.domain.DocumentChunk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Document Chunk Repository
 *
 * @author Knowledge Base Team
 */
@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    /**
     * Find all chunks for a document
     */
    List<DocumentChunk> findByDocumentId(Long documentId);

    /**
     * Find all chunks for a document ordered by chunk index
     */
    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    /**
     * Find paginated chunks for a document
     */
    Page<DocumentChunk> findByDocumentId(Long documentId, Pageable pageable);

    /**
     * Delete all chunks for a document
     */
    void deleteByDocumentId(Long documentId);

    /**
     * Count chunks for a document
     */
    long countByDocumentId(Long documentId);
}
