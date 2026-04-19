package com.codereview.auth.core.repository;

import com.codereview.auth.core.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * User Repository - Auth Service
 *
 * 简化版：使用Role枚举而非@ManyToMany关系
 *
 * @author Auth Service Team
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // 根据角色枚举查询用户
    List<User> findByRole(User.Role role);

    // 检查用户是否具有指定角色
    default boolean hasRole(Long userId, User.Role role) {
        return findById(userId).map(user -> user.getRole() == role).orElse(false);
    }
}
