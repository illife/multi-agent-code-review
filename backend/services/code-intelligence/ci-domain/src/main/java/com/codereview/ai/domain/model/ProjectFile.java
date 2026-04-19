package com.codereview.ai.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ProjectFile entity
 * Maps individual files within a project to their code reviews
 *
 * @author Code Review AI Team
 */
@Entity
@Table(name = "project_files", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"project_id", "file_path"})
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProjectFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    @JsonIgnore
    private Project project;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", insertable = false, updatable = false)
    @JsonIgnore
    private CodeReview review;

    @Column(name = "review_id")
    private Long reviewId;

    @Column(name = "file_path", length = 500, nullable = false)
    private String filePath;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(length = 50)
    private String language;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "line_count")
    private Integer lineCount;

    @Column(name = "is_analyzed")
    @Builder.Default
    private Boolean isAnalyzed = false;

    @Column(name = "analysis_priority")
    @Builder.Default
    private Integer analysisPriority = 0;

    @Column(name = "minio_path", length = 500)
    private String minioPath;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
