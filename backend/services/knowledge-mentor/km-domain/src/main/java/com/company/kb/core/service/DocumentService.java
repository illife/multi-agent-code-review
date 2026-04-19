package com.company.kb.core.service;

import com.company.kb.core.domain.Document;
import com.company.kb.core.domain.DocumentChunk;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档服务接口
 */
public interface DocumentService {

    /**
     * 上传文档
     * @param file 文件
     * @param title 标题
     * @param description 描述
     * @param username 用户名
     * @param fileMd5 文件MD5（用于秒传检查）
     * @return 文档
     */
    Document uploadDocument(MultipartFile file, String title, String description, String username, String fileMd5) throws Exception;

    /**
     * 查找已存在的文档（用于秒传）
     * @param fileMd5 文件MD5
     * @param username 用户名
     * @return 已存在的文档，如果不存在返回null
     */
    Document findExistingDocument(String fileMd5, String username);

    /**
     * 文档上传后授予权限
     * @param documentId 文档ID
     * @param username 用户名
     */
    void grantDocumentPermissionAfterUpload(Long documentId, String username);

    /**
     * 解析文档
     * @param document 文档
     * @return 文档内容
     */
    String parseDocument(Document document) throws Exception;

    /**
     * 分块处理文档
     * @param content 内容
     * @param document 文档
     * @return 文档块列表
     */
    List<DocumentChunk> chunkDocument(String content, Document document);

    /**
     * 索引文档
     * @param document 文档
     * @param chunks 文档块
     */
    void indexDocument(Document document, List<DocumentChunk> chunks) throws Exception;

    /**
     * 删除文档
     * @param documentId 文档ID
     */
    void deleteDocument(Long documentId) throws Exception;

    /**
     * 获取文档
     * @param documentId 文档ID
     * @return 文档
     */
    Document getDocumentById(Long documentId);

    /**
     * 搜索文档
     * @param keyword 关键词
     * @param pageable 分页
     * @return 文档列表
     */
    Page<Document> searchDocuments(String keyword, Pageable pageable);

    /**
     * 获取用户的文档列表
     * @param userId 用户ID
     * @param pageable 分页
     * @return 文档列表
     */
    Page<Document> getUserDocuments(String userId, Pageable pageable);

    /**
     * 更新文档状态
     * @param documentId 文档ID
     * @param status 状态
     */
    void updateDocumentStatus(Long documentId, Document.DocumentStatus status);

    /**
     * 下载文档
     * @param documentId 文档ID
     * @return 下载结果（包含文件名和资源）
     * @throws Exception 下载失败时抛出异常
     */
    FileDownloadResult downloadDocument(Long documentId) throws Exception;

    /**
     * 获取文档的文本内容
     * @param documentId 文档ID
     * @return 文本内容
     * @throws Exception 获取失败时抛出异常
     */
    String getDocumentTextContent(Long documentId) throws Exception;

    /**
     * 获取所有文档（用于批量重新索引）
     * @param pageable 分页
     * @return 文档列表
     */
    Page<Document> getAllDocuments(Pageable pageable);

    /**
     * 获取文档的分块内容
     * @param documentId 文档ID
     * @param pageable 分页
     * @return 文档块分页
     */
    Page<DocumentChunk> getDocumentChunks(Long documentId, Pageable pageable);

    /**
     * 文件下载结果
     */
    class FileDownloadResult {
        private final String fileName;
        private final Resource resource;

        public FileDownloadResult(String fileName, Resource resource) {
            this.fileName = fileName;
            this.resource = resource;
        }

        public String getFileName() {
            return fileName;
        }

        public Resource getResource() {
            return resource;
        }
    }
}
