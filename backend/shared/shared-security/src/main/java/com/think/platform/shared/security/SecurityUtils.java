package com.think.platform.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Security Utilities
 * 便捷的 Security 工具类
 *
 * @author AI Code Mentor Team
 */
@Slf4j
public class SecurityUtils {

    /**
     * 获取当前认证信息
     */
    public static Optional<Authentication> getCurrentAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * 获取当前用户 ID
     */
    public static Optional<Long> getCurrentUserId() {
        return getCurrentAuthentication()
                .flatMap(auth -> {
                    Object principal = auth.getPrincipal();
                    if (principal instanceof SharedUserDetails) {
                        return Optional.of(((SharedUserDetails) principal).getId());
                    }
                    // 兼容其他 UserDetails 实现
                    if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                        String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
                        // 可以通过 username 查询获取 userId，这里简化处理
                        log.debug("Current user username: {}", username);
                    }
                    return Optional.empty();
                });
    }

    /**
     * 获取当前用户名
     */
    public static Optional<String> getCurrentUsername() {
        return getCurrentAuthentication()
                .map(Authentication::getName);
    }

    /**
     * 获取当前用户的所有角色
     */
    public static Set<String> getCurrentUserRoles() {
        return getCurrentAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(authORITY -> {
                            // 移除 ROLE_ 前缀
                            if (authORITY.startsWith("ROLE_")) {
                                return authORITY.substring(5);
                            }
                            return authORITY;
                        })
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    /**
     * 检查当前用户是否有指定角色
     */
    public static boolean hasRole(String role) {
        return getCurrentAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .anyMatch(authORITY -> {
                            String authority = authORITY.getAuthority();
                            // 支持 "ROLE_ADMIN" 和 "ADMIN" 两种格式
                            return authority.equals("ROLE_" + role) || authority.equals(role);
                        }))
                .orElse(false);
    }

    /**
     * 检查当前用户是否有任意一个指定角色
     */
    public static boolean hasAnyRole(String... roles) {
        return getCurrentAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .anyMatch(authORITY -> {
                            String authority = authORITY.getAuthority();
                            for (String role : roles) {
                                if (authority.equals("ROLE_" + role) || authority.equals(role)) {
                                    return true;
                                }
                            }
                            return false;
                        }))
                .orElse(false);
    }

    /**
     * 检查当前用户是否已认证
     */
    public static boolean isAuthenticated() {
        return getCurrentAuthentication()
                .map(auth -> auth.isAuthenticated() &&
                        !"anonymousUser".equals(auth.getPrincipal()))
                .orElse(false);
    }

    /**
     * 检查当前用户是否为匿名用户
     */
    public static boolean isAnonymous() {
        return !isAuthenticated();
    }

    /**
     * 清除当前认证信息
     */
    public static void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 从 Authentication 中获取 Long 类型的 userId
     * 兼容多种 UserDetails 实现
     */
    public static Long extractUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        // 支持 SharedUserDetails
        if (principal instanceof SharedUserDetails) {
            return ((SharedUserDetails) principal).getId();
        }

        // 支持 CustomUserDetails (codereview-api)
        try {
            if (principal.getClass().getName().contains("CustomUserDetails")) {
                try {
                    java.lang.reflect.Method getIdMethod = principal.getClass().getMethod("getId");
                    Object id = getIdMethod.invoke(principal);
                    if (id instanceof Long) {
                        return (Long) id;
                    } else if (id instanceof Integer) {
                        return ((Integer) id).longValue();
                    } else if (id instanceof String) {
                        return Long.parseLong((String) id);
                    }
                } catch (Exception e) {
                    log.error("Failed to get userId via reflection", e);
                }
            }
        } catch (Exception e) {
            log.debug("Not a CustomUserDetails type");
        }

        log.warn("Unsupported principal type: {}", principal.getClass().getName());
        return null;
    }
}
