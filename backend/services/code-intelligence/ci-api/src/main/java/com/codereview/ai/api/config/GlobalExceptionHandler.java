package com.codereview.ai.api.config;

import com.think.platform.shared.common.result.Result;
import com.think.platform.shared.infra.ai.AiUsageLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AiUsageLimitExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Result<?> handleAiUsageLimitExceededException(AiUsageLimitExceededException e) {
        log.warn("AI usage blocked: {}", e.getMessage());
        return Result.failed(429, e.getMessage());
    }
}
