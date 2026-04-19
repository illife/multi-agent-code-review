package com.think.platform.gateway.config;

import com.think.platform.gateway.filter.AdminRoleFilter;
import com.think.platform.gateway.filter.AuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final AuthenticationFilter authenticationFilter;
    private final AdminRoleFilter adminRoleFilter;

    public static final int AUTH_SERVICE_PORT = 8083;
    public static final int CODE_INTELLIGENCE_PORT = 8088;
    public static final int KNOWLEDGE_MENTOR_PORT = 8080;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth Service Routes (public endpoints)
                .route("auth-public", r -> r
                        .path("/api/auth/login", "/api/auth/register", "/api/auth/refresh")
                        .filters(f -> f
                                .stripPrefix(0)
                                .removeRequestHeader("Cookie"))
                        .uri("http://localhost:" + AUTH_SERVICE_PORT))

                // Auth Service Routes (protected)
                .route("auth-api", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(authenticationFilter))
                        .uri("http://localhost:" + AUTH_SERVICE_PORT))

                // User API (protected + admin)
                .route("user-api", r -> r
                        .path("/api/users/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(authenticationFilter)
                                .filter(adminRoleFilter))
                        .uri("http://localhost:" + AUTH_SERVICE_PORT))

                // Code Intelligence Service Routes (when service starts)
                .route("agent-api", r -> r
                        .path("/api/agent/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(authenticationFilter))
                        .uri("http://localhost:" + CODE_INTELLIGENCE_PORT))

                .route("learning-api", r -> r
                        .path("/api/learning/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(authenticationFilter))
                        .uri("http://localhost:" + CODE_INTELLIGENCE_PORT))

                .route("project-api", r -> r
                        .path("/api/project/**", "/api/projects/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(authenticationFilter))
                        .uri("http://localhost:" + CODE_INTELLIGENCE_PORT))

                .route("review-api", r -> r
                        .path("/api/review/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(authenticationFilter))
                        .uri("http://localhost:" + CODE_INTELLIGENCE_PORT))

                .route("analysis-api", r -> r
                        .path("/api/analysis/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(authenticationFilter))
                        .uri("http://localhost:" + CODE_INTELLIGENCE_PORT))

                .route("teaching-api", r -> r
                        .path("/api/teaching/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(authenticationFilter))
                        .uri("http://localhost:" + CODE_INTELLIGENCE_PORT))

                .route("architecture-api", r -> r
                        .path("/api/architecture/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(authenticationFilter))
                        .uri("http://localhost:" + CODE_INTELLIGENCE_PORT))

                // Knowledge Mentor Service Routes
                // 使用 stripPrefix(1) 去掉 /api 前缀，后端控制器不需要 /api 前缀
                .route("document-api", r -> r
                        .path("/api/documents/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(authenticationFilter))
                        .uri("http://localhost:" + KNOWLEDGE_MENTOR_PORT))

                .route("qa-api", r -> r
                        .path("/api/qa/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(authenticationFilter))
                        .uri("http://localhost:" + KNOWLEDGE_MENTOR_PORT))

                .route("search-api", r -> r
                        .path("/api/search/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(authenticationFilter))
                        .uri("http://localhost:" + KNOWLEDGE_MENTOR_PORT))

                .route("lessons-api", r -> r
                        .path("/api/lessons/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(authenticationFilter))
                        .uri("http://localhost:" + KNOWLEDGE_MENTOR_PORT))

                .route("exercises-api", r -> r
                        .path("/api/exercises/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(authenticationFilter))
                        .uri("http://localhost:" + KNOWLEDGE_MENTOR_PORT))

                // Health check routes (no auth required)
                .route("health-auth", r -> r
                        .path("/api/auth/health")
                        .uri("http://localhost:" + AUTH_SERVICE_PORT))

                .route("health-km", r -> r
                        .path("/api/actuator/health")
                        .uri("http://localhost:" + KNOWLEDGE_MENTOR_PORT))
                .build();
    }
}
