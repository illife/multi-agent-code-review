package com.codereview.ai.domain.model;

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
import java.util.Map;

/**
 * AgentTask Entity - Multi-Agent Task Management
 *
 * Represents a task that can be executed by one or more agents in the system.
 * Supports various task types like code review, learning path generation, exercise generation, and Q&A.
 *
 * @author Code Intelligence Service Team
 */
@Entity
@Table(name = "agent_tasks")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "task_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private TaskType taskType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_data", columnDefinition = "jsonb")
    private Map<String, Object> requestData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_data", columnDefinition = "jsonb")
    private Map<String, Object> resultData;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    @Column(name = "token_usage")
    private Integer tokenUsage;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Task type enum defining the types of tasks that can be executed by agents
     */
    public enum TaskType {
        CODE_REVIEW,      // Code review and analysis task
        LEARNING_PATH,    // Learning path generation task
        EXERCISE_GEN,     // Exercise generation task
        QA                // Question and answer task
    }

    /**
     * Status enum defining the possible states of a task
     */
    public enum Status {
        PENDING,      // Task is waiting to be processed
        PROCESSING,   // Task is currently being processed
        COMPLETED,    // Task completed successfully
        FAILED        // Task failed with an error
    }
}
