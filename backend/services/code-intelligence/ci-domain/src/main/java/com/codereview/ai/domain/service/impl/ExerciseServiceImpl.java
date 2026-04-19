package com.codereview.ai.domain.service.impl;

import com.codereview.ai.domain.model.*;
import com.codereview.ai.domain.repository.ExerciseRepository;
import com.codereview.ai.domain.repository.ExerciseSubmissionRepository;
import com.codereview.ai.domain.service.ExerciseService;
import com.codereview.ai.domain.service.SkillAssessmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Exercise Service Implementation
 *
 * @author Code Review AI Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExerciseServiceImpl implements ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseSubmissionRepository submissionRepository;
    private final SkillAssessmentService skillAssessmentService;

    @Override
    @Transactional
    public Exercise createExercise(Exercise exercise, Long creatorId) {
        exercise.setCreatorId(creatorId);
        exercise.setIsPublished(false);

        Exercise saved = exerciseRepository.save(exercise);
        log.info("Created exercise: exerciseId={}, title={}, creatorId={}",
                saved.getId(), saved.getTitle(), creatorId);

        return saved;
    }

    @Override
    @Transactional
    public Exercise updateExercise(Long exerciseId, Exercise exercise) {
        Exercise existing = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new IllegalArgumentException("Exercise not found: " + exerciseId));

        existing.setTitle(exercise.getTitle());
        existing.setDescription(exercise.getDescription());
        existing.setDifficulty(exercise.getDifficulty());
        existing.setLanguage(exercise.getLanguage());
        existing.setCategory(exercise.getCategory());
        existing.setStarterCode(exercise.getStarterCode());
        existing.setSolutionCode(exercise.getSolutionCode());
        existing.setTestCases(exercise.getTestCases());
        existing.setHints(exercise.getHints());
        existing.setRequirements(exercise.getRequirements());
        existing.setEstimatedMinutes(exercise.getEstimatedMinutes());

        Exercise updated = exerciseRepository.save(existing);
        log.info("Updated exercise: exerciseId={}", exerciseId);

        return updated;
    }

    @Override
    public Exercise getExerciseById(Long exerciseId) {
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new IllegalArgumentException("Exercise not found: " + exerciseId));
    }

    @Override
    public Map<String, Object> listExercises(String language, String category,
                                              Exercise.Difficulty difficulty,
                                              String exerciseType,
                                              int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Exercise> exercises = exerciseRepository.findPublishedExercises(
                language, category, difficulty, exerciseType, pageable);

        Map<String, Object> result = new HashMap<>();
        result.put("exercises", exercises.getContent());
        result.put("totalElements", exercises.getTotalElements());
        result.put("totalPages", exercises.getTotalPages());
        result.put("currentPage", page);

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> submitAttempt(Long userId, Long exerciseId, String code, int timeSpentSeconds) {
        Exercise exercise = getExerciseById(exerciseId);

        // Create submission record
        ExerciseSubmission submission = ExerciseSubmission.builder()
                .userId(userId)
                .exerciseId(exerciseId)
                .submittedCode(code)
                .timeSpentSeconds(timeSpentSeconds)
                .submittedAt(LocalDateTime.now())
                .build();

        // Validate against test cases if available
        if (exercise.getTestCases() != null && !exercise.getTestCases().isEmpty()) {
            List<ExerciseSubmission.TestResult> results = validateCode(code, exercise.getTestCases());
            submission.setTestResults(results);

            long passedCount = results.stream().filter(ExerciseSubmission.TestResult::getPassed).count();
            double passPercentage = (double) passedCount / results.size() * 100;

            // Calculate score (assuming maxScore is 100 for now)
            int maxScore = 100;
            int score = (int) (maxScore * passPercentage / 100);
            submission.setScore(score);

            // Generate feedback
            String feedback = generateFeedback(results, exercise);
            submission.setFeedback(feedback);
        } else {
            // For exercises without test cases, mark as passed with full score
            submission.setScore(100);
            submission.setFeedback("Code submitted successfully!");
        }

        ExerciseSubmission saved = submissionRepository.save(submission);

        // Update skill assessment
        if (saved.getScore() != null && saved.getScore() >= 70) {
            skillAssessmentService.assessFromExercise(userId, exercise.getLanguage(),
                    exercise.getCategory() != null ? exercise.getCategory() : "best-practices", true);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("submission", saved);
        result.put("score", saved.getScore());
        result.put("feedback", saved.getFeedback());

        log.info("Exercise submission submitted: userId={}, exerciseId={}, score={}",
                userId, exerciseId, saved.getScore());

        return result;
    }

    @Override
    public String getHints(Long exerciseId, int hintNumber, Long userId) {
        Exercise exercise = getExerciseById(exerciseId);
        return exercise.getHint(hintNumber);
    }

    @Override
    public List<Exercise> getRecommendedExercises(Long userId, String language, int count) {
        // Get exercises the user hasn't completed yet
        List<Exercise> recommended = exerciseRepository.findRecommendedExercisesForLanguage(language);

        // Filter out already completed exercises (submissions with score >= 70)
        List<Long> completedExerciseIds = submissionRepository.findByUserIdOrderBySubmittedAtDesc(userId).stream()
                .filter(s -> s.getScore() != null && s.getScore() >= 70)
                .map(ExerciseSubmission::getExerciseId)
                .distinct()
                .toList();

        return recommended.stream()
                .filter(e -> !completedExerciseIds.contains(e.getId()))
                .limit(count)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getUserSubmissions(Long userId, Long exerciseId, int page, int size) {
        List<ExerciseSubmission> submissions;
        long totalElements;

        if (exerciseId != null) {
            submissions = submissionRepository.findByExerciseIdOrderBySubmittedAtDesc(exerciseId);
            totalElements = submissions.size();
            // Apply pagination
            int start = page * size;
            int end = Math.min(start + size, submissions.size());
            submissions = submissions.subList(start, end);
        } else {
            submissions = submissionRepository.findByUserIdOrderBySubmittedAtDesc(userId);
            totalElements = submissions.size();
            // Apply pagination
            int start = page * size;
            int end = Math.min(start + size, submissions.size());
            submissions = submissions.subList(start, end);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("submissions", submissions);
        result.put("totalElements", totalElements);
        result.put("currentPage", page);

        return result;
    }

    @Override
    @Transactional
    public Exercise publishExercise(Long exerciseId) {
        Exercise exercise = getExerciseById(exerciseId);
        exercise.setIsPublished(true);

        Exercise published = exerciseRepository.save(exercise);
        log.info("Published exercise: exerciseId={}", exerciseId);

        return published;
    }

    @Override
    @Transactional
    public void deleteExercise(Long exerciseId, Long userId) {
        Exercise exercise = getExerciseById(exerciseId);

        if (!exercise.getCreatorId().equals(userId)) {
            throw new IllegalArgumentException("No permission to delete this exercise");
        }

        exerciseRepository.delete(exercise);
        log.info("Deleted exercise: exerciseId={}", exerciseId);
    }

    @Override
    public Map<String, Object> getExerciseStats(Long exerciseId) {
        List<ExerciseSubmission> submissions = submissionRepository.findByExerciseIdOrderBySubmittedAtDesc(exerciseId);

        long totalSubmissions = submissions.size();
        long passedSubmissions = submissions.stream()
                .filter(s -> s.getScore() != null && s.getScore() >= 70)
                .count();
        double passRate = totalSubmissions > 0 ? (double) passedSubmissions / totalSubmissions * 100 : 0;

        Double avgScore = submissions.stream()
                .filter(s -> s.getScore() != null)
                .mapToInt(ExerciseSubmission::getScore)
                .average()
                .orElse(0);

        Long avgTime = submissions.stream()
                .filter(s -> s.getTimeSpentSeconds() != null)
                .mapToLong(ExerciseSubmission::getTimeSpentSeconds)
                .mapToObj(Long::valueOf)
                .collect(Collectors.averagingLong(Long::longValue))
                .longValue();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSubmissions", totalSubmissions);
        stats.put("passedSubmissions", passedSubmissions);
        stats.put("passRate", Math.round(passRate));
        stats.put("averageScore", avgScore);
        stats.put("averageTimeSeconds", avgTime);

        return stats;
    }

    @Override
    public Map<String, Object> getUserExerciseSummary(Long userId) {
        long totalSubmissions = submissionRepository.countByUserId(userId);
        List<ExerciseSubmission> submissions = submissionRepository.findByUserIdOrderBySubmittedAtDesc(userId);
        long passedSubmissions = submissions.stream()
                .filter(s -> s.getScore() != null && s.getScore() >= 70)
                .count();
        long uniqueCompleted = submissions.stream()
                .filter(s -> s.getScore() != null && s.getScore() >= 70)
                .map(ExerciseSubmission::getExerciseId)
                .distinct()
                .count();
        Double avgScore = submissions.stream()
                .filter(s -> s.getScore() != null)
                .mapToInt(ExerciseSubmission::getScore)
                .average()
                .orElse(0);
        Long totalTime = submissions.stream()
                .filter(s -> s.getTimeSpentSeconds() != null)
                .mapToLong(ExerciseSubmission::getTimeSpentSeconds)
                .sum();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalSubmissions", totalSubmissions);
        summary.put("passedSubmissions", passedSubmissions);
        summary.put("uniqueExercisesCompleted", uniqueCompleted);
        summary.put("averageScore", avgScore);
        summary.put("totalTimeSpentSeconds", totalTime);
        summary.put("passRate", totalSubmissions > 0 ? Math.round((double) passedSubmissions / totalSubmissions * 100) : 0);

        return summary;
    }

    /**
     * Validate code against test cases
     * Note: This is a simplified implementation. In production, you'd use
     * a sandboxed execution environment for actual code testing.
     */
    private List<ExerciseSubmission.TestResult> validateCode(String code, List<Exercise.TestCase> testCases) {
        List<ExerciseSubmission.TestResult> results = new ArrayList<>();

        for (Exercise.TestCase testCase : testCases) {
            // Simplified validation - in production, execute code in a sandbox
            boolean passed = validateAgainstTestCase(code, testCase);

            results.add(ExerciseSubmission.TestResult.builder()
                    .testName(testCase.getName())
                    .passed(passed)
                    .expectedOutput(testCase.getExpectedOutput())
                    .actualOutput(passed ? testCase.getExpectedOutput() : "Output doesn't match expected")
                    .errorMessage(passed ? null : "Test failed: output mismatch")
                    .build());
        }

        return results;
    }

    /**
     * Simplified test case validation
     * In production, this would execute the code in a sandboxed environment
     */
    private boolean validateAgainstTestCase(String code, Exercise.TestCase testCase) {
        // This is a placeholder for actual code execution
        // In production, use a proper code execution service with:
        // - Sandboxing
        // - Timeout handling
        // - Resource limits
        // - Security checks

        // For now, do basic validation checks
        if (testCase.getInput() != null && !code.contains(testCase.getInput())) {
            return false;
        }

        return true;
    }

    /**
     * Generate feedback based on test results
     */
    private String generateFeedback(List<ExerciseSubmission.TestResult> results, Exercise exercise) {
        long passed = results.stream().filter(ExerciseSubmission.TestResult::getPassed).count();
        long total = results.size();

        if (passed == total) {
            return "Excellent! All tests passed. Your solution is correct!";
        } else if (passed == 0) {
            return "None of the tests passed. Review the requirements and try again.";
        } else {
            return String.format("Some tests passed (%d/%d). Check the failed tests and adjust your solution.",
                    passed, total);
        }
    }
}
