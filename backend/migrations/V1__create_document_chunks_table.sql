-- ============================================================================
-- Migration: Create document_chunks table for parent-child chunking
-- Author: Knowledge Base Team
-- Date: 2026-04-10
-- Description:
--   This migration creates the document_chunks table to support parent-child
--   chunking strategy inspired by PaiSmart's implementation.
--
--   Parent chunks (1MB): Provide context for retrieval
--   Child chunks (512 chars): Used for embedding and similarity search
--
-- PostgreSQL-Specific Features:
--   - BIGSERIAL for auto-incrementing primary keys
--   - VARCHAR array for tags (native PostgreSQL array support)
--   - CHECK constraint for chunk_type validation
--   - B-tree indexes for performance
--   - Foreign keys with CASCADE for referential integrity
-- ============================================================================

-- Create document_chunks table
CREATE TABLE IF NOT EXISTS document_chunks (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Foreign key to documents table
    document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,

    -- Self-reference for parent-child relationship
    -- NULL for parent chunks, points to parent for child chunks
    parent_chunk_id BIGINT REFERENCES document_chunks(id) ON DELETE SET NULL,

    -- Chunk type: PARENT or CHILD
    chunk_type VARCHAR(10) NOT NULL CHECK (chunk_type IN ('PARENT', 'CHILD')),

    -- Chunk content (TEXT type for unlimited length)
    content TEXT NOT NULL,

    -- Position within document
    position INTEGER NOT NULL,

    -- Character count (for size tracking)
    char_count INTEGER,

    -- Embedding vector (stored as BYTEA for efficiency)
    -- Note: Can use pgvector extension for native vector operations
    embedding_vector BYTEA,

    -- Embedding metadata
    embedding_model VARCHAR(50),
    vector_dimensions INTEGER,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_document_chunks_document_id
    ON document_chunks(document_id);

CREATE INDEX IF NOT EXISTS idx_document_chunks_parent_id
    ON document_chunks(parent_chunk_id);

CREATE INDEX IF NOT EXISTS idx_document_chunks_type
    ON document_chunks(chunk_type);

-- Composite index for common queries: find all child chunks of a parent
CREATE INDEX IF NOT EXISTS idx_document_chunks_parent_type
    ON document_chunks(parent_chunk_id, chunk_type);

-- Composite index for document queries: find all chunks of a document by type
CREATE INDEX IF NOT EXISTS idx_document_chunks_document_type
    ON document_chunks(document_id, chunk_type);

-- Add comments for documentation
COMMENT ON TABLE document_chunks IS 'Stores parent and child chunks for intelligent document chunking strategy';

COMMENT ON COLUMN document_chunks.id IS 'Primary key';
COMMENT ON COLUMN document_chunks.document_id IS 'Foreign key to documents table';
COMMENT ON COLUMN document_chunks.parent_chunk_id IS 'Self-reference for parent-child relationship (NULL for parent chunks)';
COMMENT ON COLUMN document_chunks.chunk_type IS 'Chunk type: PARENT (1MB context) or CHILD (512 char embedding)';
COMMENT ON COLUMN document_chunks.content IS 'Chunk text content';
COMMENT ON COLUMN document_chunks.position IS 'Position within document';
COMMENT ON COLUMN document_chunks.char_count IS 'Character count for size tracking';
COMMENT ON COLUMN document_chunks.embedding_vector IS 'Embedding vector stored as byte array';
COMMENT ON COLUMN document_chunks.embedding_model IS 'Embedding model version (e.g., text-embedding-v4)';
COMMENT ON COLUMN document_chunks.vector_dimensions IS 'Vector dimensions (e.g., 1536 for Qwen)';
COMMENT ON COLUMN document_chunks.created_at IS 'Creation timestamp';

-- ============================================================================
-- Performance Notes:
--   - BYTEA for embedding_vector: Efficient storage, ~4MB per 1M vectors
--   - Alternative: Install pgvector extension for native vector operations
--     CREATE EXTENSION IF NOT EXISTS vector;
--     ALTER TABLE document_chunks ADD COLUMN embedding_vec vector(1536);
--
--   - Indexes cover common query patterns:
--     * Find all chunks by document: idx_document_chunks_document_id
--     * Find children by parent: idx_document_chunks_parent_id
--     * Filter by chunk type: idx_document_chunks_type
-- ============================================================================

-- ============================================================================
-- Expected usage patterns:
--
-- 1. Insert parent chunks:
--    INSERT INTO document_chunks (document_id, chunk_type, content, position, char_count)
--    VALUES (123, 'PARENT', '...large text...', 0, 1048576);
--
-- 2. Insert child chunks with parent reference:
--    INSERT INTO document_chunks (document_id, parent_chunk_id, chunk_type, content, position)
--    VALUES (123, 456, 'CHILD', '...small text...', 0);
--
-- 3. Query all chunks for a document:
--    SELECT * FROM document_chunks WHERE document_id = 123 ORDER BY position;
--
-- 4. Query child chunks of a parent:
--    SELECT * FROM document_chunks WHERE parent_chunk_id = 456 ORDER BY position;
--
-- 5. Count chunks by type:
--    SELECT chunk_type, COUNT(*) FROM document_chunks GROUP BY chunk_type;
-- ============================================================================
