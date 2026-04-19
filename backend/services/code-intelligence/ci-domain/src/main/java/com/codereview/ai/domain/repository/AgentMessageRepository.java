package com.codereview.ai.domain.repository;

import com.codereview.ai.domain.model.AgentMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AgentMessage entity
 *
 * @author Code Intelligence Service Team
 */
@Repository
public interface AgentMessageRepository extends JpaRepository<AgentMessage, Long> {

    /**
     * Find all messages by task ID
     */
    List<AgentMessage> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    /**
     * Find all messages from a specific agent
     */
    List<AgentMessage> findByFromAgentOrderByCreatedAtAsc(String fromAgent);

    /**
     * Find all messages to a specific agent
     */
    List<AgentMessage> findByToAgentOrderByCreatedAtAsc(String toAgent);

    /**
     * Find all messages between two agents
     */
    List<AgentMessage> findByFromAgentAndToAgentOrderByCreatedAtAsc(String fromAgent, String toAgent);

    /**
     * Find messages by task ID and message type
     */
    List<AgentMessage> findByTaskIdAndMessageTypeOrderByCreatedAtAsc(Long taskId, AgentMessage.MessageType messageType);

    /**
     * Find messages created after a specific time
     */
    List<AgentMessage> findByCreatedAtAfterOrderByCreatedAtAsc(LocalDateTime createdAt);

    /**
     * Count messages by task ID
     */
    long countByTaskId(Long taskId);
}
