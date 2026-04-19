package com.codereview.ai.domain.repository;

import com.codereview.ai.domain.model.ExerciseSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ExerciseSubmission entity
 *
 * @author Code Intelligence Service Team
 */
@Repository
public interface ExerciseSubmissionRepository extends JpaRepository<ExerciseSubmission, Long> {

    /**
     * Find all submissions by user ID
     */
    List<ExerciseSubmission> findByUserIdOrderBySubmittedAtDesc(Long userId);

    /**
     * Find all submissions by exercise ID
     */
    List<ExerciseSubmission> findByExerciseIdOrderBySubmittedAtDesc(Long exerciseId);

    /**
     * Find submissions by user ID and exercise ID
     */
    List<ExerciseSubmission> findByUserIdAndExerciseIdOrderBySubmittedAtDesc(Long userId, Long exerciseId);

    /**
     * Find latest submission by user ID and exercise ID
     */
    Optional<ExerciseSubmission> findFirstByUserIdAndExerciseIdOrderBySubmittedAtDesc(Long userId, Long exerciseId);

    /**
     * Find best submission by user ID and exercise ID (highest score)
     */
    Optional<ExerciseSubmission> findFirstByUserIdAndExerciseIdOrderByScoreDesc(Long userId, Long exerciseId);

    /**
     * Count submissions by user ID
     */
    long countByUserId(Long userId);

    /**
     * Count submissions by exercise ID
     */
    long countByExerciseId(Long exerciseId);

    /**
     * Calculate average score for an exercise
     */
    @Query("SELECT AVG(s.score) FROM ExerciseSubmission s WHERE s.exerciseId = :exerciseId AND s.score IS NOT NULL")
    Double getAverageScoreForExercise(@Param("exerciseId") Long exerciseId);

    /**
     * Find passed submissions for a user
     */
    @Query("SELECT s FROM ExerciseSubmission s WHERE s.userId = :userId AND s.score >= :passingScore")
    List<ExerciseSubmission> findPassedSubmissions(@Param("userId") Long userId, @Param("passingScore") Integer passingScore);

    /**
     * Count unique users who submitted an exercise
     */
    @Query("SELECT COUNT(DISTINCT s.userId) FROM ExerciseSubmission s WHERE s.exerciseId = :exerciseId")
    long countUniqueUsersForExercise(@Param("exerciseId") Long exerciseId);
}
