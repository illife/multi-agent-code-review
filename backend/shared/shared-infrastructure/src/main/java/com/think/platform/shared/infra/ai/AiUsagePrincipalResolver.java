package com.think.platform.shared.infra.ai;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Resolves the current caller for AI quota accounting.
 */
public final class AiUsagePrincipalResolver {

    private AiUsagePrincipalResolver() {
    }

    public static String currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = extractUserId(authentication);
        if (userId != null) {
            return "user:" + userId;
        }

        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .filter(username -> !username.isBlank())
                .map(username -> "username:" + username)
                .orElse("anonymous");
    }

    public static String principalForUsername(String username) {
        if (username == null || username.isBlank()) {
            return "anonymous";
        }
        return "username:" + username;
    }

    private static Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        try {
            Method getIdMethod = principal.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(principal);
            if (id instanceof Long longId) {
                return longId;
            }
            if (id instanceof Integer intId) {
                return intId.longValue();
            }
            if (id instanceof String stringId && !stringId.isBlank()) {
                return Long.parseLong(stringId);
            }
        } catch (Exception ignored) {
            return null;
        }

        return null;
    }
}
