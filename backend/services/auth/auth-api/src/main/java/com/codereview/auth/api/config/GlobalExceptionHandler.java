package com.codereview.auth.api.config;

import com.think.platform.shared.common.exception.BaseException;
import com.think.platform.shared.common.result.Result;
import com.think.platform.shared.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Auth service API exception handling.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleBaseException(BaseException e) {
        log.warn("Auth business exception: {}", e.getMessage());
        return Result.failed(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.failed(ResultCode.VALID_ERROR, errorMsg);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        log.error("Auth system exception", e);
        return Result.failed(ResultCode.INTERNAL_ERROR, "系统内部错误");
    }
}
