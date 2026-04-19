package com.codereview.ai.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * User Progress entity
 * Tracks user learning progress for specific content
 *
 * @author Code Review AI Team
 */
@Entity
@Table(name = "user_progress", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "learning_path_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_path_id", nullable = false, insertable = false, updatable = false)
    @JsonIgnore
    private LearningPath learningPath;

    @Column(name = "learning_path_id", nullable = false)
    private Long learningPathId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProgressStatus status = ProgressStatus.NOT_STARTED;

    @Column
    @Builder.Default
    private Integer progressPercent = 0;

    @Column
    @Builder.Default
    private Integer currentSection = 1;

    @Column
    private Integer score;

    @Column
    private Integer maxScore;

    @Column
    @Builder.Default
    private Integer timeSpentMinutes = 0;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    @Column
    private LocalDateTime lastAccessedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Progress status enum
     */
    public enum ProgressStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED
    }
}
