package com.codereview.ai.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Code issue entity
 *
 * @author Code Review AI Team
 */
@Entity
@Table(name = "code_issues")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", insertable = false, updatable = false)
    @JsonIgnore
    private CodeReview review;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Severity severity;

    @Column(length = 50)
    private String category;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String codeSnippet;

    @Column
    private Integer lineNumber;

    @Column(columnDefinition = "TEXT")
    private String suggestion;

    @Column(length = 50)
    private String agentType;

    @Column(length = 100)
    private String toolName;

    @Column(length = 100)
    private String ruleId;

    @Column(columnDefinition = "TEXT")
    private String teachingExplanation;

    @Column(name = "related_lesson_id")
    private Long relatedLessonId;

    @Column
    @Builder.Default
    private Boolean isResolved = false;

    @Column(columnDefinition = "TEXT")
    private String metadata;  // JSON metadata for additional info

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum Severity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW,
        INFO
    }
}
