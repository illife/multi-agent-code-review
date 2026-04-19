package com.codereview.ai.domain.repository;

import com.codereview.ai.domain.model.LearningPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for LearningPath entity
 *
 * @author Code Intelligence Service Team
 */
@Repository
public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {

    /**
     * Find all learning paths by user ID
     */
    List<LearningPath> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Count learning paths by user ID
     */
    long countByUserId(Long userId);

    /**
     * Find learning paths by user ID and status
     */
    List<LearningPath> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, LearningPath.Status status);

    /**
     * Find learning paths by user ID and target skill
     */
    List<LearningPath> findByUserIdAndTargetSkillOrderByCreatedAtDesc(Long userId, String targetSkill);

    /**
     * Find active learning path by user ID and target skill
     */
    Optional<LearningPath> findByUserIdAndTargetSkillAndStatus(Long userId, String targetSkill, LearningPath.Status status);

    /**
     * Find learning paths by task ID
     */
    List<LearningPath> findByTaskId(Long taskId);

    /**
     * Count learning paths by user ID and status
     */
    long countByUserIdAndStatus(Long userId, LearningPath.Status status);

    /**
     * Find learning paths with progress threshold
     */
    @Query("SELECT lp FROM LearningPath lp WHERE lp.userId = :userId AND lp.progress >= :minProgress")
    List<LearningPath> findByUserIdAndProgressGreaterThanEqual(@Param("userId") Long userId, @Param("minProgress") Integer minProgress);

    /**
     * Find completed learning paths
     */
    List<LearningPath> findByUserIdAndStatusOrderByCompletedAtDesc(Long userId, LearningPath.Status status);
}
