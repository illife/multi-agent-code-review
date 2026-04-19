package com.codereview.ai.domain.repository;

import com.codereview.ai.domain.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Project Repository
 *
 * @author Code Review AI Team
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * Find all projects for a specific user
     */
    Page<Project> findByUserId(Long userId, Pageable pageable);

    /**
     * Find projects by user and status
     */
    List<Project> findByUserIdAndStatus(Long userId, Project.ProjectStatus status);

    /**
     * Find project by ID and user (for access control)
     */
    Optional<Project> findByIdAndUserId(Long id, Long userId);

    /**
     * Find public projects
     */
    @Query("SELECT p FROM Project p WHERE p.visibility = 'PUBLIC'")
    Page<Project> findPublicProjects(Pageable pageable);

    /**
     * Count projects by user
     */
    long countByUserId(Long userId);

    /**
     * Count projects by user and status
     */
    long countByUserIdAndStatus(Long userId, Project.ProjectStatus status);

    /**
     * Find projects by status
     */
    List<Project> findByStatus(Project.ProjectStatus status);
}
