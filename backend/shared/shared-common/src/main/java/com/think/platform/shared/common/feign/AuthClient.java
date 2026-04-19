package com.think.platform.shared.common.feign;

import com.think.platform.shared.common.dto.UserDTO;
import com.think.platform.shared.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Auth Service Feign Client
 *
 * Provides communication with the Auth service for user management.
 * All services should use this client instead of maintaining their own User entities.
 *
 * @author Platform Team
 */
@FeignClient(name = "auth-service", url = "${auth.service.url:http://localhost:8083}")
public interface AuthClient {

    /**
     * Get user by ID
     *
     * @param userId User ID
     * @return Result containing UserDTO
     */
    @GetMapping("/api/users/{userId}")
    Result<UserDTO> getUserById(@PathVariable("userId") Long userId);

    /**
     * Get current user from JWT token
     *
     * @param token Authorization token (Bearer token)
     * @return Result containing UserDTO
     */
    @GetMapping("/api/users/me")
    Result<UserDTO> getCurrentUser(@RequestHeader("Authorization") String token);
}
