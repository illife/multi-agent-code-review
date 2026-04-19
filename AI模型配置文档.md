# AI模型配置技术文档

## 项目信息

| 项目名称 | 基于多智能体协作的智能代码审查与教学系统 |
|---------|----------------------------------------|
| 文档版本 | v1.0 |
| 更新日期 | 2025-01-12 |
| 云服务商 | 阿里云百炼 |
| 总免费额度 | 约1750万 tokens |

---

## 一、模型清单

### 1.1 主力模型（核心使用）

| 模型名称 | Bean名称 | 免费额度 | 状态 | 用途 |
|---------|---------|----------|------|------|
| **qwen-turbo-1101** | primaryModel | 10,000,000 | ✅ 已启用 | 开发测试主力 |
| **qwen-turbo-2025-02-11** | turboBackup | 1,000,000 | ⏸️ 备用 | Turbo备用 |
| **qwen-plus-latest** | plusModel | 1,000,000 | ⏸️ 备用 | 质量提升 |

### 1.2 代码专用模型

| 模型名称 | Bean名称 | 免费额度 | 状态 | 用途 |
|---------|---------|----------|------|------|
| **qwen-coder-plus** | coderModel | 1,000,000 | ⏸️ 按需 | 代码审查/生成 |
| **qwen-coder-plus-1106** | coderModel1106 | 1,000,000 | ⏸️ 备用 | 代码备用 |

### 1.3 高级模型（关键任务）

| 模型名称 | Bean名称 | 免费额度 | 状态 | 用途 |
|---------|---------|----------|------|------|
| **qwen-max** | maxModel | 1,000,000 | ⏸️ 按需 | 复杂推理 |
| **qwen3-max-2026-01-23** | qwen3Max | 1,000,000 | ⏸️ 按需 | 旗舰模型 |
| **qwen3.5-122b-a10b** | qwen35_122b | 1,000,000 | ⏸️ 按需 | 超大参数 |
| **qwen3.5-plus-2026-02-15** | qwen35Plus | 1,000,000 | ⏸️ 按需 | 高级Plus |

### 1.4 长上下文模型

| 模型名称 | Bean名称 | 免费额度 | 状态 | 用途 |
|---------|---------|----------|------|------|
| **qwen-long** | longModel | 2,000,000 | ⏸️ 按需 | 长文档分析 |

### 1.5 视觉模型

| 模型名称 | Bean名称 | 免费额度 | 状态 | 用途 |
|---------|---------|----------|------|------|
| **qwen-vl-ocr-2025-11-20** | ocrModel | 1,000,000 | ⏸️ 按需 | OCR识别 |
| **qwen-vl-max-2025-04-02** | vlMax0402 | 1,000,000 | ⏸️ 按需 | 视觉理解 |
| **qwen-vl-max-1119** | vlMax1119 | 1,000,000 | ⏸️ 按需 | 视觉理解 |
| **qwen-vl-max-2025-04-08** | vlMax0408 | 1,000,000 | ⏸️ 按需 | 视觉理解 |
| **qwen-vl-max-1230** | vlMax1230 | 1,000,000 | ⏸️ 按需 | 视觉理解 |

---

## 二、使用策略

### 2.1 模型选择决策树

```
开始
 │
 ├─ 简单开发任务（API调试、功能验证）
 │   └─> qwen-turbo-1101 (主力)
 │
 ├─ 代码相关任务
 │   ├─ 代码审查/生成
 │   │   └─> qwen-coder-plus
 │   └─ Bug修复
 │       └─> qwen-coder-plus
 │
 ├─ 重要测试任务
 │   └─> qwen-plus-latest
 │
 ├─ 复杂推理任务
 │   └─> qwen-max / qwen3-max
 │
 ├─ 超大文件/全项目分析
 │   └─> qwen-long
 │
 └─ 图片/截图相关
     └─> qwen-vl系列
```

### 2.2 环境配置策略

| 环境 | 主要模型 | 备用模型 | 目标 |
|------|----------|----------|------|
| **dev** | qwen-turbo-1101 | qwen-turbo-2025-02-11 | 快速开发 |
| **test** | qwen-turbo-1101 | qwen-coder-plus | 功能测试 |
| **prod** | 智能路由 | 全模型池 | 生产使用 |

### 2.3 额度使用计划

```
开发期 (第1-4周)
├─ 预计使用: 300-400万 tokens
├─ 每日预算: 约10万 tokens
└─ 剩余: 600万+

测试期 (第5-8周)
├─ 预计使用: 400-500万 tokens
├─ 引入代码模型测试
└─ 剩余: 主模型100万 + 备用400万

上线准备 (第9-12周)
├─ 启用备用模型池
└─ 预计可用: 300万 tokens

生产期 (第13周+)
├─ 启用智能路由
└─ 按需购买付费额度
```

