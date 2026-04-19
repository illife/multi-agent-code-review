package com.company.kb.core.repository;

import com.company.kb.core.domain.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档Repository
 *
 * @author Knowledge Base Team
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * 根据上传用户查询文档
     */
    Page<Document> findByUploadedBy(String uploadedBy, Pageable pageable);

    /**
     * 搜索文档（标题或描述）
     */
    Page<Document> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description, Pageable pageable);

    /**
     * 根据状态查询文档
     */
    Page<Document> findByStatus(Document.DocumentStatus status, Pageable pageable);

    /**
     * 查询公开文档
     */
    @Query("SELECT d FROM Document d WHERE d.isPublic = true")
    Page<Document> findPublicDocuments(Pageable pageable);

    /**
     * 搜索文档（标题或描述）
     */
    @Query("SELECT d FROM Document d WHERE d.title LIKE %:keyword% OR d.description LIKE %:keyword%")
    Page<Document> searchDocuments(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 根据标签查询文档
     */
    @Query(value = "SELECT * FROM documents WHERE :tag = ANY(tags)", nativeQuery = true)
    List<Document> findByTag(@Param("tag") String tag);

    /**
     * 根据用户、MD5和状态查询文档（用于秒传）
     */
    List<Document> findByUploadedByAndFileMd5AndStatus(
        String uploadedBy,
        String fileMd5,
        Document.DocumentStatus status
    );

    /**
     * 根据用户和MD5查询文档（用于秒传检查）
     */
    java.util.Optional<Document> findByFileMd5AndUploadedBy(String fileMd5, String uploadedBy);

    /**
     * 获取所有公开文档的ID（用于权限查询）
     */
    @Query("SELECT d.id FROM Document d WHERE d.isPublic = true")
    List<Long> findPublicDocumentIds();
}
