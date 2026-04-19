package com.codereview.ai.domain.repository;

import com.codereview.ai.domain.model.ProjectFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ProjectFile Repository
 *
 * @author Code Review AI Team
 */
@Repository
public interface ProjectFileRepository extends JpaRepository<ProjectFile, Long> {

    /**
     * Find all files for a project
     */
    List<ProjectFile> findByProjectId(Long projectId);

    /**
     * Find files by project and analysis status
     */
    List<ProjectFile> findByProjectIdAndIsAnalyzed(Long projectId, boolean isAnalyzed);

    /**
     * Find file by review ID
     */
    Optional<ProjectFile> findByReviewId(Long reviewId);

    /**
     * Count files for a project
     */
    long countByProjectId(Long projectId);

    /**
     * Count analyzed files for a project
     */
    long countByProjectIdAndIsAnalyzed(Long projectId, boolean isAnalyzed);

    /**
     * Find files ordered by priority
     */
    @Query("SELECT pf FROM ProjectFile pf WHERE pf.projectId = :projectId ORDER BY pf.analysisPriority DESC")
    List<ProjectFile> findByProjectIdOrderByPriority(@Param("projectId") Long projectId);

    /**
     * Find pending files for a project
     */
    @Query("SELECT pf FROM ProjectFile pf WHERE pf.projectId = :projectId AND pf.isAnalyzed = false ORDER BY pf.analysisPriority DESC")
    List<ProjectFile> findPendingFilesByProjectId(@Param("projectId") Long projectId);

    /**
     * Delete all files for a project
     */
    void deleteByProjectId(Long projectId);
}