---

## 三、配置文件

### 3.1 Maven依赖

```xml
<dependencies>
    <!-- LangChain4j -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j</artifactId>
        <version>0.34.0</version>
    </dependency>

    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-open-ai</artifactId>
        <version>0.34.0</version>
    </dependency>
</dependencies>
```

### 3.2 application.yml 配置

```yaml
# application-dev.yml
spring:
  profiles:
    active: dev

ai:
  provider: aliyun
  aliyun:
    api-key: ${ALIYUN_API_KEY}
    base-url: https://dashscope.aliyuncs.com/compatible-mode/v1

  # 模型配置
  models:
    # 主力模型
    primary:
      name: qwen-turbo-1101
      bean-name: primaryModel
      quota: 10000000
      temperature: 0.4
      max-tokens: 4096
      timeout: 45

    # 备用模型
    backup:
      - name: qwen-turbo-2025-02-11
        bean-name: turboBackup
        quota: 1000000
      - name: qwen-plus-latest
        bean-name: plusModel
        quota: 1000000

    # 代码专用
    code:
      - name: qwen-coder-plus
        bean-name: coderModel
        quota: 1000000
      - name: qwen-coder-plus-1106
        bean-name: coderModel1106
        quota: 1000000

    # 高级模型
    advanced:
      - name: qwen-max
        bean-name: maxModel
        quota: 1000000
      - name: qwen3-max-2026-01-23
        bean-name: qwen3Max
        quota: 1000000

    # 长上下文
    longcontext:
      - name: qwen-long
        bean-name: longModel
        quota: 2000000

  # 额度管理
  quota:
    primary-threshold: 0.1  # 10%剩余时告警
    daily-max: 500000      # 每日最大使用量
    auto-switch: true      # 自动切换备用模型

  # 路由策略
  routing:
    mode: quota-aware
    primary-first: true

# logging
logging:
  level:
    com.company.kb.ai: DEBUG
  pattern:
    console: "%d{HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

---

## 四、Java配置代码

### 4.1 主配置类

```java
package com.company.kb.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * AI模型配置
 * 基于阿里云百炼平台
 */
@Slf4j
@Configuration
public class AIModelConfig {

    @Value("${ai.aliyun.api-key}")
    private String apiKey;

    @Value("${ai.aliyun.base-url}")
    private String baseUrl;

    // ========== 主力模型 ==========

    /**
     * qwen-turbo-1101 - 主力模型
     * 额度: 1000万 tokens
     * 用途: 开发测试主力
     */
    @Bean("primaryModel")
    @Primary
    public ChatLanguageModel primaryModel() {
        log.info("🚀 初始化主力模型: qwen-turbo-1101");
        return OpenAiChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .modelName("qwen-turbo-1101")
            .temperature(0.4)
            .maxTokens(4096)
            .timeout(Duration.ofSeconds(45))
            .maxRetries(3)
            .build();
    }

    // ========== 备用模型 ==========

    @Bean("turboBackup")
    public ChatLanguageModel turboBackup() {
        return createModel("qwen-turbo-2025-02-11", 0.4, 4096, 45);
    }

    @Bean("plusModel")
    public ChatLanguageModel plusModel() {
        return createModel("qwen-plus-latest", 0.3, 6144, 60);
    }

    // ========== 代码专用 ==========

    @Bean("coderModel")
    public ChatLanguageModel coderModel() {
        return createModel("qwen-coder-plus", 0.2, 4096, 90);
    }

    // ========== 高级模型 ==========

    @Bean("maxModel")
    public ChatLanguageModel maxModel() {
        return createModel("qwen-max", 0.3, 8192, 120);
    }

    // ========== 通用方法 ==========

