package com.codereview.ai.domain.repository;

import com.codereview.ai.domain.model.AgentExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for AgentExecution entity
 *
 * @author Code Intelligence Service Team
 */
@Repository
public interface AgentExecutionRepository extends JpaRepository<AgentExecution, Long> {

    /**
     * Find all executions by task ID
     */
    List<AgentExecution> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    /**
     * Find all executions by agent name
     */
    List<AgentExecution> findByAgentNameOrderByCreatedAtDesc(String agentName);

    /**
     * Find all executions by status
     */
    List<AgentExecution> findByStatusOrderByCreatedAtDesc(AgentExecution.Status status);

    /**
     * Find executions by task ID and agent name
     */
    List<AgentExecution> findByTaskIdAndAgentNameOrderByCreatedAtAsc(Long taskId, String agentName);

    /**
     * Find executions by status and agent name
     */
    List<AgentExecution> findByStatusAndAgentNameOrderByCreatedAtDesc(AgentExecution.Status status, String agentName);

    /**
     * Count executions by task ID
     */
    long countByTaskId(Long taskId);

    /**
     * Count executions by task ID and status
     */
    long countByTaskIdAndStatus(Long taskId, AgentExecution.Status status);

    /**
     * Calculate total token usage for a task
     */
    @Query("SELECT COALESCE(SUM(e.tokenUsage), 0) FROM AgentExecution e WHERE e.taskId = :taskId AND e.tokenUsage IS NOT NULL")
    Long getTotalTokenUsage(@Param("taskId") Long taskId);

    /**
     * Calculate total execution time for a task
     */
    @Query("SELECT COALESCE(SUM(e.executionTimeMs), 0) FROM AgentExecution e WHERE e.taskId = :taskId AND e.executionTimeMs IS NOT NULL")
    Long getTotalExecutionTime(@Param("taskId") Long taskId);
}
