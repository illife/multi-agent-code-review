package com.codereview.ai.domain.repository;

import com.codereview.ai.domain.model.CodeReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Code Review Repository
 *
 * @author Code Review AI Team
 */
@Repository
public interface CodeReviewRepository extends JpaRepository<CodeReview, Long> {

    Page<CodeReview> findByUserId(Long userId, Pageable pageable);

    Page<CodeReview> findByStatus(CodeReview.ReviewStatus status, Pageable pageable);

    Page<CodeReview> findByLanguage(String language, Pageable pageable);

    @Query("SELECT r FROM CodeReview r WHERE r.visibility = 'PUBLIC'")
    Page<CodeReview> findPublicReviews(Pageable pageable);

    Page<CodeReview> findByUserIdAndStatus(Long userId, CodeReview.ReviewStatus status, Pageable pageable);

    @Query("SELECT r.id FROM CodeReview r WHERE r.userId = :userId OR r.visibility = 'PUBLIC'")
    List<Long> findAccessibleReviewIds(@Param("userId") Long userId);

    long countByStatus(CodeReview.ReviewStatus status);

    long countByLanguage(String language);
}