    private ChatLanguageModel createModel(String modelName, double temperature,
            int maxTokens, int timeoutSeconds) {
        return OpenAiChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .modelName(modelName)
            .temperature(temperature)
            .maxTokens(maxTokens)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .maxRetries(3)
            .build();
    }
}
```

### 4.2 模型路由器

```java
package com.company.kb.service.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI模型路由器
 * 根据任务类型自动选择合适的模型
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIModelRouter {

    private final ChatLanguageModel primaryModel;
    private final ChatLanguageModel turboBackup;
    private final ChatLanguageModel plusModel;
    private final ChatLanguageModel coderModel;
    private final ChatLanguageModel maxModel;
    private final QuotaManager quotaManager;

    /**
     * 根据任务类型选择模型
     */
    public ChatLanguageModel selectModel(TaskType taskType) {
        boolean useBackup = quotaManager.shouldUseBackup();

        return switch (taskType) {
            // 简单任务 - 用主模型或备用
            case SIMPLE_TEST, API_DEBUG, FAST_RESPONSE ->
                useBackup ? turboBackup : primaryModel;

            // 代码任务 - 用代码专用
            case CODE_REVIEW, CODE_GENERATE, BUG_FIX ->
                coderModel;

            // 需要更高质量 - 用plus
            case IMPORTANT_TEST, FEATURE_VALIDATION, TEACHING ->
                useBackup ? plusModel : primaryModel;

            // 复杂任务 - 用max
            case SECURITY_REVIEW, ARCHITECTURE_ANALYSIS ->
                maxModel;

            // 默认用主模型
            default -> useBackup ? turboBackup : primaryModel;
        };
    }

    /**
     * 执行任务
     */
    public String execute(TaskType taskType, String prompt) {
        ChatLanguageModel model = selectModel(taskType);

        try {
            String response = model.generate(prompt);
            quotaManager.recordUsage(prompt, response);
            return response;
        } catch (Exception e) {
            log.error("模型调用失败: {}", e.getMessage());
            return turboBackup.generate(prompt);
        }
    }

    public enum TaskType {
        // 简单任务
        SIMPLE_TEST,      // 简单测试
        API_DEBUG,        // API调试
        FAST_RESPONSE,    // 快速响应

        // 代码任务
        CODE_REVIEW,      // 代码审查
        CODE_GENERATE,    // 代码生成
        BUG_FIX,         // Bug修复

        // 重要任务
        IMPORTANT_TEST,   // 重要测试
        FEATURE_VALIDATION, // 功能验证
        TEACHING,         // 教学辅导

        // 复杂任务
        SECURITY_REVIEW,   // 安全审查
        ARCHITECTURE_ANALYSIS // 架构分析
    }
}
```

### 4.3 额度管理器

```java
package com.company.kb.service.ai;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AI模型额度管理器
 */
@Slf4j
@Service
public class QuotaManager {

    @Value("${ai.quota.primary-threshold:0.1}")
    private double alertThreshold;

    private final AtomicLong usedTokens = new AtomicLong(0);
    private final long primaryQuota = 10000000; // 1000万

    /**
     * 检查是否需要使用备用模型
     */
    public boolean shouldUseBackup() {
        double usagePercent = (double) usedTokens.get() / primaryQuota;
        return usagePercent > (1 - alertThreshold);
    }

    /**
     * 记录token使用
     */
    public void recordUsage(String prompt, String response) {
        int estimated = estimateTokens(prompt + response);
        usedTokens.addAndGet(estimated);

        double usagePercent = (double) usedTokens.get() / primaryQuota;

        if (usagePercent > 0.5 && usagePercent < 0.51) {
            log.info("📊 主模型已使用50%额度");
        } else if (usagePercent > 0.8 && usagePercent < 0.81) {
            log.warn("⚠️ 主模型已使用80%额度");
        } else if (usagePercent > 0.9 && usagePercent < 0.91) {
            log.error("🚨 主模型已使用90%额度！");
        }
    }

    /**
     * 获取额度状态
     */
    public QuotaStatus getQuotaStatus() {
        long used = usedTokens.get();
        return QuotaStatus.builder()
            .model("qwen-turbo-1101")
            .totalQuota(primaryQuota)
            .usedTokens(used)
            .remainingTokens(primaryQuota - used)
            .usagePercent((double) used / primaryQuota * 100)
            .recommendations(generateRecommendations(used))
            .build();
    }

    private int estimateTokens(String text) {
        return text.length() / 2; // 粗略估算
    }

    private List<String> generateRecommendations(long used) {
        List<String> recommendations = new ArrayList<>();
        double usagePercent = (double) used / primaryQuota;

        if (usagePercent < 0.3) {
            recommendations.add("✅ 额度充足，正常使用");
        } else if (usagePercent < 0.7) {
            recommendations.add("⚠️ 额度使用过半，建议适当控制");
        } else {
            recommendations.add("🔴 额度即将耗尽，请切换备用模型");
        }

        return recommendations;
    }

    @Data
    @lombok.Builder
    public static class QuotaStatus {
        private String model;
        private long totalQuota;
        private long usedTokens;
        private long remainingTokens;
        private double usagePercent;
        private List<String> recommendations;
    }
}
```

---

## 五、Agent使用示例

### 5.1 代码审查Agent

```java
package com.company.kb.agent;

