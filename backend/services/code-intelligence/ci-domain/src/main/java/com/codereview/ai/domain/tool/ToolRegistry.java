package com.codereview.ai.domain.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Tool Registry
 *
 * 管理所有已注册的工具，提供工具查询和格式转换功能
 *
 * @author Code Review AI Team
 */
@Slf4j
@Component
public class ToolRegistry {

    private final Map<String, Tool> tools = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 注册工具
     */
    public void register(Tool tool) {
        tools.put(tool.getName(), tool);
        log.info("Registered tool: {} (category: {}, priority: {}, free: {})",
                tool.getName(), tool.getCategory(), tool.getPriority(), tool.isFree());
    }

    /**
     * 批量注册工具
     */
    public void registerAll(List<Tool> toolList) {
        toolList.forEach(this::register);
        log.info("Batch registration complete: {} tools registered", toolList.size());
    }

    /**
     * 获取工具
     */
    public Tool getTool(String name) {
        return tools.get(name);
    }

    /**
     * 获取支持指定语言的所有工具（按优先级排序）
     */
    public List<Tool> getToolsForLanguage(String language) {
        return tools.values().stream()
                .filter(tool -> tool.supportsLanguage(language))
                .sorted(Comparator.comparingInt(Tool::getPriority))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有工具
     */
    public List<Tool> getAllTools() {
        return new ArrayList<>(tools.values());
    }

    /**
     * 获取工具数量
     */
    public int getToolCount() {
        return tools.size();
    }

    /**
     * 获取Qwen API格式的工具定义
     * 用于Function Calling API调用
     */
    public List<Map<String, Object>> getToolDefinitionsForLanguage(String language) {
        return tools.values().stream()
                .filter(tool -> tool.supportsLanguage(language))
                .sorted(Comparator.comparingInt(Tool::getPriority))
                .map(this::toQwenToolFormat)
                .collect(Collectors.toList());
    }

    /**
     * 将工具转换为Qwen Function Calling格式
     */
    private Map<String, Object> toQwenToolFormat(Tool tool) {
        Map<String, Object> functionDef = new HashMap<>();
        functionDef.put("name", tool.getName());
        functionDef.put("description", tool.getDescription());

        // 解析JSON Schema
        try {
            JsonNode schemaNode = objectMapper.readTree(tool.getParameterSchema());
            functionDef.put("parameters", schemaNode);
        } catch (Exception e) {
            log.warn("Failed to parse parameter schema for tool: {}, using empty schema", tool.getName(), e);
            functionDef.put("parameters", Map.of(
                    "type", "object",
                    "properties", new HashMap<>()
            ));
        }

        return Map.of(
                "type", "function",
                "function", functionDef
        );
    }

    /**
     * 获取工具描述列表（用于Prompt）
     */
    public String getToolDescriptions(String language) {
        List<Tool> tools = getToolsForLanguage(language);
        StringBuilder sb = new StringBuilder();

        for (Tool tool : tools) {
            sb.append(String.format("- %s: %s%s\n",
                    tool.getName(),
                    tool.getDescription(),
                    tool.isFree() ? " (免费)" : ""));
        }

        return sb.toString();
    }

    /**
     * 检查工具是否存在
     */
    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }

    /**
     * 获取所有工具名称
     */
    public Set<String> getToolNames() {
        return tools.keySet();
    }
}
