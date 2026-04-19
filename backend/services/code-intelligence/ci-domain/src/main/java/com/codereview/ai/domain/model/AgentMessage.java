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
 * AgentMessage Entity - Agent-to-Agent Communication
 *
 * Represents messages exchanged between agents during task execution.
 * Used for tracking the communication flow in multi-agent scenarios.
 *
 * @author Code Intelligence Service Team
 */
@Entity
@Table(name = "agent_messages", indexes = {
    @Index(name = "idx_agent_messages_task_id", columnList = "task_id"),
    @Index(name = "idx_agent_messages_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "from_agent", nullable = false, length = 100)
    private String fromAgent;

    @Column(name = "to_agent", nullable = false, length = 100)
    private String toAgent;

    @Column(name = "message_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", columnDefinition = "jsonb")
    private Map<String, Object> content;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Message type enum defining the types of messages
     */
    public enum MessageType {
        REQUEST,   // Request message from one agent to another
        RESPONSE,  // Response message from an agent
        BROADCAST, // Broadcast message to all agents
        ERROR      // Error message
    }
}
