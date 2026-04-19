package com.codereview.ai.domain.agent.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Agent 基类
 * 提供通用的 Agent 实现，简化具体 Agent 的开发
 *
 * @author AI Code Mentor Team
 */
@Slf4j
public abstract class BaseAgent {

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * 获取 Agent 类型 (由子类实现)
     */
    public abstract String getAgentType();

    /**
     * 获取 Agent 名称 (由子类实现)
     */
    public abstract String getName();

    /**
     * 获取 Agent 描述 (由子类实现)
     */
    public abstract String getDescription();

    /**
     * 获取 Agent 优先级 (数字越小优先级越高)
     */
    public abstract int getPriority();

    /**
     * 检查 Agent 是否可用
     */
    public boolean isAvailable() {
        return true;
    }

    /**
     * 构建系统提示词 (由子类实现)
     */
    protected abstract String buildSystemPrompt();

    /**
     * 根据上下文构建系统提示词 (可被子类重写)
     */
    protected String buildSystemPromptForContext(AgentExecutionContext context) {
        return buildSystemPrompt();
    }

    /**
     * 构建用户提示词 (由子类实现)
     */
    protected abstract String buildUserPrompt(AgentExecutionContext context);

    /**
     * 解析 AI 响应 (由子类实现)
     */
    protected abstract AgentExecutionResult parseResponse(String response, AgentExecutionContext context);

    /**
     * 执行 Agent 逻辑
     */
    public AgentExecutionResult execute(AgentExecutionContext context) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Executing agent: {} ({})", getName(), getAgentType());

            // 构建提示词
            String systemPrompt = buildSystemPromptForContext(context);
            String userPrompt = buildUserPrompt(context);

            log.debug("System prompt: {}", systemPrompt);
            log.debug("User prompt length: {}", userPrompt.length());

            // 调用 AI 服务
            String aiResponse = context.getAiService().chat(systemPrompt, userPrompt);

            // 解析响应
            AgentExecutionResult result = parseResponse(aiResponse, context);
            result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            result.setSuccess(true);

            log.info("Agent {} completed in {}ms", getName(), result.getExecutionTimeMs());
            return result;

        } catch (Exception e) {
            log.error("Agent {} execution failed", getName(), e);

            AgentExecutionResult result = AgentExecutionResult.builder()
                    .agentType(getAgentType())
                    .agentName(getName())
                    .success(false)
                    .error(e.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();

            return result;
        }
    }

    /**
     * 从响应中提取 JSON
     * 处理可能被截断的响应，尝试修复不完整的 JSON
     */
    protected String extractJson(String response) {
        // 尝试直接解析
        if (response.trim().startsWith("{")) {
            return extractAndFixJson(response);
        }

        // 尝试从代码块中提取
        int codeBlockStart = response.indexOf("```json");
        if (codeBlockStart >= 0) {
            int jsonStart = response.indexOf("{", codeBlockStart);
            int codeBlockEnd = response.indexOf("```", jsonStart);
            if (jsonStart >= 0 && codeBlockEnd > jsonStart) {
                String jsonContent = response.substring(jsonStart, codeBlockEnd);
                return extractAndFixJson(jsonContent);
            }
        }

        int codeBlockStart2 = response.indexOf("```");
        if (codeBlockStart2 >= 0) {
            int jsonStart = response.indexOf("{", codeBlockStart2);
            int codeBlockEnd = response.indexOf("```", jsonStart);
            if (jsonStart >= 0 && codeBlockEnd > jsonStart) {
                String jsonContent = response.substring(jsonStart, codeBlockEnd);
                return extractAndFixJson(jsonContent);
            }
        }

        return null;
    }

    /**
     * 提取并尝试修复 JSON
     * 如果 JSON 被截断，尝试找到最后一个完整的对象并关闭它
     */
    private String extractAndFixJson(String jsonContent) {
        // 查找最后一个完整的 JSON 对象
        int end = jsonContent.lastIndexOf("}");
        if (end > 0) {
            String extracted = jsonContent.substring(0, end + 1);

            // 尝试平衡 JSON 结构
            String balanced = balanceJson(extracted);
            if (balanced != null) {
                return balanced;
            }

            return extracted;
        }
        return jsonContent;
    }

    /**
     * 尝试平衡 JSON 结构
     * 如果 JSON 未正确关闭，添加必要的闭合括号
     */
    private String balanceJson(String json) {
        int openBraces = 0;
        int openBrackets = 0;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{' && !isInString(json, i)) openBraces++;
            else if (c == '}' && !isInString(json, i)) openBraces--;
            else if (c == '[' && !isInString(json, i)) openBrackets++;
            else if (c == ']' && !isInString(json, i)) openBrackets--;
        }

        StringBuilder balanced = new StringBuilder(json);

        // 关闭未闭合的数组
        for (int i = 0; i < openBrackets; i++) {
            balanced.append("]");
        }

        // 关闭未闭合的对象
        for (int i = 0; i < openBraces; i++) {
            balanced.append("}");
        }

        String result = balanced.toString();
        // 简单验证结果是否看起来像有效的 JSON
        if (result.startsWith("{") && result.endsWith("}")) {
            return result;
        }

        return null;
    }

    /**
     * 检查字符是否在字符串内
     */
    private boolean isInString(String json, int pos) {
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < pos; i++) {
            char c = json.charAt(i);
            if (c == '\\' && !escaped) {
                escaped = true;
            } else if (c == '"' && !escaped) {
                inString = !inString;
            } else {
                escaped = false;
            }
        }

        return inString;
    }
}
