# 多智能体系统使用指南

## 概述

本项目实现了基于多智能体架构的代码审查与教学系统。系统包含以下核心组件：

### 已实现的 Agent

#### 代码审查类 Agent

| Agent | 说明 | 优先级 |
|-------|------|--------|
| **CodeStandardsInspector** | 代码规范检查员 - 检查命名、格式、注释等 | 10 |
| **ArchitectureGuardian** | 架构守护者 - 检查设计模式、SOLID原则等 | 20 |
| **SecurityAuditor** | 安全审计员 - 检查SQL注入、XSS等安全问题 | 30 |
| **PerformanceOptimizer** | 性能优化师 - 分析性能瓶颈和优化机会 | 40 |

#### 教学类 Agent

| Agent | 说明 | 优先级 |
|-------|------|--------|
| **TeachingMentor** | 教学导师 - 提供个性化教学内容 | 100 |
| **SkillAssessor** | 技能评估师 - 评估用户编程水平 | 101 |
| **ExerciseCoach** | 练习教练 - 生成和评估练习题 | 102 |
| **LearningPathPlanner** | 学习路径规划师 - 定制学习路径 | 103 |

## 配置

### 1. 添加配置到 application.yml

```yaml
agent:
  enabled: true
  parallel-execution: true

qwen:
  api-key: ${QWEN_API_KEY}
  chat-model: qwen-turbo
```

### 2. 确保依赖已添加

```xml
<!-- shared-agent -->
<dependency>
    <groupId>com.think.platform</groupId>
    <artifactId>shared-agent</artifactId>
</dependency>

<!-- shared-ai -->
<dependency>
    <groupId>com.think.platform</groupId>
    <artifactId>shared-ai</artifactId>
</dependency>
```

## 使用示例

### 1. 代码审查

```java
@Autowired
private AgentService agentService;

// 执行代码审查
CodeReview review = agentService.executeCodeReview(
    userId,      // 用户ID
    projectId,   // 项目ID
    code,        // 代码内容
    "java",      // 编程语言
    filePath     // 文件路径
);

// 获取发现的问题
List<CodeIssue> issues = codeIssueRepository.findByReviewId(review.getId());
```

### 2. 技能评估

```java
// 评估用户技能
SkillAssessmentResult result = agentService.assessSkills(
    userId,      // 用户ID
    code,        // 代码内容
    "python"     // 编程语言
);

// 获取评估结果
int score = result.overallScore();
String level = result.overallLevel();
```

### 3. 生成教学内容

```java
// 生成教学内容
TeachingContent content = agentService.generateTeachingContent(
    userId,          // 用户ID
    "异常处理",      // 主题
    "INTERMEDIATE",  // 用户水平
    "java"           // 编程语言
);

// 获取教学内容
String formattedContent = content.content();
```

### 4. 生成练习题

```java
// 生成练习题
Exercise exercise = agentService.generateExercise(
    userId,         // 用户ID
    "java",         // 编程语言
    "MEDIUM",       // 难度
    "algorithm",    // 类型
    "排序算法"      // 主题
);

// 获取练习内容
String starterCode = exercise.getStarterCode();
List<String> hints = exercise.getHints();
```

### 5. 生成学习路径

```java
// 生成学习路径
LearningPath path = agentService.generateLearningPath(
    userId,              // 用户ID
    "全栈开发",          // 目标技能
    "BEGINNER",          // 当前水平
    "希望转行做开发",    // 描述
    "java"               // 编程语言
);

// 获取学习路径
int estimatedDuration = path.estimatedDuration();
List<ModuleSummary> modules = path.modules();
```

## API 端点

### Agent 任务管理

| 端点 | 方法 | 描述 |
|------|------|------|
| `/api/agent/tasks/code-review/execute` | POST | 创建并执行代码审查任务 |
| `/api/agent/tasks/learning-path/execute` | POST | 创建并执行学习路径任务 |
| `/api/agent/tasks/exercise/execute` | POST | 创建并执行练习题生成任务 |
| `/api/agent/tasks/{taskId}` | GET | 获取任务状态 |
| `/api/agent/tasks/{taskId}/executions` | GET | 获取任务执行详情 |

### 教学相关

| 端点 | 方法 | 描述 |
|------|------|------|
| `/api/teaching/explain` | POST | 获取代码解释 |
| `/api/teaching/skills` | GET | 获取用户技能水平 |
| `/api/teaching/exercises/recommended` | GET | 获取推荐练习 |

## Agent 执行流程

### 代码审查流程

```
用户上传代码
    ↓
创建 CodeReview 记录
    ↓
并行执行三个检查员 Agent:
    - CodeStandardsInspector
    - ArchitectureGuardian
    - SecurityAuditor
    ↓
聚合结果，保存 CodeIssue
    ↓
返回审查报告
```

### 教学流程

```
用户请求教学内容
    ↓
SkillAssessor 评估用户水平
    ↓
TeachingMentor 根据水平生成内容
    ↓
返回个性化教学内容
```

## 扩展指南

### 添加新的 Agent

1. 创建 Agent 类继承 `BaseAgent`

```java
@Component
public class MyCustomAgent extends BaseAgent {

    @Override
    public String getAgentType() {
        return "MY_CUSTOM_AGENT";
    }

    @Override
    protected String buildSystemPrompt() {
        return "你的系统提示词";
    }

    @Override
    protected String buildUserPrompt(AgentExecutionContext context) {
        return "你的用户提示词";
    }

    @Override
    protected AgentExecutionResult parseResponse(String response, AgentExecutionContext context) {
        // 解析 AI 响应
        return result;
    }
}
```

2. 在 `AgentAutoConfiguration` 中注册

```java
@Autowired
private MyCustomAgent myCustomAgent;

@Bean
public AgentRegistry agentRegistry() {
    AgentRegistry registry = new AgentRegistry();
    registry.register("MY_CUSTOM_AGENT", myCustomAgent);
    return registry;
}
```

3. 在 `AgentService` 中添加调用方法

```java
public MyResult executeMyAgent(Long userId, ...) {
    AgentExecutionContext context = ...;
    AgentExecutionResult result = agentService.executeAgent("MY_CUSTOM_AGENT", context);
    return convertToMyResult(result);
}
```

## 故障排查

### Agent 没有被注册

检查 `@Component` 注解和 `AgentAutoConfiguration` 中的注册代码。

### AI 调用失败

检查 `qwen.api-key` 配置是否正确，以及网络连接是否正常。

### 执行超时

增加 `agent.timeout` 配置值，或检查 AI 服务响应速度。
