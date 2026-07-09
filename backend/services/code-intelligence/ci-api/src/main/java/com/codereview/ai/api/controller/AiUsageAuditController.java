package com.codereview.ai.api.controller;

import com.think.platform.shared.common.result.Result;
import com.think.platform.shared.infra.ai.AiUsageAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/agent/ai-usage/audit")
@RequiredArgsConstructor
@Tag(name = "AI 用量审计", description = "管理员查看站内 AI 调用模型与 token 消耗")
public class AiUsageAuditController {

    private final AiUsageAuditService aiUsageAuditService;

    @GetMapping("/today")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "查看今日站内 AI 用量")
    public Result<Map<String, Object>> today() {
        return Result.success(aiUsageAuditService.todaySummary());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "按日期查看站内 AI 用量")
    public Result<Map<String, Object>> byDate(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        String targetDate = date != null ? date.toString() : null;
        return Result.success(aiUsageAuditService.summaryForDate(targetDate));
    }
}