import com.company.kb.service.ai.AIModelRouter;
import com.company.kb.service.ai.AIModelRouter.TaskType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 代码审查Agent
 */
@Service
@RequiredArgsConstructor
public class CodeReviewAgent {

    private final AIModelRouter modelRouter;

    /**
     * 审查代码
     */
    public String review(String code, String language) {
        String prompt = String.format("""
            作为代码审查专家，请审查以下%s代码：

            ```%s
            %s
            ```

            请检查：
            1. 代码风格问题
            2. 潜在bug
            3. 性能问题
            4. 安全隐患
            5. 最佳实践建议

            请以JSON格式返回审查结果。
            """, language, language, code);

        return modelRouter.execute(TaskType.CODE_REVIEW, prompt);
    }
}
```

### 5.2 教学Agent

```java
package com.company.kb.agent;

import com.company.kb.service.ai.AIModelRouter;
import com.company.kb.service.ai.AIModelRouter.TaskType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 教学辅导Agent
 */
@Service
@RequiredArgsConstructor
public class TeachingAgent {

    private final AIModelRouter modelRouter;

    /**
     * 解释代码
     */
    public String explainCode(String code, String userLevel) {
        String prompt = String.format("""
            请为%s水平的开发者解释以下代码：

            ```java
            %s
            ```

            请提供：
            1. 代码功能概述
            2. 逐行解释
            3. 关键概念说明
            4. 改进建议
            """, userLevel, code);

        return modelRouter.execute(TaskType.TEACHING, prompt);
    }
}
```

---

## 六、环境变量设置

```bash
# .env 文件

# 阿里云百炼API密钥
ALIYUN_API_KEY=sk-your-api-key-here

# Spring Profile
SPRING_PROFILES_ACTIVE=dev

# 应用端口
SERVER_PORT=8080

# 日志级别
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_COMPANY_KB=DEBUG
```

---

## 七、API接口示例

### 7.1 模型状态查询

```java
@RestController
@RequestMapping("/api/model")
@RequiredArgsConstructor
public class ModelController {

    private final QuotaManager quotaManager;

    /**
     * 获取额度状态
     */
    @GetMapping("/quota/status")
    public QuotaStatus getQuotaStatus() {
        return quotaManager.getQuotaStatus();
    }
}
```

---

## 八、注意事项

### 8.1 开发阶段

1. ✅ **主要使用 `qwen-turbo-1101`**
   - 1000万额度充足
   - 响应速度快
   - 适合快速迭代

2. ✅ **简单任务用主模型**
   - API调试
   - 功能验证
   - 单元测试

3. ⏸️ **暂不启用高级模型**
   - qwen-max系列留到关键时刻
   - 节省额度

### 8.2 测试阶段

1. ✅ **引入代码专用模型**
   - `qwen-coder-plus` 用于代码审查测试
   - 验证代码相关功能

2. ✅ **重要测试提升质量**
   - 使用 `qwen-plus-latest`
   - 获得更准确的测试结果

### 8.3 生产阶段

1. ✅ **启用智能路由**
   - 根据任务自动选择模型
   - 优化成本

2. ✅ **监控额度使用**
   - 设置告警阈值
   - 及时切换备用模型

---

## 九、故障处理

### 9.1 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| API调用失败 | API Key错误 | 检查环境变量 |
| 额度不足 | 用完免费额度 | 切换备用模型或购买 |
| 响应超时 | 网络问题/模型负载 | 增加超时时间或重试 |
| 结果质量差 | 模型选择不当 | 切换到更高级的模型 |

### 9.2 应急预案

```java
/**
 * 应急降级处理
 */
public String emergencyFallback(String prompt) {
    try {
        // 1. 尝试主模型
        return primaryModel.generate(prompt);
    } catch (Exception e1) {
        log.warn("主模型失败，尝试备用: {}", e1.getMessage());
        try {
            // 2. 尝试备用模型
            return turboBackup.generate(prompt);
        } catch (Exception e2) {
            log.error("备用模型也失败: {}", e2.getMessage());
            // 3. 返回错误信息
            return "抱歉，AI服务暂时不可用，请稍后重试";
        }
    }
}
```

---

## 十、更新记录

| 日期 | 版本 | 更新内容 |
|------|------|----------|
| 2025-01-12 | v1.0 | 初始版本，整理所有可用模型 |

---

## 附录

### A. 相关链接

- 阿里云百炼: https://bailian.console.aliyun.com/
- LangChain4j文档: https://docs.langchain4j.dev/
- Spring Boot文档: https://spring.io/projects/spring-boot

### B. 联系方式

如有问题，请联系开发团队。
