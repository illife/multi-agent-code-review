package com.company.kb.core.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Document Uploaded Event
 *
 * Kafka event message published when a document is uploaded.
 * Contains the documentId for async processing via DocumentProcessingConsumer.
 *
 * @author Knowledge Base Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploaded {

    /**
     * Document ID
     */
    private Long documentId;

    /**
     * Document title
     */
    private String title;

    /**
     * File name
     */
    private String fileName;

    /**
     * File type
     */
    private String fileType;

    /**
     * Uploaded by username
     */
    private String uploadedBy;

    /**
     * Event timestamp
     */
    private LocalDateTime timestamp;

    /**
     * Event type (for routing/filtering)
     */
    private String eventType;

    /**
     * Create a DocumentUploaded event from document ID
     */
    public static DocumentUploaded from(Long documentId) {
        return DocumentUploaded.builder()
                .documentId(documentId)
                .eventType("DOCUMENT_UPLOADED")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create a DocumentUploaded event with full metadata
     */
    public static DocumentUploaded from(Long documentId, String title, String fileName,
                                       String fileType, String uploadedBy) {
        return DocumentUploaded.builder()
                .documentId(documentId)
                .title(title)
                .fileName(fileName)
                .fileType(fileType)
                .uploadedBy(uploadedBy)
                .eventType("DOCUMENT_UPLOADED")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
