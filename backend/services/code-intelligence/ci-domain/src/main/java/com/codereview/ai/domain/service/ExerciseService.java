package com.codereview.ai.domain.service;

import com.codereview.ai.domain.model.Exercise;

import java.util.List;
import java.util.Map;

/**
 * Exercise Service Interface
 *
 * Provides exercise and practice problem functionality
 *
 * @author Code Review AI Team
 */
public interface ExerciseService {

    /**
     * Create new exercise
     *
     * @param exercise Exercise to create
     * @param creatorId User ID of exercise creator
     * @return Created exercise
     */
    Exercise createExercise(Exercise exercise, Long creatorId);

    /**
     * Update existing exercise
     *
     * @param exerciseId Exercise ID
     * @param exercise Updated exercise data
     * @return Updated exercise
     */
    Exercise updateExercise(Long exerciseId, Exercise exercise);

    /**
     * Get exercise by ID
     *
     * @param exerciseId Exercise ID
     * @return Exercise
     */
    Exercise getExerciseById(Long exerciseId);

    /**
     * List exercises with filters
     *
     * @param language Filter by language (optional)
     * @param category Filter by category (optional)
     * @param difficulty Filter by difficulty (optional)
     * @param skillTag Filter by skill tag (optional)
     * @param pageable Pagination parameters
     * @return Paginated exercise list
     */
    Map<String, Object> listExercises(String language, String category,
                                        Exercise.Difficulty difficulty,
                                        String skillTag,
                                        int page, int size);

    /**
     * Submit exercise attempt
     *
     * @param userId User ID
     * @param exerciseId Exercise ID
     * @param code Submitted code
     * @param timeSpentSeconds Time spent in seconds
     * @return Attempt result with feedback
     */
    Map<String, Object> submitAttempt(Long userId, Long exerciseId, String code, int timeSpentSeconds);

    /**
     * Get hints for exercise
     *
     * @param exerciseId Exercise ID
     * @param hintNumber Hint number (1-indexed)
     * @param userId User ID (for tracking hints used)
     * @return Hint text or null if not available
     */
    String getHints(Long exerciseId, int hintNumber, Long userId);

    /**
     * Get recommended exercises for user
     *
     * @param userId User ID
     * @param language Programming language
     * @param count Number of recommendations
     * @return List of recommended exercises
     */
    List<Exercise> getRecommendedExercises(Long userId, String language, int count);

    /**
     * Get user's exercise submissions
     *
     * @param userId User ID
     * @param exerciseId Exercise ID (optional)
     * @param page Page number
     * @param size Page size
     * @return Paginated submissions
     */
    Map<String, Object> getUserSubmissions(Long userId, Long exerciseId, int page, int size);

    /**
     * Publish exercise
     *
     * @param exerciseId Exercise ID
     * @return Published exercise
     */
    Exercise publishExercise(Long exerciseId);

    /**
     * Delete exercise
     *
     * @param exerciseId Exercise ID
     * @param userId User ID (for permission check)
     */
    void deleteExercise(Long exerciseId, Long userId);

    /**
     * Get exercise statistics
     *
     * @param exerciseId Exercise ID
     * @return Statistics map
     */
    Map<String, Object> getExerciseStats(Long exerciseId);

    /**
     * Get user's exercise summary
     *
     * @param userId User ID
     * @return Summary map
     */
    Map<String, Object> getUserExerciseSummary(Long userId);
}
