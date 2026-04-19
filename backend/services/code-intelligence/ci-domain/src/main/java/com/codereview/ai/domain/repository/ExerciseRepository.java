package com.codereview.ai.domain.repository;

import com.codereview.ai.domain.model.Exercise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Exercise Repository
 *
 * @author Code Review AI Team
 */
@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    Page<Exercise> findByIsPublished(Boolean isPublished, Pageable pageable);

    Page<Exercise> findByDifficulty(Exercise.Difficulty difficulty, Pageable pageable);

    Page<Exercise> findByLanguage(String language, Pageable pageable);

    Page<Exercise> findByCategory(String category, Pageable pageable);

    Page<Exercise> findBySkillTag(String skillTag, Pageable pageable);

    Page<Exercise> findByCreatorId(Long creatorId, Pageable pageable);

    @Query("SELECT e FROM Exercise e WHERE e.isPublished = true " +
           "AND (:language IS NULL OR e.language = :language) " +
           "AND (:category IS NULL OR e.category = :category) " +
           "AND (:difficulty IS NULL OR e.difficulty = :difficulty) " +
           "AND (:skillTag IS NULL OR e.skillTag = :skillTag)")
    Page<Exercise> findPublishedExercises(
            @Param("language") String language,
            @Param("category") String category,
            @Param("difficulty") Exercise.Difficulty difficulty,
            @Param("skillTag") String skillTag,
            Pageable pageable);

    @Query("SELECT e FROM Exercise e WHERE e.isPublished = true " +
           "AND e.language = :language " +
           "ORDER BY " +
           "CASE e.difficulty " +
           "  WHEN 'EASY' THEN 1 " +
           "  WHEN 'MEDIUM' THEN 2 " +
           "  WHEN 'HARD' THEN 3 " +
           "  ELSE 4 " +
           "END")
    List<Exercise> findRecommendedExercisesForLanguage(@Param("language") String language);

    @Query("SELECT e FROM Exercise e WHERE e.isPublished = true " +
           "AND e.language = :language " +
           "AND e.difficulty = :difficulty " +
           "AND e.id NOT IN (" +
           "  SELECT es.exerciseId FROM ExerciseSubmission es WHERE es.userId = :userId" +
           ")")
    List<Exercise> findUncompletedExercisesForUser(
            @Param("userId") Long userId,
            @Param("language") String language,
            @Param("difficulty") Exercise.Difficulty difficulty);

    long countByIsPublished(Boolean isPublished);

    long countByCreatorId(Long creatorId);

    @Query("SELECT e.difficulty, COUNT(e) FROM Exercise e " +
           "WHERE e.isPublished = true GROUP BY e.difficulty")
    List<Object[]> countByDifficulty();
}
