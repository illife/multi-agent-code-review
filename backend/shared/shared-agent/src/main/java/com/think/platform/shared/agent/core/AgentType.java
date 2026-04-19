package com.think.platform.shared.agent.core;

/**
 * Agent 类型枚举
 * 定义系统中所有支持的 Agent 类型
 *
 * @author AI Code Mentor Team
 */
public enum AgentType {

    // ===== 代码审查类 Agents =====

    /**
     * 代码规范检查员
     * 检查代码风格、命名规范、代码格式等
     */
    CODE_STANDARDS_INSPECTOR("CodeStandardsInspector", "代码规范检查员", 10),

    /**
     * 架构守护者
     * 检查代码架构设计、模块依赖、设计模式使用等
     */
    ARCHITECTURE_GUARDIAN("ArchitectureGuardian", "架构守护者", 20),

    /**
     * 安全审计员
     * 检查安全漏洞、SQL注入、XSS、敏感信息泄露等
     */
    SECURITY_AUDITOR("SecurityAuditor", "安全审计员", 30),

    /**
     * 性能优化师
     * 分析代码性能瓶颈、资源使用情况
     */
    PERFORMANCE_OPTIMIZER("PerformanceOptimizer", "性能优化师", 40),

    // ===== 教学类 Agents =====

    /**
     * 教学导师
     * 根据用户水平提供个性化教学指导
     */
    TEACHING_MENTOR("TeachingMentor", "教学导师", 100),

    /**
     * 技能评估师
     * 评估用户的编程技能水平
     */
    SKILL_ASSESSOR("SkillAssessor", "技能评估师", 101),

    /**
     * 练习教练
     * 生成和评估编程练习
     */
    EXERCISE_COACH("ExerciseCoach", "练习教练", 102),

    /**
     * 学习路径规划师
     * 为用户定制学习路径
     */
    LEARNING_PATH_PLANNER("LearningPathPlanner", "学习路径规划师", 103),

    // ===== 文档类 Agents =====

    /**
     * 文档生成器
     * 自动生成代码文档
     */
    DOCUMENTATION_GENERATOR("DocumentationGenerator", "文档生成器", 200),

    /**
     * 代码解释器
     * 解释代码的功能和逻辑
     */
    CODE_EXPLAINER("CodeExplainer", "代码解释器", 201),

    // ===== 分析类 Agents =====

    /**
     * 依赖分析员
     * 分析项目依赖关系
     */
    DEPENDENCY_ANALYZER("DependencyAnalyzer", "依赖分析员", 300),

    /**
     * 复杂度分析员
     * 分析代码复杂度
     */
    COMPLEXITY_ANALYZER("ComplexityAnalyzer", "复杂度分析员", 301),

    // ===== 编排类 Agents =====

    /**
     * Agent 编排器
     * 负责协调多个 Agent 的执行
     */
    ORCHESTRATOR("Orchestrator", "Agent编排器", 900);

    private final String code;
    private final String name;
    private final int priority;

    AgentType(String code, String name, int priority) {
        this.code = code;
        this.name = name;
        this.priority = priority;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * 根据 code 获取 AgentType
     */
    public static AgentType fromCode(String code) {
        for (AgentType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown agent type code: " + code);
    }

    /**
     * 检查是否为代码审查类 Agent
     */
    public boolean isInspectorAgent() {
        return priority >= 10 && priority < 100;
    }

    /**
     * 检查是否为教学类 Agent
     */
    public boolean isMentorAgent() {
        return priority >= 100 && priority < 200;
    }
}
