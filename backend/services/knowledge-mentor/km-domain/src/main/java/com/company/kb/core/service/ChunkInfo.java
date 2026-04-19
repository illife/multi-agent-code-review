package com.company.kb.core.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Document chunk information DTO
 * Used for search results and context building
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkInfo {
    private String id;
    private Double score;
    private String content;
    private String title;
    private String fileName;
    private Long documentId;
    private String highlight;
}
