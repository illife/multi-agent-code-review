package com.company.kb.core.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 教学文档实体
 * 存储根据测试结果和知识点生成的教学文档
 */
@Entity
@Table(name = "teaching_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeachingDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 文档标题
     */
    @Column(nullable = false, length = 255)
    private String title;

    /**
     * 关联的用户ID
     */
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    /**
     * 文档类型: LESSON(课时), PRACTICE(练习), REVIEW(复习), KNOWLEDGE_GAP(知识缺口)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    /**
     * 关联的知识点ID列表（JSON格式）
     */
    @Column(name = "knowledge_point_ids", columnDefinition = "TEXT")
    private String knowledgePointIds;

    /**
     * 关联的测试结果ID
     */
    @Column(name = "test_result_id")
    private Long testResultId;

    /**
     * Markdown格式的教学内容
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * 教学文档元数据（JSON格式）
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * 文档状态: DRAFT(草稿), PUBLISHED(已发布), ARCHIVED(已归档)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DocumentStatus status;

    /**
     * 优先级: 1(高), 2(中), 3(低)
     */
    @Column(name = "priority", nullable = false)
    private Integer priority = 2;

    /**
     * 标签（JSON格式）
     */
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 文档类型枚举
     */
    public enum DocumentType {
        LESSON,        // 课时文档
        PRACTICE,      // 练习文档
        REVIEW,        // 复习文档
        KNOWLEDGE_GAP, // 知识缺口分析
        CUSTOM         // 自定义
    }

    /**
     * 文档状态枚举
     */
    public enum DocumentStatus {
        DRAFT,     // 草稿
        PUBLISHED, // 已发布
        ARCHIVED   // 已归档
    }
}
