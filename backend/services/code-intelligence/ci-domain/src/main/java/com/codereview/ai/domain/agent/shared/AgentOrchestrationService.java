package com.codereview.ai.domain.agent.shared;

import com.codereview.ai.domain.model.TeachingReport;

/**
 * Agent 编排服务接口
 * 定义 Agent 执行和编排的核心操作
 *
 * @author AI Code Mentor Team
 */
public interface AgentOrchestrationService {

    /**
     * 执行单个 Agent
     *
     * @param agentType Agent 类型
     * @param context   执行上下文
     * @return 执行结果
     */
    AgentExecutionResult executeAgent(String agentType, AgentExecutionContext context);

    /**
     * 执行代码审查（并行执行多个检查员）
     *
     * @param context 执行上下文
     * @return 所有 Agent 的执行结果列表
     */
    java.util.List<AgentExecutionResult> executeCodeReview(AgentExecutionContext context);

    /**
     * 生成代码审查教学报告
     *
     * @param reviewId      代码审查ID
     * @param codeContent   代码内容
     * @param language      编程语言
     * @param fileName      文件名
     * @param totalIssues   总问题数
     * @param criticalIssues 严重问题数
     * @param highIssues    高危问题数
     * @param mediumIssues  中危问题数
     * @param lowIssues     低危问题数
     * @return 教学报告
     */
    TeachingReport generateTeachingReport(
            Long reviewId,
            String codeContent,
            String language,
            String fileName,
            Long userId,
            int totalIssues,
            int criticalIssues,
            int highIssues,
            int mediumIssues,
            int lowIssues
    );

    /**
     * 获取所有已注册的 Agent
     *
     * @return Agent 列表
     */
    java.util.List<BaseAgent> getAllAgents();

    /**
     * 检查 Agent 是否已注册
     *
     * @param agentType Agent 类型
     * @return 是否已注册
     */
    boolean hasAgent(String agentType);
}
