package com.codereview.ai.domain.service;

import com.codereview.ai.domain.model.*;
import com.codereview.ai.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Learning Service
 *
 * AI teaching functionality including learning paths, exercises, and submissions.
 *
 * @author Code Intelligence Service Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LearningService {

    private final LearningPathRepository learningPathRepository;
    private final LearningRequestRepository learningRequestRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseSubmissionRepository submissionRepository;
    private final AgentTaskService agentTaskService;

    /**
     * Create a learning request
     *
     * @param userId User ID
     * @param targetSkill Target skill to learn
     * @param currentLevel Current skill level
     * @param description Additional description
     * @return Created LearningRequest
     */
    @Transactional
    public LearningRequest createLearningRequest(Long userId, String targetSkill,
                                                String currentLevel, String description) {
        log.info("Creating learning request: userId={}, targetSkill={}", userId, targetSkill);

        LearningRequest request = LearningRequest.builder()
                .userId(userId)
                .requestType(LearningRequest.RequestType.LEARNING_PATH)
                .targetSkill(targetSkill)
                .currentLevel(currentLevel != null ? currentLevel : "BEGINNER")
                .description(description)
                .status(LearningRequest.RequestStatus.PENDING)
                .build();

        return learningRequestRepository.save(request);
    }

    /**
     * Get or create a learning path for a user and skill
     *
     * @param userId User ID
     * @param targetSkill Target skill
     * @return LearningPath
     */
    @Transactional
    public LearningPath getOrCreateLearningPath(Long userId, String targetSkill) {
        log.info("Getting learning path: userId={}, targetSkill={}", userId, targetSkill);

        // Check if active path exists
        Optional<LearningPath> existingPath = learningPathRepository
                .findByUserIdAndTargetSkillAndStatus(userId, targetSkill, LearningPath.Status.ACTIVE);

        if (existingPath.isPresent()) {
            return existingPath.get();
        }

        // Create new learning path via agent task
        AgentTask task = agentTaskService.createAndExecuteLearningPathTask(
                userId, targetSkill, null, null);

        // Extract learning path from task result
        Map<String, Object> resultData = task.getResultData();
        if (resultData != null && resultData.containsKey("learningPath")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> pathData = (Map<String, Object>) resultData.get("learningPath");

            LearningPath path = LearningPath.builder()
                    .userId(userId)
                    .taskId(task.getId())
                    .title((String) pathData.get("title"))
                    .targetSkill(targetSkill)
                    .weeks((Integer) pathData.getOrDefault("weeks", 4))
                    .steps((List<Map<String, Object>>) pathData.get("steps"))
                    .status(LearningPath.Status.ACTIVE)
                    .progress(0)
                    .currentStep(0)
                    .build();

            return learningPathRepository.save(path);
        }

        throw new IllegalStateException("Failed to generate learning path");
    }

    /**
     * Get a learning path by ID
     *
     * @param pathId Learning path ID
     * @return LearningPath
     */
    public LearningPath getLearningPath(Long pathId) {
        return learningPathRepository.findById(pathId)
                .orElseThrow(() -> new IllegalArgumentException("Learning path not found: " + pathId));
    }

    /**
     * Get all learning paths for a user
     *
     * @param userId User ID
     * @return List of LearningPaths
     */
    public List<LearningPath> getUserLearningPaths(Long userId) {
        return learningPathRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get active learning paths for a user
     *
     * @param userId User ID
     * @return List of active LearningPaths
     */
    public List<LearningPath> getActiveLearningPaths(Long userId) {
        return learningPathRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                userId, LearningPath.Status.ACTIVE);
    }

    /**
     * Update learning path progress
     *
     * @param pathId Learning path ID
     * @param progress Progress percentage (0-100)
     * @param currentStep Current step index
     */
    @Transactional
    public void updateLearningPathProgress(Long pathId, Integer progress, Integer currentStep) {
        LearningPath path = getLearningPath(pathId);

        path.setProgress(progress);
        path.setCurrentStep(currentStep);

        if (progress >= 100) {
            path.setStatus(LearningPath.Status.COMPLETED);
            path.setCompletedAt(LocalDateTime.now());
        }

        learningPathRepository.save(path);
    }

    /**
     * Get an exercise by ID
     *
     * @param exerciseId Exercise ID
     * @return Exercise
     */
    public Exercise getExercise(Long exerciseId) {
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new IllegalArgumentException("Exercise not found: " + exerciseId));
    }

    /**
     * Get exercises by skill tag
     *
     * @param skillTag Skill tag
     * @param difficulty Difficulty level
     * @return List of Exercises
     */
    public List<Exercise> getExercisesBySkill(String skillTag, String difficulty) {
        // This would typically query by skill tag and difficulty
        // For now, return all published exercises
        return exerciseRepository.findAll().stream()
                .filter(e -> e.getIsPublished())
                .filter(e -> skillTag == null || skillTag.equals(e.getSkillTag()))
                .filter(e -> difficulty == null || difficulty.equals(e.getDifficulty().name()))
                .toList();
    }

    /**
     * Generate exercises using AI
     *
     * @param userId User ID
     * @param skillTag Skill tag
     * @param difficulty Difficulty level
     * @param count Number of exercises
     * @return List of generated Exercises
     */
    @Transactional
    public List<Exercise> generateExercises(Long userId, String skillTag,
                                           String difficulty, Integer count) {
        log.info("Generating exercises: userId={}, skillTag={}, difficulty={}",
                userId, skillTag, difficulty);

        AgentTask task = agentTaskService.createAndExecuteExerciseTask(
                userId, skillTag, difficulty, count);

        // Extract exercises from task result
        Map<String, Object> resultData = task.getResultData();
        if (resultData != null && resultData.containsKey("exercises")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> exercisesData = (List<Map<String, Object>>) resultData.get("exercises");

            return exercisesData.stream()
                    .map(this::mapToExercise)
                    .toList();
        }

        throw new IllegalStateException("Failed to generate exercises");
    }

    /**
     * Submit an exercise solution
     *
     * @param exerciseId Exercise ID
     * @param userId User ID
     * @param code Submitted code
     * @return ExerciseSubmission
     */
    @Transactional
    public ExerciseSubmission submitExercise(Long exerciseId, Long userId, String code) {
        log.info("Submitting exercise: exerciseId={}, userId={}", exerciseId, userId);

        Exercise exercise = getExercise(exerciseId);

        ExerciseSubmission submission = ExerciseSubmission.builder()
                .userId(userId)
                .exerciseId(exerciseId)
                .submittedCode(code)
                .submittedAt(LocalDateTime.now())
                .build();

        // Validate submission against test cases
        if (exercise.getTestCases() != null && !exercise.getTestCases().isEmpty()) {
            List<ExerciseSubmission.TestResult> results = validateCode(code, exercise.getTestCases());
            submission.setTestResults(results);

            // Calculate score
            long passed = results.stream().filter(ExerciseSubmission.TestResult::getPassed).count();
            int score = (int) ((passed * 100.0) / results.size());
            submission.setScore(score);
        }

        return submissionRepository.save(submission);
    }

    /**
     * Get submission history for a user and exercise
     *
     * @param userId User ID
     * @param exerciseId Exercise ID
     * @return List of ExerciseSubmissions
     */
    public List<ExerciseSubmission> getSubmissionHistory(Long userId, Long exerciseId) {
        return submissionRepository.findByUserIdAndExerciseIdOrderBySubmittedAtDesc(userId, exerciseId);
    }

    /**
     * Get best submission for a user and exercise
     *
     * @param userId User ID
     * @param exerciseId Exercise ID
     * @return Best ExerciseSubmission
     */
    public Optional<ExerciseSubmission> getBestSubmission(Long userId, Long exerciseId) {
        return submissionRepository.findFirstByUserIdAndExerciseIdOrderByScoreDesc(userId, exerciseId);
    }

    /**
     * Get user statistics
     *
     * @param userId User ID
     * @return Map of statistics
     */
    public Map<String, Object> getUserStatistics(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalLearningPaths", learningPathRepository.countByUserId(userId));
        stats.put("activeLearningPaths", learningPathRepository.countByUserIdAndStatus(
                userId, LearningPath.Status.ACTIVE));
        stats.put("completedLearningPaths", learningPathRepository.countByUserIdAndStatus(
                userId, LearningPath.Status.COMPLETED));
        stats.put("totalSubmissions", submissionRepository.countByUserId(userId));

        return stats;
    }

    /**
     * Validate code against test cases
     *
     * @param code Code to validate
     * @param testCases Test cases
     * @return List of TestResults
     */
    private List<ExerciseSubmission.TestResult> validateCode(String code, List<Exercise.TestCase> testCases) {
        // This is a simplified version - in production, you would:
        // 1. Compile the code
        // 2. Run against test cases
        // 3. Capture output
        // For now, return mock results
        return testCases.stream()
                .map(tc -> ExerciseSubmission.TestResult.builder()
                        .testName(tc.getName())
                        .passed(false) // Would be actual test result
                        .expectedOutput(tc.getExpectedOutput())
                        .actualOutput("Not implemented")
                        .build())
                .toList();
    }

    /**
     * Map exercise data from agent result to Exercise entity
     *
     * @param data Exercise data
     * @return Exercise
     */
    private Exercise mapToExercise(Map<String, Object> data) {
        Exercise exercise = Exercise.builder()
                .title((String) data.get("title"))
                .description((String) data.get("description"))
                .difficulty(Exercise.Difficulty.valueOf((String) data.getOrDefault("difficulty", "MEDIUM")))
                .skillTag((String) data.get("skillTag"))
                .language((String) data.get("language"))
                .starterCode((String) data.get("starterCode"))
                .solutionCode((String) data.get("solutionCode"))
                .estimatedMinutes((Integer) data.getOrDefault("estimatedMinutes", 30))
                .isPublished(false)
                .creatorId(1L) // System-generated
                .build();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> testCasesData = (List<Map<String, Object>>) data.get("testCases");
        if (testCasesData != null) {
            List<Exercise.TestCase> testCases = testCasesData.stream()
                    .map(tc -> Exercise.TestCase.builder()
                            .name((String) tc.get("name"))
                            .input((String) tc.get("input"))
                            .expectedOutput((String) tc.get("expectedOutput"))
                            .isHidden((Boolean) tc.getOrDefault("isHidden", false))
                            .build())
                    .toList();
            exercise.setTestCases(testCases);
        }

        @SuppressWarnings("unchecked")
        List<String> hints = (List<String>) data.get("hints");
        exercise.setHints(hints);

        return exercise;
    }
}
