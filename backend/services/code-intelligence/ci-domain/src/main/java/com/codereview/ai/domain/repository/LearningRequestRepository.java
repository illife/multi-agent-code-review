package com.codereview.ai.domain.repository;

import com.codereview.ai.domain.model.LearningRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for LearningRequest entity
 *
 * @author Code Intelligence Service Team
 */
@Repository
public interface LearningRequestRepository extends JpaRepository<LearningRequest, Long> {

    /**
     * Find all learning requests by user ID
     */
    List<LearningRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find learning requests by user ID and status
     */
    List<LearningRequest> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, LearningRequest.RequestStatus status);

    /**
     * Find learning requests by task ID
     */
    List<LearningRequest> findByTaskId(Long taskId);

    /**
     * Find learning requests by target skill
     */
    List<LearningRequest> findByTargetSkillOrderByCreatedAtDesc(String targetSkill);

    /**
     * Find learning requests by user ID and request type
     */
    List<LearningRequest> findByUserIdAndRequestTypeOrderByCreatedAtDesc(Long userId, LearningRequest.RequestType requestType);

    /**
     * Find pending learning requests
     */
    List<LearningRequest> findByStatusOrderByCreatedAtAsc(LearningRequest.RequestStatus status);

    /**
     * Count learning requests by user ID
     */
    long countByUserId(Long userId);

    /**
     * Count learning requests by user ID and status
     */
    long countByUserIdAndStatus(Long userId, LearningRequest.RequestStatus status);
}
