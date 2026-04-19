package com.codereview.ai.config;

import com.think.platform.shared.security.GatewayAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security Configuration
 *
 * Configures Spring Security for JWT authentication
 *
 * @author Code Review AI Team
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

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
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // WebSocket endpoint
                        .requestMatchers("/api/ws/**").permitAll()

                        // Test endpoint
                        .requestMatchers("/api/test/**").permitAll()

                        // Code review endpoints - allow for development
                        .requestMatchers("/api/review/submit").permitAll()
                        .requestMatchers("/api/review/submit-async").permitAll()

                        // Teaching endpoints - require authentication
                        .requestMatchers("/api/teaching/**").authenticated()

                        // Project endpoints - require authentication
                        .requestMatchers("/api/project/**").authenticated()

                        // All other requests require authentication (documents, review, etc.)
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public GatewayAuthenticationFilter gatewayAuthenticationFilter() {
        return new GatewayAuthenticationFilter();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
