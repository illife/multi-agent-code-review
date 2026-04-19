package com.codereview.ai.domain.repository;

import com.codereview.ai.domain.model.CodeIssue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Code Issue Repository
 *
 * @author Code Review AI Team
 */
@Repository
public interface CodeIssueRepository extends JpaRepository<CodeIssue, Long> {

    Page<CodeIssue> findByReviewId(Long reviewId, Pageable pageable);

    @Query("SELECT i FROM CodeIssue i WHERE i.reviewId = :reviewId ORDER BY " +
           "CASE i.severity " +
           "  WHEN 'CRITICAL' THEN 1 " +
           "  WHEN 'HIGH' THEN 2 " +
           "  WHEN 'MEDIUM' THEN 3 " +
           "  WHEN 'LOW' THEN 4 " +
           "  ELSE 5 " +
           "END")
    List<CodeIssue> findByReviewIdOrderBySeverity(@Param("reviewId") Long reviewId);

    Page<CodeIssue> findBySeverity(CodeIssue.Severity severity, Pageable pageable);

    Page<CodeIssue> findByAgentType(String agentType, Pageable pageable);

    @Query("SELECT i FROM CodeIssue i WHERE i.reviewId = :reviewId AND i.isResolved = false")
    List<CodeIssue> findUnresolvedIssuesByReviewId(@Param("reviewId") Long reviewId);

    long countByReviewId(Long reviewId);

    long countByReviewIdAndSeverity(Long reviewId, CodeIssue.Severity severity);

    long countByReviewIdAndCategory(Long reviewId, String category);

    void deleteByReviewId(Long reviewId);
}
