package com.company.kb.core.repository;

import com.company.kb.core.domain.AgentTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能体任务 Repository
 */
@Repository
public interface AgentTaskRepository extends JpaRepository<AgentTask, Long> {

    /**
     * 根据用户ID查询任务
     */
    List<AgentTask> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * 根据用户ID查询任务（分页）
     */
    Page<AgentTask> findByUserId(String userId, Pageable pageable);

    /**
     * 根据用户ID和状态查询任务
     */
    List<AgentTask> findByUserIdAndStatusOrderByNextRunAtAsc(
        String userId, AgentTask.TaskStatus status
    );

    /**
     * 查询待执行的任务（按下次执行时间排序）
     */
    @Query("SELECT t FROM AgentTask t WHERE t.status = 'PENDING' " +
           "AND (t.nextRunAt IS NULL OR t.nextRunAt <= :now) " +
           "ORDER BY t.priority ASC, t.nextRunAt ASC")
    List<AgentTask> findPendingTasks(@Param("now") LocalDateTime now);

    /**
     * 查询运行超时的任务
     */
    @Query("SELECT t FROM AgentTask t WHERE t.status = 'RUNNING' " +
           "AND t.startedAt < :timeout")
    List<AgentTask> findTimeoutTasks(@Param("timeout") LocalDateTime timeout);

    /**
     * 根据任务类型查询
     */
    List<AgentTask> findByUserIdAndTaskTypeOrderByCreatedAtDesc(
        String userId, AgentTask.TaskType taskType
    );

    /**
     * 查询可重试的失败任务
     */
    @Query("SELECT t FROM AgentTask t WHERE t.status = 'FAILED' " +
           "AND t.retryCount < t.maxRetries " +
           "ORDER BY t.priority ASC, t.createdAt ASC")
    List<AgentTask> findRetryableTasks();

    /**
     * 统计用户任务执行情况
     */
    @Query("SELECT COUNT(t) FROM AgentTask t WHERE t.userId = :userId " +
           "AND t.status = :status")
    long countByUserIdAndStatus(@Param("userId") String userId, @Param("status") AgentTask.TaskStatus status);

    /**
     * 根据关键词搜索任务
     */
    @Query("SELECT t FROM AgentTask t WHERE t.userId = :userId " +
           "AND (t.taskName LIKE %:keyword% OR t.result LIKE %:keyword%) " +
           "ORDER BY t.createdAt DESC")
    List<AgentTask> searchByKeyword(@Param("userId") String userId, @Param("keyword") String keyword);

    /**
     * 根据关键词搜索任务（分页）
     */
    @Query("SELECT t FROM AgentTask t WHERE t.userId = :userId " +
           "AND (t.taskName LIKE %:keyword% OR t.result LIKE %:keyword%)")
    Page<AgentTask> searchByKeyword(@Param("userId") String userId, @Param("keyword") String keyword, Pageable pageable);
}
