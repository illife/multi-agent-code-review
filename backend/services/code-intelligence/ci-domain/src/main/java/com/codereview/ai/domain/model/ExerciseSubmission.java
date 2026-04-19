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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ExerciseSubmission entity
 * Tracks user exercise submissions
 *
 * @author Code Review AI Team
 */
@Entity
@Table(name = "exercise_submissions")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false, insertable = false, updatable = false)
    @JsonIgnore
    private Exercise exercise;

    @Column(name = "exercise_id", nullable = false)
    private Long exerciseId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String submittedCode;

    @Column
    private Integer score;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private List<TestResult> testResults;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column
    private Integer timeSpentSeconds;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Test result for validation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestResult {
        private String testName;
        private Boolean passed;
        private String expectedOutput;
        private String actualOutput;
        private String errorMessage;
    }

    /**
     * Calculate pass percentage
     */
    public double getPassPercentage() {
        if (testResults == null || testResults.isEmpty()) {
            return 0.0;
        }
        long passed = testResults.stream().filter(TestResult::getPassed).count();
        return (double) passed / testResults.size() * 100;
    }

    /**
     * Get total test count
     */
    public int getTotalTests() {
        return testResults != null ? testResults.size() : 0;
    }

    /**
     * Get passed test count
     */
    public long getPassedTests() {
        return testResults != null ? testResults.stream().filter(TestResult::getPassed).count() : 0;
    }
}
