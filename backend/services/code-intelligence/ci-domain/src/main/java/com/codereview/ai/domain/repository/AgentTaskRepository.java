package com.codereview.ai.domain.repository;

import com.codereview.ai.domain.model.AgentTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for AgentTask entity
 *
 * @author Code Intelligence Service Team
 */
@Repository
public interface AgentTaskRepository extends JpaRepository<AgentTask, Long> {

    /**
     * Find all tasks by user ID
     */
    List<AgentTask> findByUserId(Long userId);

    /**
     * Find all tasks by user ID and task type
     */
    List<AgentTask> findByUserIdAndTaskType(Long userId, AgentTask.TaskType taskType);

    /**
     * Find all tasks by status
     */
    List<AgentTask> findByStatus(AgentTask.Status status);

    /**
     * Find pending tasks older than specified time
     */
    @Query("SELECT t FROM AgentTask t WHERE t.status = 'PENDING' AND t.createdAt < :createdAt")
    List<AgentTask> findPendingTasksOlderThan(@Param("createdAt") LocalDateTime createdAt);

    /**
     * Find task by user ID and task ID
     */
    Optional<AgentTask> findByIdAndUserId(Long id, Long userId);

    /**
     * Count tasks by user ID and status
     */
    long countByUserIdAndStatus(Long userId, AgentTask.Status status);
}
