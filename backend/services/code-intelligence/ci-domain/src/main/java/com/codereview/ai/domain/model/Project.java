package com.codereview.ai.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Project entity for multi-file code review
 * Represents a complete project uploaded for batch analysis
 *
 * @author Code Review AI Team
 */
@Entity
@Table(name = "projects")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @JsonIgnore
    private User user;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "project_name", length = 255, nullable = false)
    private String projectName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_type", length = 20, nullable = false)
    private UploadType uploadType;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "storage_path", length = 500)
    private String storagePath;

    @Column(name = "total_files")
    @Builder.Default
    private Integer totalFiles = 0;

    @Column(name = "total_size")
    private Long totalSize;

    @Column(name = "language", length = 50)
    private String language;

    @Column(name = "analyzed_files")
    @Builder.Default
    private Integer analyzedFiles = 0;

    @Column(name = "total_issues")
    @Builder.Default
    private Integer totalIssues = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private ProjectVisibility visibility = ProjectVisibility.PRIVATE;

    @Column(name = "file_filter_config")
    private String fileFilterConfig;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Upload type enum
     * Defines how the project was uploaded
     */
    public enum UploadType {
        ZIP,
        MULTIFILE,
        GIT
    }

    /**
     * Project status enum
     * Defines the current state of project analysis
     */
    public enum ProjectStatus {
        PENDING,
        ANALYZING,
        COMPLETED,
        FAILED
    }

    /**
     * Project visibility enum
     * Defines who can access the project
     */
    public enum ProjectVisibility {
        PRIVATE,
        PUBLIC,
        TEAM
    }
}
