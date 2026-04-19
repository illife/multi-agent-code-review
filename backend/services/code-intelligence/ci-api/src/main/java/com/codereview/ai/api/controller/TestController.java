package com.codereview.ai.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Test Controller for debugging authentication
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    @GetMapping("/auth-info")
    public Map<String, Object> getAuthInfo(Authentication authentication) {
        Map<String, Object> info = new HashMap<>();

        if (authentication != null) {
            info.put("authenticated", true);
            info.put("principal", authentication.getPrincipal());
            info.put("authorities", authentication.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList());
            info.put("details", authentication.getDetails());
        } else {
            info.put("authenticated", false);
            info.put("message", "No authentication found");
        }

        log.info("Auth info requested: {}", info);
        return info;
    }
}
