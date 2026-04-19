package com.codereview.ai.security;

import com.think.platform.shared.security.SharedUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 安全工具类
 *
 * 提供获取当前用户信息的便捷方法
 * 从 SecurityContext 获取用户信息（由 GatewayAuthenticationFilter 设置）
 *
 * @author Code Review AI Team
 */
@Slf4j
@Component
public class SecurityUtils {

    /**
     * 从 SecurityContext 获取当前用户ID
     * Gateway 已经验证过 JWT 并设置了 SecurityContext
     *
     * @return 用户ID，如果无法获取抛出异常
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SharedUserDetails) {
            SharedUserDetails userDetails = (SharedUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getId();
            log.debug("Retrieved userId from SecurityContext: {}", userId);
            return userId;
        }
        throw new IllegalStateException("User not authenticated");
    }

    /**
     * 从 SecurityContext 获取当前用户ID
     * 支持 HttpServletRequest 参数（兼容旧代码）
     *
     * @param request HTTP 请求（忽略，使用 SecurityContext）
     * @return 用户ID
     */
    public Long getCurrentUserId(HttpServletRequest request) {
        return getCurrentUserId();
    }

    /**
     * 从 SecurityContext 获取当前用户ID
     * 带默认值支持
     *
     * @param request HTTP 请求（忽略，使用 SecurityContext）
     * @param defaultUserId 默认用户ID
     * @return 用户ID
     */
    public Long getCurrentUserId(HttpServletRequest request, Long defaultUserId) {
        try {
            return getCurrentUserId();
        } catch (IllegalStateException e) {
            log.warn("User not authenticated, using default userId: {}", defaultUserId);
            return defaultUserId;
        }
    }

    /**
     * 检查请求是否已认证
     *
     * @param request HTTP 请求（忽略，使用 SecurityContext）
     * @return 是否已认证
     */
    public boolean isAuthenticated(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof SharedUserDetails;
    }

    /**
     * 从 SecurityContext 获取当前用户名
     *
     * @return 用户名
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SharedUserDetails) {
            SharedUserDetails userDetails = (SharedUserDetails) authentication.getPrincipal();
            return userDetails.getUsername();
        }
        throw new IllegalStateException("User not authenticated");
    }

    /**
     * 从 SecurityContext 获取当前用户详细信息
     *
     * @return SharedUserDetails
     */
    public SharedUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SharedUserDetails) {
            return (SharedUserDetails) authentication.getPrincipal();
        }
        throw new IllegalStateException("User not authenticated");
    }
}
