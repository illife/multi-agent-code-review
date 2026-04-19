package com.company.kb.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 文档实体
 *
 * Simplified document entity for the multi-agent platform.
 * User management is delegated to the Auth service.
 *
 * @author Knowledge Base Team
 */
@Entity
@Table(name = "documents")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @EqualsAndHashCode.Include
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(length = 500)
    private String filePath;

    @Column(nullable = false, length = 50)
    private String fileType;

    private Long fileSize;

    @Column(name = "uploaded_by", nullable = false, length = 255)
    private String uploadedBy;

    @Column(columnDefinition = "VARCHAR(500)[]")
    private String[] tags;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.PROCESSING;

    @Column
    private LocalDateTime indexedAt;

    @Column(name = "storage_path", length = 500)
    private String storagePath;

    @Column(name = "file_md5", length = 32)
    private String fileMd5;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 文档状态枚举
     */
    public enum DocumentStatus {
        UPLOADED,    // 已上传
        PROCESSING,  // 处理中
        INDEXED,     // 已索引
        FAILED       // 失败
    }
}
