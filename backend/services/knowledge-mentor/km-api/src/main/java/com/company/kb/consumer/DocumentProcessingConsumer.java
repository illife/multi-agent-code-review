package com.company.kb.consumer;

import com.company.kb.core.domain.Document;
import com.company.kb.core.domain.DocumentChunk;
import com.company.kb.core.event.DocumentUploaded;
import com.company.kb.core.repository.DocumentChunkRepository;
import com.company.kb.core.repository.DocumentRepository;
import com.company.kb.core.service.DocumentChunker;
import com.company.kb.infra.ai.embedding.EmbeddingProvider;
import com.company.kb.infra.document.DocumentParserService;
import com.company.kb.infra.elasticsearch.service.ElasticsearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.think.platform.shared.infra.minio.MinioStorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Document Processing Consumer
 *
 * Kafka consumer that asynchronously processes uploaded documents.
 * Implements the pipeline: parse -> chunk -> embed -> index
 *
 * Features:
 * - @KafkaListener on "document-processing" topic
 * - Parses DocumentUploaded events containing documentId
 * - Processes documents through the full pipeline
 * - Updates document status from PROCESSING to INDEXED/FAILED
 * - Implements retry logic with exponential backoff
 * - Sends failed documents to DLQ after retries exhausted
 *
 * Error Handling:
 * - Catches exceptions and logs detailed errors
 * - Retries up to 3 times with exponential backoff
 * - Sends failed documents to DLQ after retries exhausted
 *
 * @author Knowledge Base Team
 */
@Slf4j
@Component
public class DocumentProcessingConsumer {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentParserService documentParserService;
    private final DocumentChunker documentChunker;
    private final EmbeddingProvider embeddingProvider;
    private final ElasticsearchService elasticsearchService;
    private final MinioStorageService minioStorageService;
    private final ObjectMapper objectMapper;

    @Autowired
    public DocumentProcessingConsumer(
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            DocumentParserService documentParserService,
            DocumentChunker documentChunker,
            @Qualifier("qwenEmbeddingProvider") EmbeddingProvider embeddingProvider,
            ElasticsearchService elasticsearchService,
            MinioStorageService minioStorageService,
            ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.documentParserService = documentParserService;
        this.documentChunker = documentChunker;
        this.embeddingProvider = embeddingProvider;
        this.elasticsearchService = elasticsearchService;
        this.minioStorageService = minioStorageService;
        this.objectMapper = objectMapper;
    }

    /**
     * Maximum retry attempts for failed processing
     */
    private static final int MAX_RETRY_ATTEMPTS = 3;

    /**
     * Base delay for exponential backoff (in milliseconds)
     */
    private static final long RETRY_DELAY_MS = 3000; // 3 seconds

