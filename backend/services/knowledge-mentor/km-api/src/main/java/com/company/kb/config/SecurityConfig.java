package com.company.kb.config;

import com.think.platform.shared.security.GatewayAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;

/**
 * Security Configuration for Knowledge Mentor Service
 *
 * 信任来自 API Gateway 的请求，直接从请求头读取用户信息
 * Gateway 已经验证过 JWT，不需要重复验证
 *
 * @author Platform Team
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.auth.bypass-patterns:}")
    private String[] authBypassPatterns;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            GatewayAuthenticationFilter gatewayAuthenticationFilter
    ) throws Exception {
        http
                // Disable CSRF for stateless APIs
                .csrf(AbstractHttpConfigurer::disable)

                // Disable CORS - handled by API Gateway
                .cors(AbstractHttpConfigurer::disable)

                // Disable HTTP Basic
                .httpBasic(AbstractHttpConfigurer::disable)

                // Disable form login
                .formLogin(AbstractHttpConfigurer::disable)

                // Configure session management - stateless
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Add Gateway authentication filter before UsernamePasswordAuthenticationFilter
                // Trust Gateway-validated requests, read user info from headers
                .addFilterBefore(gatewayAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // Configure authorization rules
                .authorizeHttpRequests(auth -> {
                    String[] configuredBypassPatterns = Arrays.stream(authBypassPatterns)
                            .map(String::trim)
                            .filter(pattern -> !pattern.isEmpty())
                            .toArray(String[]::new);

                    if (configuredBypassPatterns.length > 0) {
                        auth.requestMatchers(configuredBypassPatterns).permitAll();
                    }

                    auth
                            // Public endpoints
                            .requestMatchers("/actuator/**").permitAll()
                            .requestMatchers("/error").permitAll()
                            .requestMatchers("/ws/**").permitAll()
                            // Always allow CORS OPTIONS preflight requests
                            .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

                            // All other requests require authentication (from Gateway)
                            .anyRequest().authenticated();
                });

        return http.build();
    }

    @Bean
    public GatewayAuthenticationFilter gatewayAuthenticationFilter() {
        return new GatewayAuthenticationFilter();
    }
}
