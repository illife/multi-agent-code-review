package com.codereview.auth.core.service;

import com.codereview.auth.core.domain.User;
import com.codereview.auth.core.repository.UserRepository;
import com.think.platform.shared.common.dto.RegisterRequest;
import com.think.platform.shared.common.exception.BusinessException;
import com.think.platform.shared.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User Service - Auth Service
 *
 * 负责用户管理业务逻辑（简化版，使用Role枚举）
 *
 * @author Auth Service Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordService passwordService;

    /**
     * 用户注册
     */
    @Transactional
    public User registerUser(RegisterRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("邮箱已被使用");
        }

        // 创建新用户（默认角色为USER）
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordService.encodePassword(request.getPassword()))
                .fullName(request.getFullName())
                .role(User.Role.USER)  // 使用枚举而非Role实体
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getUsername());

        return savedUser;
    }

    /**
     * 根据ID获取用户
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }

    /**
     * 根据用户名获取用户
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }

    /**
     * 根据邮箱获取用户
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }

    /**
     * 分页查询用户
     */
    public Page<User> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public User updateUser(Long id, String fullName) {
        User user = getUserById(id);
        user.setFullName(fullName);
        User updatedUser = userRepository.save(user);
        log.info("User updated: {}", updatedUser.getUsername());
        return updatedUser;
    }

    /**
     * 更新用户角色
     */
    @Transactional
    public void updateUserRole(Long id, User.Role role) {
        User user = getUserById(id);
        user.setRole(role);
        userRepository.save(user);
        log.info("User role updated: {} -> {}", user.getUsername(), role);
    }

    /**
     * 更新最后登录时间
     */
    @Transactional
    public void updateLastLoginTime(Long userId) {
        User user = getUserById(userId);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * 启用用户
     */
    @Transactional
    public void enableUser(Long id) {
        User user = getUserById(id);
        user.setIsActive(true);
        userRepository.save(user);
        log.info("User enabled: {}", user.getUsername());
    }

    /**
     * 禁用用户
     */
    @Transactional
    public void disableUser(Long id) {
        User user = getUserById(id);
        user.setIsActive(false);
        userRepository.save(user);
        log.info("User disabled: {}", user.getUsername());
    }

    /**
     * 删除用户
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
        log.info("User deleted: {}", user.getUsername());
    }

    /**
     * 根据角色查询用户
     */
    public List<User> getUsersByRole(User.Role role) {
        return userRepository.findByRole(role);
    }

    /**
     * 检查用户是否激活
     */
    public boolean isUserActive(String username) {
        User user = getUserByUsername(username);
        return user.getIsActive();
    }

    /**
     * 检查用户是否为管理员
     */
    public boolean isUserAdmin(String username) {
        User user = getUserByUsername(username);
        return user.getRole() == User.Role.ADMIN;
    }

    /**
     * 提升用户为管理员
     */
    @Transactional
    public void promoteToAdmin(Long id) {
        updateUserRole(id, User.Role.ADMIN);
    }

    /**
     * 降级为普通用户
     */
    @Transactional
    public void demoteToUser(Long id) {
        updateUserRole(id, User.Role.USER);
    }
}
