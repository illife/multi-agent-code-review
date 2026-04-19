package com.codereview.ai.domain.repository;

import com.codereview.ai.domain.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Role Repository for Code Review AI
 *
 * @author Code Review AI Team
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Find role by name
     */
    Optional<Role> findByName(String name);

    /**
     * Check if role exists by name
     */
    boolean existsByName(String name);
}
