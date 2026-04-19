package com.codereview.ai.domain.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tool Auto Configuration
 *
 * 应用启动时自动注册所有Tool实现类
 *
 * @author Code Review AI Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolAutoConfiguration {

    private final ToolRegistry toolRegistry;
    private final ApplicationContext applicationContext;

    /**
     * 应用启动完成后自动注册所有工具
     */
    @EventListener(ApplicationReadyEvent.class)
    public void registerTools() {
        Map<String, Tool> tools = applicationContext.getBeansOfType(Tool.class);

        if (tools.isEmpty()) {
            log.warn("No tools found to register");
            return;
        }

        // 按优先级排序后注册
        List<Tool> sortedTools = tools.values().stream()
                .sorted(Comparator.comparingInt(Tool::getPriority))
                .collect(Collectors.toList());

        toolRegistry.registerAll(sortedTools);

        log.info("===================================================");
        log.info("Tool Registration Complete");
        log.info("Total Tools Registered: {}", sortedTools.size());
        log.info("===================================================");

        // 打印工具列表
        for (Tool tool : sortedTools) {
            log.info("  - {} [{}] - Priority: {}, Free: {}",
                    tool.getName(),
                    tool.getCategory(),
                    tool.getPriority(),
                    tool.isFree());
        }
    }
}
