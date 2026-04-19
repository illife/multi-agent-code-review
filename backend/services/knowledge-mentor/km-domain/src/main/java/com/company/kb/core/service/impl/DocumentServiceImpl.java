package com.company.kb.core.service.impl;

import com.company.kb.core.domain.Document;
import com.company.kb.core.domain.DocumentChunk;
import com.company.kb.core.repository.DocumentRepository;
import com.company.kb.core.repository.DocumentChunkRepository;
import com.company.kb.core.service.DocumentChunker;
import com.company.kb.core.service.DocumentService;
import com.company.kb.infra.elasticsearch.service.ElasticsearchService;
import com.think.platform.shared.infra.minio.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Simplified document service implementation
 * Removed user permission management - moved to Auth service
 * Infrastructure dependencies removed - should be handled by separate integration layer
 *
 * @author Knowledge Base Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunker documentChunker;
    private final DocumentChunkRepository documentChunkRepository;
    private final MinioStorageService minioStorageService;
    private final ElasticsearchService elasticsearchService;
    private final org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 默认bucket名称
     */
    private static final String DEFAULT_BUCKET_NAME = "knowledge-base-documents";

    @Override
    @Transactional
    public Document uploadDocument(MultipartFile file, String title, String description, String username, String fileMd5) throws Exception {
        log.info("Uploading document: title={}, user={}", title, username);

        // Check if already exists (instant upload)
        Document existingDocument = findExistingDocument(fileMd5, username);
        if (existingDocument != null) {
            log.info("Document already exists (instant upload): {}", fileMd5);
            return existingDocument;
        }

        // 如果 title 为空，使用文件名作为默认标题
        String finalTitle = title;
        if (finalTitle == null || finalTitle.trim().isEmpty()) {
            String originalFilename = file.getOriginalFilename();
            finalTitle = extractTitleFromFileName(originalFilename);
            log.info("使用文件名作为标题: {}", finalTitle);
        }

        // 创建文档记录（先保存以获取ID）
        Document document = Document.builder()
                .title(finalTitle)
                .description(description)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .fileMd5(fileMd5)
                .uploadedBy(username)
                .status(Document.DocumentStatus.UPLOADED)
                .build();

        document = documentRepository.save(document);

        // 生成唯一对象名并上传到MinIO
        String objectName = generateObjectName(document, file.getOriginalFilename());
        String storagePath = uploadToMinIO(objectName, file);

        // 更新storagePath
        document.setStoragePath(storagePath);
        document = documentRepository.save(document);

        // 发送Kafka消息触发异步处理
        try {
            kafkaTemplate.send("document-processing", document.getId().toString());
            log.info("Kafka消息已发送: documentId={}, topic=document-processing", document.getId());
        } catch (Exception e) {
            log.error("发送Kafka消息失败: documentId={}", document.getId(), e);
        }

        log.info("Document uploaded successfully: documentId={}", document.getId());

        return document;
    }

    /**
     * 从文件名提取标题（去除扩展名）
     * @param fileName 原始文件名
     * @return 提取的标题
     */
    private String extractTitleFromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "未命名文档";
        }

        // 去除文件扩展名
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }

        return fileName;
    }

    @Override
    public Document findExistingDocument(String fileMd5, String username) {
        return documentRepository.findByFileMd5AndUploadedBy(fileMd5, username)
                .orElse(null);
    }

    @Override
    public void grantDocumentPermissionAfterUpload(Long documentId, String username) {
        // Permission management moved to Auth service, this method is kept as empty implementation
        log.debug("Permission management moved to Auth service");
    }

    @Override
    public String parseDocument(Document document) throws Exception {
        // TODO: Implement document parsing via integration service
        log.warn("Document parsing not implemented yet for document: {}", document.getId());
        return "";
    }

    @Override
    public List<DocumentChunk> chunkDocument(String content, Document document) {
        return documentChunker.chunkDocument(content, document);
    }

    @Override
    @Transactional
    public void indexDocument(Document document, List<DocumentChunk> chunks) throws Exception {
        log.info("开始索引文档: documentId={}, chunkCount={}", document.getId(), chunks.size());

        // 转换为ES DocumentChunk并批量索引
        List<com.company.kb.infra.elasticsearch.DocumentChunk> esChunks = chunks.stream()
            .map(chunk -> convertToEsChunk(chunk, document))
            .collect(java.util.stream.Collectors.toList());

        elasticsearchService.bulkIndexChunks(esChunks);
        log.info("文档块已索引到Elasticsearch: documentId={}, chunkCount={}", document.getId(), esChunks.size());

        // 更新文档状态
        document.setStatus(Document.DocumentStatus.INDEXED);
        documentRepository.save(document);
    }

    /**
     * 转换DocumentChunk为Elasticsearch格式
     */
    private com.company.kb.infra.elasticsearch.DocumentChunk convertToEsChunk(
            DocumentChunk chunk, Document document) {
        return com.company.kb.infra.elasticsearch.DocumentChunk.builder()
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
    }

    @Override
    @Transactional
    public void deleteDocument(Long documentId) throws Exception {
        Document document = getDocumentById(documentId);
        log.info("开始删除文档: documentId={}", documentId);

        // 1. 从Elasticsearch删除文档块
        try {
            elasticsearchService.deleteDocumentChunks(documentId);
            log.info("已从Elasticsearch删除文档块: documentId={}", documentId);
        } catch (Exception e) {
            log.error("从Elasticsearch删除文档块失败: documentId={}", documentId, e);
            // 继续执行，不影响其他清理操作
        }

        // 2. 从MinIO删除文件
        if (document.getStoragePath() != null && !document.getStoragePath().isEmpty()) {
            try {
                String[] parts = document.getStoragePath().split("/", 2);
                if (parts.length == 2) {
                    String bucketName = parts[0];
                    String objectName = parts[1];
                    minioStorageService.deleteFile(bucketName, objectName);
                    log.info("已从MinIO删除文件: {}", document.getStoragePath());
                }
            } catch (Exception e) {
                log.error("从MinIO删除文件失败: storagePath={}", document.getStoragePath(), e);
                // 继续执行，不影响数据库删除
            }
        }

        // 3. 删除数据库记录
        documentRepository.delete(document);

        log.info("文档删除完成: documentId={}", documentId);
    }

    @Override
    public Document getDocumentById(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在: " + documentId));
    }

    @Override
    public Page<Document> searchDocuments(String keyword, Pageable pageable) {
        return documentRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                keyword, keyword, pageable);
    }

    @Override
    public Page<Document> getUserDocuments(String userId, Pageable pageable) {
        return documentRepository.findByUploadedBy(userId, pageable);
    }

    @Override
    @Transactional
    public void updateDocumentStatus(Long documentId, Document.DocumentStatus status) {
        Document document = getDocumentById(documentId);
        document.setStatus(status);
        documentRepository.save(document);
    }

    @Override
    public FileDownloadResult downloadDocument(Long documentId) throws Exception {
        Document document = getDocumentById(documentId);

        if (document.getStoragePath() == null || document.getStoragePath().isEmpty()) {
            throw new IllegalStateException("文档存储路径为空: " + documentId);
        }

        // storagePath格式: "bucketName/objectName"
        String[] parts = document.getStoragePath().split("/", 2);
        if (parts.length != 2) {
            throw new IllegalStateException("文档存储路径格式错误: " + document.getStoragePath());
        }

        String bucketName = parts[0];
        String objectName = parts[1];

        // 从MinIO下载文件
        InputStream inputStream = minioStorageService.downloadFile(bucketName, objectName);

        // 将InputStream读取到byte数组（避免多次读取问题）
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        byte[] fileBytes = buffer.toByteArray();

        // 使用ByteArrayResource，支持多次读取
        Resource resource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return document.getFileName();
            }
        };

        log.info("文件下载成功: documentId={}, fileName={}", documentId, document.getFileName());
        return new FileDownloadResult(document.getFileName(), resource);
    }

    @Override
    public String getDocumentTextContent(Long documentId) throws Exception {
        Document document = getDocumentById(documentId);
        // TODO: Parse and return text content
        throw new UnsupportedOperationException("Getting document text content not implemented yet");
    }

    @Override
    public Page<Document> getAllDocuments(Pageable pageable) {
        return documentRepository.findAll(pageable);
    }

    @Override
    public Page<DocumentChunk> getDocumentChunks(Long documentId, Pageable pageable) {
        return documentChunkRepository.findByDocumentId(documentId, pageable);
    }

    // Private helper methods

    private void processDocumentAsync(Document document) {
        // TODO: Send Kafka message for async processing
        log.info("TODO: Send Kafka message for async document processing: documentId={}", document.getId());
    }

    /**
     * 生成MinIO对象名
     * 格式: {userId}/{year}/{month}/{documentId}_{uuid}_{filename}
     */
    private String generateObjectName(Document document, String originalFilename) {
        String userId = document.getUploadedBy();
        LocalDate now = LocalDate.now();
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s/%d/%02d/%d_%s_%s",
            userId, now.getYear(), now.getMonthValue(),
            document.getId(), uuid, originalFilename);
    }

    /**
     * 上传文件到MinIO
     * 返回格式: bucketName/objectName
     */
    private String uploadToMinIO(String objectName, MultipartFile file) {
        try {
            minioStorageService.uploadFile(DEFAULT_BUCKET_NAME, objectName, file);
            String storagePath = DEFAULT_BUCKET_NAME + "/" + objectName;
            log.info("文件上传成功: {}", storagePath);
            return storagePath;
        } catch (Exception e) {
            log.error("文件上传失败: objectName={}", objectName, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }
}
