package com.codereview.ai.domain.service;

import com.codereview.ai.domain.model.AgentTask;
import com.codereview.ai.domain.model.LearningPath;
import com.codereview.ai.domain.model.LearningRequest;
import com.codereview.ai.domain.repository.AgentTaskRepository;
import com.codereview.ai.domain.repository.LearningPathRepository;
import com.codereview.ai.domain.repository.LearningRequestRepository;
import com.codereview.ai.domain.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent Task Service
 *
 * High-level service for creating different types of agent tasks.
 * Provides convenient methods for each task type.
 *
 * @author Code Intelligence Service Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentTaskService {

    private final AgentTaskRepository taskRepository;
    private final AgentOrchestratorService orchestratorService;
    private final ProjectRepository projectRepository;
    private final LearningPathRepository learningPathRepository;
    private final LearningRequestRepository learningRequestRepository;

    /**
     * Create a code review task for a project
     *
     * @param userId User ID creating the task
     * @param projectId Project ID to review
     * @return Created AgentTask
     */
    @Transactional
    public AgentTask createCodeReviewTask(Long userId, Long projectId) {
        log.info("Creating code review task: userId={}, projectId={}", userId, projectId);

        // Verify project exists
        projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("projectId", projectId);
        requestData.put("userId", userId);

        return orchestratorService.createTask(userId, AgentTask.TaskType.CODE_REVIEW, requestData);
    }

    /**
     * Create a code review task and execute it immediately
     *
     * @param userId User ID creating the task
     * @param projectId Project ID to review
     * @return Executed AgentTask
     */
    @Transactional
    public AgentTask createAndExecuteCodeReviewTask(Long userId, Long projectId) {
        AgentTask task = createCodeReviewTask(userId, projectId);
        return orchestratorService.executeTask(task.getId());
    }

    /**
     * Create a learning path generation task
     *
     * @param userId User ID creating the task
     * @param targetSkill Target skill to learn
     * @param currentLevel Current skill level (optional)
     * @param description Additional description (optional)
     * @return Created AgentTask
     */
    @Transactional
    public AgentTask createLearningPathTask(Long userId, String targetSkill,
                                           String currentLevel, String description) {
        log.info("Creating learning path task: userId={}, targetSkill={}", userId, targetSkill);

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("targetSkill", targetSkill);
        requestData.put("currentLevel", currentLevel != null ? currentLevel : "BEGINNER");
        requestData.put("description", description != null ? description : "Learn " + targetSkill);

        AgentTask task = orchestratorService.createTask(userId, AgentTask.TaskType.LEARNING_PATH, requestData);

        // Also create a learning request record
        LearningRequest request = LearningRequest.builder()
                .taskId(task.getId())
                .userId(userId)
                .requestType(LearningRequest.RequestType.LEARNING_PATH)
                .targetSkill(targetSkill)
                .currentLevel(currentLevel != null ? currentLevel : "BEGINNER")
                .description(description)
                .status(LearningRequest.RequestStatus.PENDING)
                .build();

        learningRequestRepository.save(request);

        return task;
    }

    /**
     * Create a learning path task and execute it immediately
     *
     * @param userId User ID creating the task
     * @param targetSkill Target skill to learn
     * @param currentLevel Current skill level
     * @param description Additional description
     * @return Executed AgentTask
     */
    @Transactional
    public AgentTask createAndExecuteLearningPathTask(Long userId, String targetSkill,
                                                      String currentLevel, String description) {
        AgentTask task = createLearningPathTask(userId, targetSkill, currentLevel, description);
        return orchestratorService.executeTask(task.getId());
    }

    /**
     * Create an exercise generation task
     *
     * @param userId User ID creating the task
     * @param skillTag Skill tag for the exercise
     * @param difficulty Difficulty level
     * @param count Number of exercises to generate
     * @return Created AgentTask
     */
    @Transactional
    public AgentTask createExerciseTask(Long userId, String skillTag,
                                       String difficulty, Integer count) {
        log.info("Creating exercise task: userId={}, skillTag={}, difficulty={}",
                userId, skillTag, difficulty);

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("skillTag", skillTag);
        requestData.put("difficulty", difficulty != null ? difficulty : "MEDIUM");
        requestData.put("count", count != null ? count : 1);

        AgentTask task = orchestratorService.createTask(userId, AgentTask.TaskType.EXERCISE_GEN, requestData);

        // Also create a learning request record
        LearningRequest request = LearningRequest.builder()
                .taskId(task.getId())
                .userId(userId)
                .requestType(LearningRequest.RequestType.EXERCISE)
                .targetSkill(skillTag)
                .description("Generate " + (count != null ? count : 1) + " " +
                            (difficulty != null ? difficulty : "MEDIUM") + " exercises")
                .status(LearningRequest.RequestStatus.PENDING)
                .build();

        learningRequestRepository.save(request);

        return task;
    }

    /**
     * Create an exercise task and execute it immediately
     *
     * @param userId User ID creating the task
     * @param skillTag Skill tag for the exercise
     * @param difficulty Difficulty level
     * @param count Number of exercises to generate
     * @return Executed AgentTask
     */
    @Transactional
    public AgentTask createAndExecuteExerciseTask(Long userId, String skillTag,
                                                  String difficulty, Integer count) {
        AgentTask task = createExerciseTask(userId, skillTag, difficulty, count);
        return orchestratorService.executeTask(task.getId());
    }

    /**
     * Create a Q&A task
     *
     * @param userId User ID creating the task
     * @param question Question to answer
     * @param context Optional context for the question
     * @return Created AgentTask
     */
    @Transactional
    public AgentTask createQATask(Long userId, String question, String context) {
        log.info("Creating QA task: userId={}, question={}", userId, question);

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("question", question);
        requestData.put("context", context);

        return orchestratorService.createTask(userId, AgentTask.TaskType.QA, requestData);
    }

    /**
     * Create a Q&A task and execute it immediately
     *
     * @param userId User ID creating the task
     * @param question Question to answer
     * @param context Optional context for the question
     * @return Executed AgentTask
     */
    @Transactional
    public AgentTask createAndExecuteQATask(Long userId, String question, String context) {
        AgentTask task = createQATask(userId, question, context);
        return orchestratorService.executeTask(task.getId());
    }

    /**
     * Get task by ID
     *
     * @param taskId Task ID
     * @return AgentTask
     */
    public AgentTask getTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
    }

    /**
     * Get task by ID and user ID (for permission check)
     *
     * @param taskId Task ID
     * @param userId User ID
     * @return AgentTask
     */
    public AgentTask getTaskForUser(Long taskId, Long userId) {
        return taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found or access denied: " + taskId));
    }

    /**
     * Get all tasks for a user
     *
     * @param userId User ID
     * @return List of AgentTasks
     */
    public java.util.List<AgentTask> getUserTasks(Long userId) {
        return taskRepository.findByUserId(userId);
    }

    /**
     * Get all tasks for a user by type
     *
     * @param userId User ID
     * @param taskType Task type
     * @return List of AgentTasks
     */
    public java.util.List<AgentTask> getUserTasksByType(Long userId, AgentTask.TaskType taskType) {
        return taskRepository.findByUserIdAndTaskType(userId, taskType);
    }

    /**
     * Get pending task count for a user
     *
     * @param userId User ID
     * @return Number of pending tasks
     */
    public long getPendingTaskCount(Long userId) {
        return taskRepository.countByUserIdAndStatus(userId, AgentTask.Status.PENDING);
    }
}
