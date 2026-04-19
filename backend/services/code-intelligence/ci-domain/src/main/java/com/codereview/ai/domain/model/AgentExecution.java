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
 * AgentExecution Entity - Agent Execution Tracking
 *
 * Tracks individual agent executions within a task.
 * Records input/output data, execution time, token usage, and status.
 *
 * @author Code Intelligence Service Team
 */
@Entity
@Table(name = "agent_executions", indexes = {
    @Index(name = "idx_agent_executions_task_id", columnList = "task_id"),
    @Index(name = "idx_agent_executions_agent_name", columnList = "agent_name"),
    @Index(name = "idx_agent_executions_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "agent_name", nullable = false, length = 100)
    private String agentName;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_data", columnDefinition = "jsonb")
    private Map<String, Object> inputData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_data", columnDefinition = "jsonb")
    private Map<String, Object> outputData;

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    @Column(name = "token_usage")
    private Integer tokenUsage;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Status enum defining the possible states of an agent execution
     */
    public enum Status {
        PENDING,      // Execution is pending
        RUNNING,      // Execution is currently running
        SUCCESS,      // Execution completed successfully
        FAILED        // Execution failed with an error
    }
}
