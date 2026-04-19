package com.codereview.ai.domain.agent.shared.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Agent 配置属性
 *
 * @author AI Code Mentor Team
 */
@Data
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    /**
     * 是否启用 Agent 功能
     */
    private boolean enabled = true;

    /**
     * 并行执行开关
     */
    private boolean parallelExecution = true;

    /**
     * 超时时间 (毫秒)
     */
    private long timeout = 60000;

    /**
     * 最大重试次数
     */
    private int maxRetries = 2;

    /**
     * 缓存执行结果
     */
    private boolean cacheResults = true;

    /**
     * 缓存过期时间 (秒)
     */
    private long cacheTtl = 3600;

    /**
     * 启用的 Agent 列表
     */
    private EnabledAgents enabledAgents = new EnabledAgents();

    @Data
    public static class EnabledAgents {
        private boolean codeStandardsInspector = true;
        private boolean architectureGuardian = true;
        private boolean securityAuditor = true;
        private boolean performanceOptimizer = true;
        private boolean teachingMentor = true;
        private boolean skillAssessor = true;
        private boolean exerciseCoach = true;
        private boolean learningPathPlanner = true;
    }
}
