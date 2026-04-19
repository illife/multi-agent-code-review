package com.codereview.ai.domain.service;

import com.codereview.ai.domain.agent.shared.AgentExecutionContext;
import com.codereview.ai.domain.agent.shared.AgentExecutionResult;
import com.codereview.ai.domain.agent.shared.AgentOrchestrationService;
import com.codereview.ai.domain.model.*;
import com.codereview.ai.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent Orchestrator Service
 *
 * Coordinates multi-agent task execution using the new shared-agent framework.
 * This service manages the lifecycle of agent tasks, tracks executions, and handles inter-agent communication.
 *
 * @author Code Intelligence Service Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentOrchestratorService {

    private final AgentTaskRepository taskRepository;
    private final AgentExecutionRepository executionRepository;
    private final AgentMessageRepository messageRepository;
    private final AgentOrchestrationService agentService;

    /**
     * Create a new agent task
     *
     * @param userId User ID creating the task
     * @param taskType Type of task to create
     * @param requestData Request data for the task
     * @return Created AgentTask
     */
    @Transactional
    public AgentTask createTask(Long userId, AgentTask.TaskType taskType, Map<String, Object> requestData) {
        log.info("Creating agent task: userId={}, taskType={}", userId, taskType);

        AgentTask task = AgentTask.builder()
                .userId(userId)
                .taskType(taskType)
                .requestData(requestData)
                .status(AgentTask.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        return taskRepository.save(task);
    }

    /**
     * Execute a task by coordinating multiple agents
     *
     * @param taskId Task ID to execute
     * @return Updated AgentTask with results
     */
    @Transactional
    public AgentTask executeTask(Long taskId) {
        log.info("Executing agent task: taskId={}", taskId);

        AgentTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (task.getStatus() != AgentTask.Status.PENDING) {
            throw new IllegalStateException("Task is not in PENDING state: " + task.getStatus());
        }

        task.setStatus(AgentTask.Status.PROCESSING);
        taskRepository.save(task);

        try {
            long startTime = System.currentTimeMillis();

            // Execute agents based on task type
            List<AgentExecution> executions = executeAgentsForTask(task);

            // Aggregate results
            Map<String, Object> aggregatedResults = aggregateResults(executions);
            int totalTokens = executions.stream()
                    .mapToInt(e -> e.getTokenUsage() != null ? e.getTokenUsage() : 0)
                    .sum();

            // Update task with results
            task.setResultData(aggregatedResults);
            task.setStatus(AgentTask.Status.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            task.setExecutionTimeMs((int) (System.currentTimeMillis() - startTime));
            task.setTokenUsage(totalTokens);

            return taskRepository.save(task);

        } catch (Exception e) {
            log.error("Error executing task: {}", taskId, e);
            task.setStatus(AgentTask.Status.FAILED);
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
            return taskRepository.save(task);
        }
    }

    /**
     * Execute agents for a specific task
     */
    private List<AgentExecution> executeAgentsForTask(AgentTask task) {
        List<String> agentTypes = getAgentsForTaskType(task.getTaskType());
        List<AgentExecution> executions = new ArrayList<>();

        for (String agentType : agentTypes) {
            try {
                AgentExecution execution = executeAgentForTask(
                        task.getId(),
                        agentType,
                        task.getRequestData(),
                        task.getUserId()
                );
                executions.add(execution);
            } catch (Exception e) {
                log.error("Failed to execute agent: {} for task: {}", agentType, task.getId(), e);
                // Create a failed execution record
                AgentExecution failedExecution = AgentExecution.builder()
                        .taskId(task.getId())
                        .agentName(agentType)
                        .action("EXECUTE")
                        .status(AgentExecution.Status.FAILED)
                        .errorMessage(e.getMessage())
                        .inputData(task.getRequestData())
                        .createdAt(LocalDateTime.now())
                        .build();
                executions.add(executionRepository.save(failedExecution));
            }
        }

        return executions;
    }

    /**
     * Execute a single agent for a task
     */
    private AgentExecution executeAgentForTask(Long taskId, String agentType,
                                                Map<String, Object> inputData, Long userId) {
        log.info("Executing agent: taskId={}, agentType={}", taskId, agentType);

        long startTime = System.currentTimeMillis();

        // Build execution context
        AgentExecutionContext context = buildContext(inputData, userId);

        // Execute agent using the agent service
        AgentExecutionResult result = agentService.executeAgent(agentType, context);

        // Create execution record
        AgentExecution execution = AgentExecution.builder()
                .taskId(taskId)
                .agentName(agentType)
                .action("EXECUTE")
                .inputData(inputData)
                .outputData(resultToMap(result))
                .status(result.isSuccess() ? AgentExecution.Status.SUCCESS : AgentExecution.Status.FAILED)
                .executionTimeMs((int) result.getExecutionTimeMs())
                .createdAt(LocalDateTime.now())
                .build();

        if (!result.isSuccess()) {
            execution.setErrorMessage(result.getError());
        }

        return executionRepository.save(execution);
    }

    /**
     * Build AgentExecutionContext from input data
     */
    private AgentExecutionContext buildContext(Map<String, Object> inputData, Long userId) {
        AgentExecutionContext.AgentExecutionContextBuilder builder = AgentExecutionContext.builder()
                .userId(userId)
                .requestId(UUID.randomUUID().toString());

        // Extract common fields
        if (inputData != null) {
            builder.code((String) inputData.get("code"));
            builder.language((String) inputData.get("language"));
            builder.filePath((String) inputData.get("filePath"));
            builder.projectId(inputData.get("projectId") != null ?
                    ((Number) inputData.get("projectId")).longValue() : null);

            // Add all other data to contextData
            Map<String, Object> contextData = new HashMap<>(inputData);
            contextData.remove("code");
            contextData.remove("language");
            contextData.remove("filePath");
            contextData.remove("projectId");
            builder.contextData(contextData);
        }

        return builder.build();
    }

    /**
     * Convert AgentExecutionResult to Map
     */
    private Map<String, Object> resultToMap(AgentExecutionResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", result.isSuccess());
        map.put("message", result.getMessage());
        map.put("issues", result.getIssues());
        map.put("outputData", result.getOutputData());
        map.put("executionTimeMs", result.getExecutionTimeMs());
        return map;
    }

    /**
     * Aggregate results from multiple executions
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> aggregateResults(List<AgentExecution> executions) {
        Map<String, Object> aggregated = new HashMap<>();

        // Collect all issues
        List<Map<String, Object>> allIssues = executions.stream()
                .filter(e -> e.getOutputData() != null)
                .flatMap(e -> {
                    Map<String, Object> output = e.getOutputData();
                    Object issues = output.get("issues");
                    if (issues instanceof List) {
                        return ((List<?>) issues).stream();
                    }
                    return java.util.stream.Stream.empty();
                })
                .map(obj -> (Map<String, Object>) obj)
                .collect(Collectors.toList());

        // Deduplicate issues based on lineNumber, title, and category
        List<Map<String, Object>> deduplicatedIssues = deduplicateIssues(allIssues);

        aggregated.put("issues", deduplicatedIssues);
        aggregated.put("totalIssuesFound", allIssues.size());
        aggregated.put("uniqueIssuesCount", deduplicatedIssues.size());
        aggregated.put("executionCount", executions.size());
        aggregated.put("successCount", executions.stream()
                .mapToInt(e -> e.getStatus() == AgentExecution.Status.SUCCESS ? 1 : 0)
                .sum());

        return aggregated;
    }

    /**
     * Deduplicate issues based on lineNumber, title, and category
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> deduplicateIssues(List<Map<String, Object>> issues) {
        // Use a set to track unique issue signatures
        Set<String> seenSignatures = new HashSet<>();
        List<Map<String, Object>> deduplicated = new ArrayList<>();

        for (Map<String, Object> issue : issues) {
            // Create a signature for deduplication
            String signature = createIssueSignature(issue);

            // Only add if we haven't seen this signature before
            if (!seenSignatures.contains(signature)) {
                seenSignatures.add(signature);
                deduplicated.add(issue);
            }
        }

        return deduplicated;
    }

    /**
     * Create a unique signature for an issue for deduplication
     */
    @SuppressWarnings("unchecked")
    private String createIssueSignature(Map<String, Object> issue) {
        StringBuilder signature = new StringBuilder();

        // Line number is the strongest indicator of uniqueness
        Object lineNumber = issue.get("lineNumber");
        if (lineNumber != null) {
            signature.append("line:").append(lineNumber).append("|");
        }

        // Title and category
        String title = (String) issue.get("title");
        String category = (String) issue.get("category");
        signature.append("title:").append(title != null ? title : "").append("|");
        signature.append("category:").append(category != null ? category : "").append("|");

        // Code snippet (first 50 chars) for additional uniqueness
        Object codeSnippet = issue.get("codeSnippet");
        if (codeSnippet instanceof String snippet && snippet.length() > 0) {
            String truncated = snippet.length() > 50 ? snippet.substring(0, 50) : snippet;
            signature.append("code:").append(truncated);
        }

        return signature.toString();
    }

    /**
     * Get agents for a specific task type
     */
    private List<String> getAgentsForTaskType(AgentTask.TaskType taskType) {
        return switch (taskType) {
            case CODE_REVIEW -> List.of(
                    "CODE_STANDARDS_INSPECTOR",
                    "ARCHITECTURE_GUARDIAN",
                    "SECURITY_AUDITOR",
                    "PERFORMANCE_OPTIMIZER"
            );
            case LEARNING_PATH -> List.of("LEARNING_PATH_PLANNER");
            case EXERCISE_GEN -> List.of("EXERCISE_COACH");
            case QA -> List.of("TEACHING_MENTOR");
        };
    }

    /**
     * Get task status
     */
    public AgentTask getTaskStatus(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
    }

    /**
     * Get all executions for a task
     */
    public List<AgentExecution> getTaskExecutions(Long taskId) {
        return executionRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    /**
     * Get all messages for a task
     */
    public List<AgentMessage> getTaskMessages(Long taskId) {
        return messageRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    /**
     * Send a message between agents
     */
    @Transactional
    public AgentMessage sendMessage(Long taskId, String fromAgent, String toAgent,
                                   AgentMessage.MessageType messageType, Map<String, Object> content) {
        log.debug("Agent message: taskId={}, from={}, to={}, type={}", taskId, fromAgent, toAgent, messageType);

        AgentMessage message = AgentMessage.builder()
                .taskId(taskId)
                .fromAgent(fromAgent)
                .toAgent(toAgent)
                .messageType(messageType)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();

        return messageRepository.save(message);
    }

    /**
     * Get statistics for a task
     */
    public Map<String, Object> getTaskStatistics(Long taskId) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalExecutions", executionRepository.countByTaskId(taskId));
        stats.put("totalMessages", messageRepository.countByTaskId(taskId));
        stats.put("totalTokens", executionRepository.getTotalTokenUsage(taskId));
        stats.put("totalExecutionTime", executionRepository.getTotalExecutionTime(taskId));

        return stats;
    }
}
