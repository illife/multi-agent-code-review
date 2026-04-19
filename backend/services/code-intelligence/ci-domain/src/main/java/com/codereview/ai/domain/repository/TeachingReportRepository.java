package com.codereview.ai.domain.repository;

import com.codereview.ai.domain.model.TeachingReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Teaching Report Repository
 *
 * Repository for TeachingReport entities
 *
 * @author Code Review AI Team
 */
@Repository
public interface TeachingReportRepository extends JpaRepository<TeachingReport, Long> {

    /**
     * Find teaching report by review ID
     *
     * @param reviewId The review ID
     * @return Optional containing the teaching report if found
     */
    Optional<TeachingReport> findByReviewId(Long reviewId);

    /**
     * Delete teaching report by review ID
     *
     * @param reviewId The review ID
     */
    void deleteByReviewId(Long reviewId);
}
