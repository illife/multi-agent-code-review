package com.company.kb.core.repository;

import com.company.kb.core.domain.TeachingDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 教学文档 Repository
 */
@Repository
public interface TeachingDocumentRepository extends JpaRepository<TeachingDocument, Long> {

    /**
     * 根据用户ID查询教学文档
     */
    List<TeachingDocument> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * 根据用户ID和文档类型查询
     */
    List<TeachingDocument> findByUserIdAndDocumentTypeOrderByCreatedAtDesc(
        String userId, TeachingDocument.DocumentType documentType
    );

    /**
     * 根据用户ID和状态查询
     */
    List<TeachingDocument> findByUserIdAndStatusOrderByCreatedAtDesc(
        String userId, TeachingDocument.DocumentStatus status
    );

    /**
     * 根据测试结果ID查询
     */
    List<TeachingDocument> findByTestResultId(Long testResultId);

    /**
     * 查询用户已发布的教学文档
     */
    @Query("SELECT t FROM TeachingDocument t WHERE t.userId = :userId " +
           "AND t.status = 'PUBLISHED' ORDER BY t.createdAt DESC")
    List<TeachingDocument> findPublishedByUserId(@Param("userId") String userId);

    /**
     * 根据关键词搜索教学文档
     */
    @Query("SELECT t FROM TeachingDocument t WHERE t.userId = :userId " +
           "AND (t.title LIKE %:keyword% OR t.content LIKE %:keyword%) " +
           "ORDER BY t.createdAt DESC")
    List<TeachingDocument> searchByKeyword(@Param("userId") String userId, @Param("keyword") String keyword);
}
