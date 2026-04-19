package com.codereview.auth.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.think.platform.shared.common.result.Result;
import com.think.platform.shared.common.result.ResultCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JWT Access Denied Handler - Auth Service
 *
 * 处理授权异常（403 禁止访问）
 *
 * @author Auth Service Team
 */
@Slf4j
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        log.error("Access denied: {}", accessDeniedException.getMessage());

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        Result<Void> result = Result.forbidden("权限不足：" + accessDeniedException.getMessage());
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
