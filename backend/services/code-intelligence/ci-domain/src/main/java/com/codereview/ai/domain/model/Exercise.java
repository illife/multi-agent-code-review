package com.codereview.ai.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Exercise entity
 * Represents practice problems and coding exercises
 *
 * @author Code Review AI Team
 */
@Entity
@Table(name = "exercises")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id")
    private Long taskId;

    @Column(length = 255, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", length = 20, nullable = false)
    private Difficulty difficulty;

    @Column(name = "skill_tag", length = 100)
    private String skillTag;

    @Column(length = 50, nullable = false)
    private String language;

    @Column(length = 100)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String starterCode;

    @Column(columnDefinition = "TEXT")
    private String solutionCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private List<TestCase> testCases;

    @Column(columnDefinition = "TEXT[]")
    private List<String> hints;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private Map<String, Object> requirements;

    @Column
    private Integer estimatedMinutes;

    @Column
    @Builder.Default
    private Boolean isPublished = false;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Difficulty enum for exercises
     */
    public enum Difficulty {
        EASY,
        MEDIUM,
        HARD
    }

    /**
     * Test case for code validation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCase {
        private String name;
        private String input;
        private String expectedOutput;
        private Boolean isHidden;
    }

    /**
     * Get next hint
     */
    public String getHint(int hintNumber) {
        if (hints == null || hintNumber < 1 || hintNumber > hints.size()) {
            return null;
        }
        return hints.get(hintNumber - 1);
    }

    /**
     * Get total hints available
     */
    public int getTotalHints() {
        return hints != null ? hints.size() : 0;
    }
}