    /**
     * Main consumer method for document processing
     *
     * Consumes DocumentUploaded events from the "document-processing" topic
     * and processes them through the full pipeline.
     *
     * @param record Kafka consumer record containing the event
     * @param acknowledgment Manual acknowledgment for message commit
     */
    @KafkaListener(
        topics = "document-processing",
        groupId = "document-processing-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeDocumentUpload(
            ConsumerRecord<String, String> record,
            Acknowledgment acknowledgment) {

        log.info("====================================");
        log.info("DocumentProcessingConsumer 收到消息");
        log.info("Topic: {}", record.topic());
        log.info("Partition: {}", record.partition());
        log.info("Offset: {}", record.offset());
        log.info("Key: {}", record.key());
        log.info("Value: {}", record.value());
        log.info("====================================");

        Long documentId = null;
        int retryCount = 0;

        try {
            // Parse the event message
            DocumentUploaded event = parseEvent(record.value());
            documentId = event.getDocumentId();

            log.info("开始处理文档: documentId={}, title={}", documentId, event.getTitle());

            // Process the document with retry logic
            processDocumentWithRetry(documentId, retryCount);

            // Acknowledge the message
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
                log.info("消息已确认: documentId={}", documentId);
            }

        } catch (Exception e) {
            log.error("文档处理失败: documentId={}, error={}",
                documentId, e.getMessage(), e);

            // Update document status to FAILED
            if (documentId != null) {
                updateDocumentStatus(documentId, Document.DocumentStatus.FAILED);
            }

            // Don't acknowledge - let the error handler send to DLQ
            // The DefaultErrorHandler in KafkaConfig will handle retries and DLQ
        }
    }

    /**
     * Process document with retry logic
     *
     * Implements exponential backoff retry mechanism.
     * Will retry up to MAX_RETRY_ATTEMPTS times before failing.
     *
     * @param documentId Document ID to process
     * @param retryCount Current retry count
     * @throws Exception if all retries are exhausted
     */
    @Transactional
    public void processDocumentWithRetry(Long documentId, int retryCount) throws Exception {
        while (retryCount <= MAX_RETRY_ATTEMPTS) {
            try {
                processDocument(documentId);
                return; // Success - exit retry loop
            } catch (Exception e) {
                retryCount++;
                log.warn("文档处理失败 (尝试 {}/{}): documentId={}, error={}",
                    retryCount, MAX_RETRY_ATTEMPTS, documentId, e.getMessage());

                if (retryCount > MAX_RETRY_ATTEMPTS) {
                    log.error("达到最大重试次数，文档处理失败: documentId={}", documentId);
                    throw e;
                }

                // Exponential backoff: 3s, 6s, 12s
                long delay = RETRY_DELAY_MS * (1L << (retryCount - 1));
                log.info("等待 {}ms 后重试...", delay);
                Thread.sleep(delay);
            }
        }
    }

    /**
     * Main document processing pipeline
     *
     * Orchestrates the complete document processing flow:
     * 1. Parse document content from file
     * 2. Split content into chunks
     * 3. Generate embeddings for each chunk
     * 4. Index chunks in Elasticsearch
     * 5. Update document status to INDEXED
     *
     * @param documentId Document ID to process
     * @throws Exception if any step fails
     */
    @Transactional
    public void processDocument(Long documentId) throws Exception {
        log.info("开始文档处理流程: documentId={}", documentId);

        // Step 1: Load document from database
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("文档不存在: " + documentId));

        log.info("文档加载成功: documentId={}, title={}, status={}",
            document.getId(), document.getTitle(), document.getStatus());

        // Step 2: Update status to PROCESSING
        updateDocumentStatus(documentId, Document.DocumentStatus.PROCESSING);
        log.info("文档状态更新为 PROCESSING: documentId={}", documentId);

        // Step 3: Parse document content
        String content = parseDocument(document);
        log.info("文档解析完成: documentId={}, contentLength={}",
            documentId, content.length());

        // Step 4: Split content into chunks
        List<DocumentChunk> chunks = chunkDocument(content, document);
        log.info("文档分块完成: documentId={}, chunkCount={}",
            documentId, chunks.size());

        // Step 5: Save chunks to database
        chunks = documentChunkRepository.saveAll(chunks);
        log.info("文档块已保存到数据库: documentId={}, chunkCount={}",
            documentId, chunks.size());

        // Step 6: Generate embeddings for chunks
        embedChunks(chunks);
        log.info("向量生成完成: documentId={}, chunkCount={}",
            documentId, chunks.size());

        // Step 7: Save updated chunks with embeddings
        documentChunkRepository.saveAll(chunks);
        log.info("向量已保存到数据库: documentId={}", documentId);

        // Step 8: Index chunks in Elasticsearch
        indexChunks(chunks, document);
        log.info("文档已索引到Elasticsearch: documentId={}", documentId);

        // Step 9: Update status to INDEXED
        updateDocumentStatus(documentId, Document.DocumentStatus.INDEXED);
        document.setIndexedAt(LocalDateTime.now());
        documentRepository.save(document);

        log.info("文档处理完成: documentId={}, status=INDEXED", documentId);
    }

    /**
     * Parse document content from file
     *
     * Uses DocumentParserService to extract text from various file formats
     * including PDF, DOCX, TXT, and Markdown.
     *
     * @param document Document entity
     * @return Extracted text content
     * @throws Exception if parsing fails
     */
    private String parseDocument(Document document) throws Exception {
        log.debug("开始解析文档: documentId={}, fileName={}, fileType={}",
            document.getId(), document.getFileName(), document.getFileType());

        File tempFile = null;
        try {
            // 获取storagePath并解析bucketName和objectName
            String storagePath = document.getStoragePath();
            if (storagePath == null || storagePath.isEmpty()) {
                throw new IllegalStateException("文档存储路径为空: " + document.getId());
            }

            // storagePath格式: "bucketName/objectName"
            String[] parts = storagePath.split("/", 2);
            if (parts.length != 2) {
                throw new IllegalStateException("文档存储路径格式错误: " + storagePath);
            }

            String bucketName = parts[0];
            String objectName = parts[1];

            // 从MinIO下载文件到临时目录
            InputStream inputStream = minioStorageService.downloadFile(bucketName, objectName);

            // 创建临时文件
            tempFile = File.createTempFile("doc_", "_" + document.getFileName());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                IOUtils.copy(inputStream, fos);
            }

            log.debug("文件已下载到临时文件: tempFile={}", tempFile.getAbsolutePath());

            // 解析文件
            String content = documentParserService.parse(tempFile.getAbsolutePath());

            if (content == null || content.trim().isEmpty()) {
                throw new IllegalStateException("文档内容为空: " + document.getFileName());
            }

            log.debug("文档解析成功: documentId={}, contentLength={}",
                document.getId(), content.length());

            return content.trim();

        } catch (Exception e) {
            log.error("文档解析失败: documentId={}, fileName={}, error={}",
                document.getId(), document.getFileName(), e.getMessage(), e);
            throw new Exception("文档解析失败: " + e.getMessage(), e);
        } finally {
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                log.debug("临时文件清理: tempFile={}, deleted={}", tempFile.getAbsolutePath(), deleted);
            }
        }
    }

    /**
     * Split document content into chunks
     *
     * Uses DocumentChunker to split the content into manageable chunks
     * with overlap for better context preservation.
     *
     * @param content Document text content
     * @param document Document entity
     * @return List of document chunks
     */
    private List<DocumentChunk> chunkDocument(String content, Document document) {
        log.debug("开始文档分块: documentId={}, contentLength={}",
            document.getId(), content.length());

        List<DocumentChunk> chunks = documentChunker.chunkDocument(content, document);

        log.debug("文档分块完成: documentId={}, chunkCount={}",
            document.getId(), chunks.size());

        return chunks;
    }

    /**
     * Generate embeddings for document chunks
     *
     * Uses the EmbeddingProvider to generate vector embeddings for each chunk.
     * Implements batch processing for better performance.
     *
     * @param chunks List of document chunks
     * @throws Exception if embedding generation fails
     */
    private void embedChunks(List<DocumentChunk> chunks) throws Exception {
        if (chunks == null || chunks.isEmpty()) {
            log.warn("文档块列表为空，跳过向量化");
            return;
        }

        log.info("开始向量生成: chunkCount={}", chunks.size());

        try {
            // Extract text content from chunks
            List<String> texts = new ArrayList<>();
            for (DocumentChunk chunk : chunks) {
                texts.add(chunk.getTextContent());
            }

            // Generate embeddings in batch (up to 10 at a time)
            List<float[]> embeddings = embeddingProvider.generateEmbeddingsBatch(texts);

            if (embeddings.size() != chunks.size()) {
                throw new IllegalStateException(
                    "向量数量不匹配: chunks=" + chunks.size() + ", embeddings=" + embeddings.size());
            }

            // Assign embeddings to chunks
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = chunks.get(i);
                float[] embedding = embeddings.get(i);
                chunk.setEmbeddingFromFloatArray(embedding);
                chunk.setEmbeddingModel(embeddingProvider.getProviderName());
                chunk.setVectorDimensions(embedding.length);
            }

            log.info("向量生成完成: chunkCount={}, dimensions={}",
                chunks.size(), embeddings.get(0).length);

        } catch (Exception e) {
            log.error("向量生成失败: chunkCount={}, error={}",
                chunks.size(), e.getMessage(), e);
            throw new Exception("向量生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * Index document chunks in Elasticsearch
     *
     * Converts domain DocumentChunk entities to Elasticsearch DocumentChunk
     * entities and indexes them using the ElasticsearchService.
     *
     * @param chunks List of domain document chunks
     * @param document Document entity
     * @throws Exception if indexing fails
     */
    private void indexChunks(List<DocumentChunk> chunks, Document document) throws Exception {
        if (chunks == null || chunks.isEmpty()) {
            log.warn("文档块列表为空，跳过索引");
            return;
        }

        log.info("开始索引文档块: chunkCount={}", chunks.size());

        try {
            // Convert domain chunks to Elasticsearch chunks
            List<com.company.kb.infra.elasticsearch.DocumentChunk> esChunks = new ArrayList<>();

            for (DocumentChunk chunk : chunks) {
                com.company.kb.infra.elasticsearch.DocumentChunk esChunk =
                    com.company.kb.infra.elasticsearch.DocumentChunk.builder()
                        .chunkId(chunk.getId())
                        .documentId(chunk.getDocumentId())
                        .content(chunk.getTextContent())
                        .contentVector(chunk.getEmbeddingAsFloatArray())
                        .title(document.getTitle())
                        .fileName(document.getFileName())
                        .fileType(document.getFileType())
                        .tags(document.getTags())
                        .isPublic(document.getIsPublic())
                        .position(chunk.getChunkIndex())
                        .createdAt(chunk.getCreatedAt() != null ?
                            chunk.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000 : null)
                        .build();

                esChunks.add(esChunk);
            }

            // Bulk index in Elasticsearch
            elasticsearchService.bulkIndexChunks(esChunks);

            log.info("文档块索引完成: chunkCount={}", esChunks.size());

        } catch (Exception e) {
            log.error("文档块索引失败: documentId={}, chunkCount={}, error={}",
                document.getId(), chunks.size(), e.getMessage(), e);
            throw new Exception("文档块索引失败: " + e.getMessage(), e);
        }
    }

    /**
     * Update document status
     *
     * Updates the document status in the database.
     * Used to track processing progress.
     *
     * @param documentId Document ID
     * @param status New status
     */
    @Transactional
    public void updateDocumentStatus(Long documentId, Document.DocumentStatus status) {
        try {
            Document document = documentRepository.findById(documentId).orElse(null);
            if (document != null) {
                document.setStatus(status);
                documentRepository.save(document);
                log.debug("文档状态已更新: documentId={}, status={}", documentId, status);
            } else {
                log.warn("文档不存在，无法更新状态: documentId={}", documentId);
            }
        } catch (Exception e) {
            log.error("更新文档状态失败: documentId={}, status={}, error={}",
                documentId, status, e.getMessage(), e);
        }
    }

    /**
     * Parse DocumentUploaded event from JSON string
     *
     * Handles both simple documentId strings and full JSON event objects.
     *
     * @param value JSON string or simple documentId
     * @return Parsed DocumentUploaded event
     * @throws Exception if parsing fails
     */
    private DocumentUploaded parseEvent(String value) throws Exception {
        try {
            // Try to parse as full JSON event first
            return objectMapper.readValue(value, DocumentUploaded.class);
        } catch (Exception e) {
            // Fallback: treat as simple documentId string
            try {
                Long documentId = Long.parseLong(value.trim());
                log.debug("解析为简单documentId: {}", documentId);
                return DocumentUploaded.from(documentId);
            } catch (NumberFormatException ex) {
                log.error("无法解析事件消息: {}", value);
                throw new Exception("无效的事件消息格式: " + value, ex);
            }
        }
    }
}
