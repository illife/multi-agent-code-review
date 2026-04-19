package com.company.kb.controller;

import com.company.kb.api.dto.PageResponse;
import com.company.kb.core.domain.Document;
import com.company.kb.core.domain.DocumentChunk;
import com.company.kb.core.service.DocumentService;
import com.think.platform.shared.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 文档控制器
 * 提供文档上传、管理、搜索等API接口
 */
@Slf4j
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 上传文档
     * @param file 文件
     * @param title 标题
     * @param description 描述
     * @return 文档信息
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "fileMd5", required = false) String fileMd5,
            Authentication authentication) {

        try {
            String username = authentication.getName();  // ✅ 正确：这是username
            log.info("收到文档上传请求: username={}, fileName={}, fileMd5={}",
                username, file.getOriginalFilename(), fileMd5);

            // 1. 检查是否为秒传（文件MD5已存在）
            if (fileMd5 != null && !fileMd5.isEmpty()) {
                Document existingDocument = documentService.findExistingDocument(fileMd5, username);
                if (existingDocument != null) {
                    log.info("文件已存在，秒传成功: documentId={}, fileName={}",
                        existingDocument.getId(), existingDocument.getFileName());

                    Map<String, Object> result = new HashMap<>();
                    result.put("id", existingDocument.getId());
                    result.put("fileName", existingDocument.getFileName());
                    result.put("title", existingDocument.getTitle());
                    result.put("status", existingDocument.getStatus());
                    result.put("alreadyExists", true);
                    result.put("message", "文件已存在，秒传成功");

                    return Result.success(result);
                }
            }

            // 2. 创建文档记录（状态：PROCESSING）
            Document document = documentService.uploadDocument(file, title, description, username, fileMd5);

            // 2. 授予上传者所有者权限（在独立事务中）
            documentService.grantDocumentPermissionAfterUpload(document.getId(), username);

            // 3. 发送到Kafka异步处理（转换为String）
            kafkaTemplate.send("document-processing", document.getId().toString());

            log.info("文档已提交处理: documentId={}", document.getId());

            // 4. 返回文档信息
            Map<String, Object> result = new HashMap<>();
            result.put("id", document.getId());
            result.put("fileName", document.getFileName());
            result.put("title", document.getTitle());
            result.put("status", document.getStatus());
            result.put("message", "文档已提交，正在后台处理...");

            return Result.success(result);

        } catch (Exception e) {
            log.error("文档上传失败", e);
            return Result.failed(500, "文档上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取文档详情
     * @param documentId 文档ID
     * @return 文档详情
     */
    @GetMapping("/{documentId}")
    public Result<Document> getDocument(@PathVariable Long documentId) {
        try {
            Document document = documentService.getDocumentById(documentId);
            if (document == null) {
                return Result.failed(404, "文档不存在");
            }
            return Result.success(document);
        } catch (Exception e) {
            log.error("获取文档失败: documentId={}", documentId, e);
            return Result.failed(500, "获取文档失败: " + e.getMessage());
        }
    }

    /**
     * 获取文档的分块内容
     * @param documentId 文档ID
     * @param page 页码
     * @param size 每页大小
     * @return 文档块列表
     */
    @GetMapping("/{documentId}/chunks")
    public Result<PageResponse<DocumentChunk>> getDocumentChunks(
            @PathVariable Long documentId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "chunkIndex"));
            Page<DocumentChunk> chunks = documentService.getDocumentChunks(documentId, pageable);
            return Result.success(PageResponse.of(chunks));
        } catch (Exception e) {
            log.error("获取文档块失败: documentId={}", documentId, e);
            return Result.failed(500, "获取文档块失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的文档列表
     * @param page 页码
     * @param size 每页大小
     * @return 文档列表
     */
    @GetMapping
    public Result<PageResponse<Document>> getDocuments(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Document> documents = documentService.getUserDocuments(username, pageable);
            return Result.success(PageResponse.of(documents));
        } catch (Exception e) {
            log.error("获取文档列表失败", e);
            return Result.failed(500, "获取文档列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的文档列表（别名）
     * @param page 页码
     * @param size 每页大小
     * @return 文档列表
     */
    @GetMapping("/my")
    public Result<PageResponse<Document>> getMyDocuments(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Document> documents = documentService.getUserDocuments(username, pageable);
            return Result.success(PageResponse.of(documents));
        } catch (Exception e) {
            log.error("获取文档列表失败", e);
            return Result.failed(500, "获取文档列表失败: " + e.getMessage());
        }
    }

    /**
     * 搜索文档
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页大小
     * @return 文档列表
     */
    @GetMapping("/search")
    public Result<PageResponse<Document>> searchDocuments(
            @RequestParam(value = "keyword") String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Document> documents = documentService.searchDocuments(keyword, pageable);
            return Result.success(PageResponse.of(documents));
        } catch (Exception e) {
            log.error("搜索文档失败: keyword={}", keyword, e);
            return Result.failed(500, "搜索文档失败: " + e.getMessage());
        }
    }

    /**
     * 删除文档
     * @param documentId 文档ID
     * @return 删除结果
     */
    @DeleteMapping("/{documentId}")
    public Result<String> deleteDocument(@PathVariable Long documentId) {
        try {
            documentService.deleteDocument(documentId);
            return Result.success("文档删除成功");
        } catch (Exception e) {
            log.error("删除文档失败: documentId={}", documentId, e);
            return Result.failed(500, "删除文档失败: " + e.getMessage());
        }
    }

    /**
     * 更新文档状态
     * @param documentId 文档ID
     * @param status 状态
     * @return 更新结果
     */
    @PutMapping("/{documentId}/status")
    public Result<String> updateDocumentStatus(
            @PathVariable Long documentId,
            @RequestParam(value = "status") Document.DocumentStatus status) {

        try {
            documentService.updateDocumentStatus(documentId, status);
            return Result.success("文档状态更新成功");
        } catch (Exception e) {
            log.error("更新文档状态失败: documentId={}, status={}", documentId, status, e);
            return Result.failed(500, "更新文档状态失败: " + e.getMessage());
        }
    }

    /**
     * 下载文档
     * @param documentId 文档ID
     * @return 文件内容
     */
    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long documentId) {
        try {
            log.info("下载文档请求: documentId={}", documentId);

            DocumentService.FileDownloadResult downloadResult = documentService.downloadDocument(documentId);

            // URL编码文件名
            String encodedFileName = URLEncoder.encode(downloadResult.getFileName(), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + downloadResult.getFileName() + "\"; " +
                            "filename*=UTF-8''" + encodedFileName)
                    .body(downloadResult.getResource());

        } catch (Exception e) {
            log.error("下载文档失败: documentId={}", documentId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 预览文档（支持PDF等浏览器可直接显示的文件）
     * @param documentId 文档ID
     * @return 文件内容
     */
    @GetMapping("/{documentId}/preview")
    public ResponseEntity<Resource> previewDocument(@PathVariable Long documentId) {
        try {
            log.info("预览文档请求: documentId={}", documentId);

            DocumentService.FileDownloadResult downloadResult = documentService.downloadDocument(documentId);

            // 根据文件类型确定Content-Type
            MediaType mediaType = determineMediaType(downloadResult.getFileName());

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(downloadResult.getResource());

        } catch (Exception e) {
            log.error("预览文档失败: documentId={}", documentId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取文档文本内容（用于文本文件预览）
     * @param documentId 文档ID
     * @return 文本内容
     */
    @GetMapping("/{documentId}/content")
    public Result<Map<String, Object>> getDocumentContent(@PathVariable Long documentId) {
        try {
            log.info("获取文档内容: documentId={}", documentId);

            Document document = documentService.getDocumentById(documentId);
            if (document == null) {
                return Result.failed(404, "文档不存在");
            }

            String content = documentService.getDocumentTextContent(documentId);

            Map<String, Object> result = new HashMap<>();
            result.put("documentId", documentId);
            result.put("fileName", document.getFileName());
            result.put("fileType", document.getFileType());
            result.put("content", content);
            result.put("contentLength", content != null ? content.length() : 0);

            return Result.success(result);

        } catch (Exception e) {
            log.error("获取文档内容失败: documentId={}", documentId, e);
            return Result.failed(500, "获取文档内容失败: " + e.getMessage());
        }
    }

    /**
     * 批量重新索引所有文档
     * 用于ES索引重建后的批量数据处理
     */
    @PostMapping("/reindex")
    public Result<Map<String, Object>> reindexAllDocuments() {
        try {
            log.info("开始批量重新索引所有文档");

            // 获取所有文档（使用无限制的分页）
            Pageable pageable = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Document> documentPage = documentService.getAllDocuments(pageable);
            java.util.List<Document> documents = documentPage.getContent();

            int successCount = 0;
            int failCount = 0;

            for (Document doc : documents) {
                try {
                    // 发送Kafka消息触发重新处理
                    kafkaTemplate.send("document-processing", doc.getId().toString());
                    successCount++;
                    log.info("已发送重新索引请求: documentId={}, title={}", doc.getId(), doc.getTitle());
                } catch (Exception e) {
                    failCount++;
                    log.error("发送重新索引请求失败: documentId={}", doc.getId(), e);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("totalDocuments", documents.size());
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("message", String.format("已发送%d个文档重新索引请求", successCount));

            log.info("批量重新索引完成: total={}, success={}, fail={}", documents.size(), successCount, failCount);
            return Result.success(result);

        } catch (Exception e) {
            log.error("批量重新索引失败", e);
            return Result.failed(500, "批量重新索引失败: " + e.getMessage());
        }
    }

    /**
     * 重新索引单个文档
     */
    @PostMapping("/{documentId}/reindex")
    public Result<String> reindexDocument(@PathVariable Long documentId) {
        try {
            log.info("开始重新索引文档: documentId={}", documentId);

            Document document = documentService.getDocumentById(documentId);
            if (document == null) {
                return Result.failed(404, "文档不存在");
            }

            // 发送Kafka消息触发重新处理
            kafkaTemplate.send("document-processing", documentId.toString());

            log.info("已发送重新索引请求: documentId={}, title={}", documentId, document.getTitle());
            return Result.success("文档重新索引请求已发送");

        } catch (Exception e) {
            log.error("重新索引文档失败: documentId={}", documentId, e);
            return Result.failed(500, "重新索引文档失败: " + e.getMessage());
        }
    }

    /**
     * 根据文件扩展名确定MediaType
     */
    private MediaType determineMediaType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "txt" -> MediaType.TEXT_PLAIN;
            case "html", "htm" -> MediaType.TEXT_HTML;
            case "xml" -> MediaType.APPLICATION_XML;
            case "json" -> MediaType.APPLICATION_JSON;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
