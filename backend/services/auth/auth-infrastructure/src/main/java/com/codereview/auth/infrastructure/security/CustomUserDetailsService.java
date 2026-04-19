package com.codereview.auth.infrastructure.security;

import com.codereview.auth.core.domain.User;
import com.codereview.auth.core.repository.UserRepository;
import com.think.platform.shared.security.SharedUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Custom User Details Service - Auth Service
 *
 * 从数据库加载用户信息并转换为 UserDetails（简化版，使用Role枚举）
 *
 * @author Auth Service Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        // 获取用户角色（从枚举转换为Set<String>）
        Set<String> roles = Set.of(user.getRole().name());

        log.debug("User loaded with roles: {}", roles);

        // 创建 SharedUserDetails
        return SharedUserDetails.create(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                roles,
                user.getIsActive(),
                user.getIsActive()
        );
    }
}
