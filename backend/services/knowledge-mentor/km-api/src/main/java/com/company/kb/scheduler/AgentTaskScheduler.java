package com.company.kb.scheduler;

import com.company.kb.core.domain.AgentTask;
import com.company.kb.core.service.AgentTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 智能体任务调度器
 * 定期扫描并执行待处理的智能体任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentTaskScheduler {

    private final AgentTaskService agentTaskService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    /**
     * 每30秒执行一次待处理任务
     */
    @Scheduled(fixedRate = 30000)
    public void processPendingTasks() {
        try {
            List<AgentTask> pendingTasks = agentTaskService.getPendingTasks();

            if (!pendingTasks.isEmpty()) {
                log.info("发现 {} 个待执行任务", pendingTasks.size());

                for (AgentTask task : pendingTasks) {
                    // 异步执行任务
                    CompletableFuture.runAsync(() -> {
                        try {
                            log.info("开始执行任务: taskId={}, taskName={}", task.getId(), task.getTaskName());
                            agentTaskService.executeTask(task.getId());
                        } catch (Exception e) {
                            log.error("任务执行异常: taskId={}", task.getId(), e);
                        }
                    }, executorService);
                }
            }

        } catch (Exception e) {
            log.error("处理待执行任务失败", e);
        }
    }

    /**
     * 每分钟检查并处理超时任务
     */
    @Scheduled(fixedRate = 60000)
    public void processTimeoutTasks() {
        try {
            // 超过30分钟仍在运行的任务视为超时
            LocalDateTime timeout = LocalDateTime.now().minusMinutes(30);
            List<AgentTask> timeoutTasks = agentTaskService.getTimeoutTasks(timeout);

            if (!timeoutTasks.isEmpty()) {
                log.warn("发现 {} 个超时任务", timeoutTasks.size());

                for (AgentTask task : timeoutTasks) {
                    try {
                        log.warn("处理超时任务: taskId={}, taskName={}", task.getId(), task.getTaskName());
                        agentTaskService.handleTimeoutTask(task);
                    } catch (Exception e) {
                        log.error("处理超时任务失败: taskId={}", task.getId(), e);
                    }
                }
            }

        } catch (Exception e) {
            log.error("处理超时任务失败", e);
        }
    }

    /**
     * 每分钟重试失败任务
     */
    @Scheduled(fixedRate = 60000)
    public void retryFailedTasks() {
        try {
            List<AgentTask> retryableTasks = agentTaskService.getRetryableTasks();

            if (!retryableTasks.isEmpty()) {
                log.info("发现 {} 个可重试任务", retryableTasks.size());

                for (AgentTask task : retryableTasks) {
                    // 异步重试任务
                    CompletableFuture.runAsync(() -> {
                        try {
                            log.info("重试任务: taskId={}, taskName={}, retryCount={}",
                                    task.getId(), task.getTaskName(), task.getRetryCount() + 1);
                            agentTaskService.executeTask(task.getId());
                        } catch (Exception e) {
                            log.error("任务重试异常: taskId={}", task.getId(), e);
                        }
                    }, executorService);
                }
            }

        } catch (Exception e) {
            log.error("重试失败任务失败", e);
        }
    }
}
