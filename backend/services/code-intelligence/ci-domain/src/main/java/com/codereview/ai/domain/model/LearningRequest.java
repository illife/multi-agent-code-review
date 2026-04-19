package com.codereview.ai.domain.model;

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
import java.util.Map;

/**
 * LearningRequest Entity - Learning Path Requests
 *
 * Represents user requests for personalized learning paths.
 * Stores the target skill, current level, and user preferences.
 *
 * @author Code Intelligence Service Team
 */
@Entity
@Table(name = "learning_requests", indexes = {
    @Index(name = "idx_learning_requests_user_id", columnList = "user_id"),
    @Index(name = "idx_learning_requests_task_id", columnList = "task_id"),
    @Index(name = "idx_learning_requests_target_skill", columnList = "target_skill")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "request_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private RequestType requestType;

    @Column(name = "target_skill", nullable = false, length = 100)
    private String targetSkill;

    @Column(name = "current_level", length = 50)
    private String currentLevel;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preference", columnDefinition = "jsonb")
    private Map<String, Object> preference;

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Request type enum defining the types of learning requests
     */
    public enum RequestType {
        LEARNING_PATH,    // Request for a complete learning path
        EXERCISE,         // Request for specific exercises
        QA                // Request for Q&A
    }

    /**
     * Request status enum defining the possible states of a learning request
     */
    public enum RequestStatus {
        PENDING,      // Request is waiting to be processed
        PROCESSING,   // Request is being processed
        COMPLETED,    // Request has been completed
        FAILED        // Request failed
    }
}
