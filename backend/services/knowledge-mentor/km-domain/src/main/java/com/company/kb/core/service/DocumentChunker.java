package com.company.kb.core.service;

import com.company.kb.core.domain.Document;
import com.company.kb.core.domain.DocumentChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple document chunking utility
 * Provides basic text splitting functionality for document processing
 */
@Slf4j
@Component
public class DocumentChunker {

    private static final int DEFAULT_CHUNK_SIZE = 1000;
    private static final int DEFAULT_OVERLAP = 100;

    /**
     * Split document content into chunks
     *
     * @param content Document text content
     * @param document Document entity
     * @return List of document chunks
     */
    public List<DocumentChunk> chunkDocument(String content, Document document) {
        List<DocumentChunk> chunks = new ArrayList<>();

        if (content == null || content.isEmpty()) {
            log.warn("Document content is empty, cannot chunk");
            return chunks;
        }

        int start = 0;
        int chunkIndex = 0;

        while (start < content.length()) {
            int end = Math.min(start + DEFAULT_CHUNK_SIZE, content.length());
            String chunkContent = content.substring(start, end);

            DocumentChunk chunk = DocumentChunk.builder()
                    .document(document)
                    .documentId(document.getId())
                    .chunkIndex(chunkIndex++)
                    .textContent(chunkContent)
                    .charCount(chunkContent.length())
                    .build();

            chunks.add(chunk);

            // Move to next chunk with overlap
            // If we reached the end of the document, break to avoid infinite loop
            if (end >= content.length()) {
                break;
            }
            start = end - DEFAULT_OVERLAP;
            if (start < 0) start = 0;
        }

        log.info("Document chunking completed: documentId={}, chunkCount={}", document.getId(), chunks.size());
        return chunks;
    }
}
