package com.think.platform.gateway.config;

import com.think.platform.gateway.filter.AdminRoleFilter;
import com.think.platform.gateway.filter.AuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final AuthenticationFilter authenticationFilter;
    private final AdminRoleFilter adminRoleFilter;

    @Value("${services.endpoints.auth-service.url:http://auth-api:8083}")
    private String authServiceUrl;

    @Value("${services.endpoints.code-intelligence-service.url:http://code-intelligence-api:8081}")
    private String codeIntelligenceUrl;

    @Value("${services.endpoints.knowledge-mentor-service.url:http://knowledge-mentor-api:8080}")
    private String knowledgeMentorUrl;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth Service Routes (public endpoints)
                .route("auth-public", r -> r
                        .path("/api/auth/login", "/api/auth/register", "/api/auth/refresh")
                        .filters(f -> f
                                .stripPrefix(0)
                                .removeRequestHeader("Cookie"))
                        .uri(authServiceUrl))

                // Auth Service Routes (protected)
                .route("auth-api", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(authenticationFilter))
                        .uri(authServiceUrl))

                // User API (protected + admin)
                .route("user-api", r -> r
                        .path("/api/users/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(authenticationFilter)
                                .filter(adminRoleFilter))
                        .uri(authServiceUrl))

                // Code Intelligence Service Routes (when service starts)
                .route("agent-api", r -> r
                        .path("/api/agent/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(authenticationFilter))
                        .uri(codeIntelligenceUrl))

                .route("learning-api", r -> r
                        .path("/api/learning/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(authenticationFilter))
                        .uri(codeIntelligenceUrl))

                .route("project-api", r -> r
                        .path("/api/project/**", "/api/projects/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(authenticationFilter))
                        .uri(codeIntelligenceUrl))

                .route("review-api", r -> r
                        .path("/api/review/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(authenticationFilter))
                        .uri(codeIntelligenceUrl))

                .route("analysis-api", r -> r
                        .path("/api/analysis/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(authenticationFilter))
                        .uri(codeIntelligenceUrl))

                .route("teaching-api", r -> r
                        .path("/api/teaching/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(authenticationFilter))
                        .uri(codeIntelligenceUrl))

                .route("architecture-api", r -> r
                        .path("/api/architecture/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(authenticationFilter))
                        .uri(codeIntelligenceUrl))

                // Knowledge Mentor Service Routes
                // 使用 stripPrefix(1) 去掉 /api 前缀，后端控制器不需要 /api 前缀
                .route("document-api", r -> r
                        .path("/api/documents/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(authenticationFilter))
                        .uri(knowledgeMentorUrl))

                .route("qa-api", r -> r
                        .path("/api/qa/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(authenticationFilter))
                        .uri(knowledgeMentorUrl))

                .route("search-api", r -> r
                        .path("/api/search/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(authenticationFilter))
                        .uri(knowledgeMentorUrl))

                .route("lessons-api", r -> r
                        .path("/api/lessons/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(authenticationFilter))
                        .uri(knowledgeMentorUrl))

                .route("exercises-api", r -> r
                        .path("/api/exercises/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(authenticationFilter))
                        .uri(knowledgeMentorUrl))

                // WebSocket routes (QA streaming)
                .route("ws-qa", r -> r
                        .path("/api/ws/qa")
                        .filters(f -> f
                                .stripPrefix(1))
                        .uri(knowledgeMentorUrl))

                // Health check routes (no auth required)
                .route("health-auth", r -> r
                        .path("/api/auth/health")
                        .uri(authServiceUrl))

                .route("health-km", r -> r
                        .path("/api/actuator/health")
                        .uri(knowledgeMentorUrl))
                .build();
    }
}
