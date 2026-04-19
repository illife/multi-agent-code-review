package com.think.platform.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * CORS Configuration for API Gateway
 * Allows cross-origin requests from frontend
 *
 * @author Gateway Team
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Allow credentials (cookies, authorization headers)
        corsConfig.setAllowCredentials(true);

        // Allow frontend origins (development and production)
        corsConfig.addAllowedOriginPattern("http://localhost:5173");
        corsConfig.addAllowedOriginPattern("http://localhost:5174");
        corsConfig.addAllowedOriginPattern("http://localhost:5175");
        corsConfig.addAllowedOriginPattern("http://localhost:5176");
        corsConfig.addAllowedOriginPattern("http://127.0.0.1:5173");
        corsConfig.addAllowedOriginPattern("http://127.0.0.1:5174");
        corsConfig.addAllowedOriginPattern("http://127.0.0.1:5175");
        corsConfig.addAllowedOriginPattern("http://127.0.0.1:5176");

        // Allow common headers
        corsConfig.addAllowedHeader("*");

        // Allow common HTTP methods
        corsConfig.addAllowedMethod("*");

        // Expose headers that frontend needs to read
        corsConfig.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials",
            "X-Total-Count",
            "X-Page-Count"
        ));

        // Max age for preflight requests (1 hour)
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
