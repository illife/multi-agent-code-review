package com.codereview.ai.domain.repository;

import com.codereview.ai.domain.model.ProjectReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ProjectReport Repository
 *
 * @author Code Review AI Team
 */
@Repository
public interface ProjectReportRepository extends JpaRepository<ProjectReport, Long> {

    /**
     * Find report by project ID
     */
    Optional<ProjectReport> findByProjectId(Long projectId);

    /**
     * Delete report by project ID
     */
    void deleteByProjectId(Long projectId);

    /**
     * Check if report exists for project
     */
    boolean existsByProjectId(Long projectId);
}
