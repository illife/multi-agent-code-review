package com.company.kb.core.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 智能体任务实体
 * 后台异步执行的智能体任务
 */
@Entity
@Table(name = "agent_tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 任务名称
     */
    @Column(nullable = false, length = 255)
    private String taskName;

    /**
     * 任务类型: KNOWLEDGE_EXTRACTION, CONTENT_GENERATION, ANALYSIS, REVIEW等
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 50)
    private TaskType taskType;

    /**
     * 关联的用户ID
     */
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    /**
     * 任务配置（JSON格式）
     */
    @Column(name = "config", columnDefinition = "TEXT")
    private String config;

    /**
     * 任务状态: PENDING(待执行), RUNNING(执行中), COMPLETED(已完成), FAILED(失败), CANCELLED(已取消)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status;

    /**
     * 任务优先级: 1(高), 2(中), 3(低)
     */
    @Column(name = "priority", nullable = false)
    private Integer priority = 2;

    /**
     * 任务执行结果（JSON格式）
     */
    @Column(name = "result", columnDefinition = "TEXT")
    private String result;

    /**
     * 错误信息（执行失败时）
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /**
     * 执行开始时间
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * 执行结束时间
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 重试次数
     */
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    /**
     * 最大重试次数
     */
    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 3;

    /**
     * 下次执行时间（定时任务）
     */
    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    /**
     * Cron表达式（定时任务）
     */
    @Column(name = "cron_expression", length = 100)
    private String cronExpression;

    /**
     * 任务元数据（JSON格式）
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * 创建者
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 任务类型枚举
     */
    public enum TaskType {
        KNOWLEDGE_EXTRACTION,    // 知识提取
        CONTENT_GENERATION,     // 内容生成
        DOCUMENT_ANALYSIS,      // 文档分析
        CODE_REVIEW,            // 代码审查
        TEST_GENERATION,        // 测试生成
        LEARNING_PATH,          // 学习路径规划
        KNOWLEDGE_GAP_ANALYSIS, // 知识缺口分析
        CUSTOM                  // 自定义任务
    }

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        PENDING,    // 待执行
        RUNNING,    // 执行中
        COMPLETED,  // 已完成
        FAILED,     // 失败
        CANCELLED   // 已取消
    }
}
