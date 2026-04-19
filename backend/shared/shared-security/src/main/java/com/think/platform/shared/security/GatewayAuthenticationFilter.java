package com.think.platform.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;

/**
 * Gateway Authentication Filter
 *
 * 信任来自 API Gateway 的请求，直接从请求头读取用户信息
 * Gateway 已经验证过 JWT，并添加了 X-User-Id, X-Username, X-User-Role 请求头
 *
 * @author Platform Team
 */
@Slf4j
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-Username";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // OPTIONS 预检请求直接放行，不进行任何认证处理
        // CORS 预检请求由 Gateway 处理，后端服务不需要处理
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = request.getHeader(USER_ID_HEADER);
        String username = request.getHeader(USERNAME_HEADER);
        String role = request.getHeader(USER_ROLE_HEADER);

        // 如果请求包含 Gateway 添加的用户信息头，则信任并设置认证
        if (userId != null && username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 使用静态工厂方法创建 SharedUserDetails
            String normalizedRole = role != null ? role : "USER";
            SharedUserDetails userDetails = SharedUserDetails.create(
                    Long.parseLong(userId),
                    username,
                    null,  // email
                    null,  // password (不需要)
                    Collections.singleton(normalizedRole)
            );

            // 创建认证对象
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            // 设置到 SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Set authentication from Gateway headers for user: {} (ID: {}, Role: {})", username, userId, normalizedRole);
        }

        filterChain.doFilter(request, response);
    }
}
