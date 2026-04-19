package com.company.kb.filter;

import com.company.kb.config.RateLimitConfig;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * API访问频率限制过滤器
 *
 * 拦截所有传入请求，根据端点类型应用相应的频率限制
 * - 认证端点：10次/分钟
 * - 普通API：100次/分钟
 * - 只读端点：200次/分钟
 *
 * 超过限制的请求将返回HTTP 429状态码
 *
 * @author hjy
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter implements Filter {

    private final RateLimitConfig rateLimitConfig;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!rateLimitConfig.isRateLimitEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // 提取客户端标识
            String userId = httpRequest.getRemoteUser();
            String ipAddress = getClientIpAddress(httpRequest);
            String clientId = rateLimitConfig.resolveClientId(userId, ipAddress);

            // 获取请求路径
            String requestPath = httpRequest.getRequestURI();

            // 根据端点类型确定使用哪个限流器
            RateLimiter rateLimiter;
            String endpointType;

            if (isAuthEndpoint(requestPath)) {
                rateLimiter = rateLimitConfig.getAuthLimiter(clientId);
                endpointType = "认证";
            } else if (isReadOnlyEndpoint(httpRequest, requestPath)) {
                rateLimiter = rateLimitConfig.getReadLimiter(clientId);
                endpointType = "只读";
            } else {
                rateLimiter = rateLimitConfig.getApiLimiter(clientId);
                endpointType = "普通API";
            }

            // 尝试获取许可（非阻塞）
            boolean acquired = rateLimitConfig.tryAcquire(rateLimiter);

            if (!acquired) {
                log.warn("客户端超过频率限制 - 客户端: {}, 端点: {}, 类型: {}",
                    clientId, requestPath, endpointType);

                httpResponse.setStatus(429); // HTTP 429 Too Many Requests
                httpResponse.setContentType("application/json;charset=UTF-8");
                httpResponse.getWriter().write(
                    "{\"error\": \"访问频率过高，请稍后再试。Rate limit exceeded. Please try again later.\"}"
                );
                return;
            }

            // 添加频率限制相关的响应头
            addRateLimitHeaders(httpResponse, rateLimiter);

            // 继续处理请求
            chain.doFilter(request, response);

        } catch (Exception e) {
            log.error("频率限制过滤器发生错误", e);
            chain.doFilter(request, response);
        }
    }

    /**
     * 判断端点是否为认证端点
     *
     * @param requestPath 请求路径
     * @return true表示是认证端点
     */
    private boolean isAuthEndpoint(String requestPath) {
        return requestPath.contains("/api/auth/") ||
               requestPath.contains("/api/login") ||
               requestPath.contains("/api/register");
    }

    /**
     * 判断端点是否为只读端点（GET请求）
     *
     * @param request HTTP请求
     * @param requestPath 请求路径
     * @return true表示是只读端点
     */
    private boolean isReadOnlyEndpoint(HttpServletRequest request, String requestPath) {
        return "GET".equalsIgnoreCase(request.getMethod()) &&
               !requestPath.contains("/api/search") &&
               !requestPath.contains("/api/qa");
    }

    /**
     * 从请求中提取客户端IP地址
     *
     * 支持代理和负载均衡器场景，按优先级检查以下请求头：
     * X-Forwarded-For, X-Real-IP, Proxy-Client-IP等
     *
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 处理X-Forwarded-For中的多个IP
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * 向响应中添加频率限制相关的HTTP头
     *
     * @param response HTTP响应
     * @param rateLimiter 限流器
     */
    private void addRateLimitHeaders(HttpServletResponse response, RateLimiter rateLimiter) {
        // 获取当前可用许可数（近似值）
        double availablePermits = rateLimiter.getRate();
        response.setHeader("X-RateLimit-Limit", String.valueOf((int)(availablePermits * 60))); // 转换为每分钟
        response.setHeader("X-RateLimit-Remaining", "1"); // RateLimiter不提供精确的剩余许可数
        response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + 60000)); // 1分钟后重置
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("频率限制过滤器初始化完成");
    }

    @Override
    public void destroy() {
        log.info("频率限制过滤器已销毁");
    }
}