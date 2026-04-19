# 基于多智能体的智能代码审查与教学系统 API 文档

## 服务架构

### 微服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| **Gateway** | 8082 | API网关（统一入口） |
| **Auth Service** | 8083 | 认证服务 |
| **Code Intelligence** | 8088 | 代码审查服务 |
| **Knowledge Mentor** | 8080 | 教学服务 |

### 前端访问

所有请求通过网关统一路由：
- 开发环境：`http://localhost:8082/api`
- 前端端口：5173

---

## 一、认证接口 (`/api/auth`)

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | `/login` | 用户登录 | ❌ |
| POST | `/register` | 用户注册 | ❌ |
| GET | `/validate` | 验证Token | ✅ |
| POST | `/refresh` | 刷新Token | ✅ |
| GET | `/me` | 获取当前用户信息 | ✅ |
| PUT | `/profile` | 更新用户资料 | ✅ |
| POST | `/password/change` | 修改密码 | ✅ |
| POST | `/logout` | 用户登出 | ✅ |

---

## 二、代码审查接口 (`/api/review`)

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | `/submit` | 提交代码审查（同步） | ✅ |
| POST | `/submit-async` | 提交代码审查（异步） | ✅ |
| GET | `/{reviewId}` | 获取审查详情 | ✅ |
| GET | `/list` | 获取用户的审查列表 | ✅ |
| GET | `/{reviewId}/issues` | 获取审查问题列表 | ✅ |
| DELETE | `/{reviewId}` | 删除审查记录 | ✅ |

---

## 三、项目管理接口 (`/api/project`)

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | `/upload/zip` | 上传ZIP项目文件 | ✅ |
| GET | `/{projectId}/status` | 获取项目状态 | ✅ |
| GET | `/{projectId}/files` | 获取项目文件列表 | ✅ |
| GET | `/{projectId}/report` | 获取项目报告 | ✅ |
| DELETE | `/{projectId}` | 删除项目 | ✅ |
| GET | `/list` | 获取用户项目列表 | ✅ |
| POST | `/{projectId}/generate-report` | 生成项目报告 | ✅ |

---

## 四、多智能体任务接口 (`/api/agent`) ⭐ 核心功能

### 4.1 代码审查任务

| 方法 | 路径 | 描述 | 涉及智能体 |
|------|------|------|----------|
| POST | `/tasks/code-review` | 创建代码审查任务 | 规范、架构、安全、性能 |
| POST | `/tasks/code-review/execute` | 创建并执行代码审查任务 | 4个审查智能体 |

### 4.2 学习路径任务

| 方法 | 路径 | 描述 | 涉及智能体 |
|------|------|------|----------|
| POST | `/tasks/learning-path` | 创建学习路径任务 | 学习路径规划师 |
| POST | `/tasks/learning-path/execute` | 创建并执行学习路径任务 | 学习路径规划师 |

### 4.3 练习题生成任务

| 方法 | 路径 | 描述 | 涉及智能体 |
|------|------|------|----------|
| POST | `/tasks/exercise` | 创建练习题生成任务 | 练习教练 |
| POST | `/tasks/exercise/execute` | 创建并执行练习题生成任务 | 练习教练 |

### 4.4 智能问答任务

| 方法 | 路径 | 描述 | 涉及智能体 |
|------|------|------|----------|
| POST | `/tasks/qa` | 提问获取AI回答 | 教学导师 |

### 4.5 任务管理

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/tasks/{taskId}` | 获取任务状态 |
| GET | `/tasks/{taskId}/executions` | 获取各智能体执行记录 |
| GET | `/tasks/{taskId}/messages` | 获取智能体间通信消息 |
| GET | `/tasks/{taskId}/statistics` | 获取任务统计信息 |
| GET | `/tasks` | 获取用户所有任务 |

---

## 五、学习管理接口 (`/api/learning`) ⭐

### 学习路径管理

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/paths` | 创建学习路径 |
| GET | `/paths/{pathId}` | 获取学习路径详情 |
| GET | `/paths` | 获取用户学习路径列表 |
| GET | `/paths/active` | 获取活跃学习路径 |
| PUT | `/paths/{pathId}/progress` | 更新学习进度 |

### 练习题管理

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/exercises/{exerciseId}` | 获取练习题详情 |
| GET | `/exercises` | 获取练习题列表 |
| POST | `/exercises/generate` | 生成练习题 |
| POST | `/exercises/{exerciseId}/submit` | 提交代码并评分 |
| GET | `/exercises/{exerciseId}/submissions` | 获取提交历史 |
| GET | `/exercises/{exerciseId}/best` | 获取最佳提交 |

### 学习统计

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/statistics` | 获取学习统计信息 |

---

## 六、AI教学接口 (`/api/teaching`) ⭐

### 进度追踪

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/progress` | 获取用户总体学习进度 |
| POST | `/progress/{learningPathId}/start` | 开始学习路径 |
| PUT | `/progress/{learningPathId}/update` | 更新学习进度 |
| POST | `/progress/{learningPathId}/complete` | 完成学习路径 |

### 技能评估

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/skills` | 获取用户技能列表 |
| GET | `/skills/{language}` | 获取特定语言技能 |
| GET | `/skills/summary` | 获取技能摘要 |
| GET | `/skills/recommendations` | 获取技能提升建议 |

### 练习题管理

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/exercises` | 获取练习题列表 |
| GET | `/exercises/{id}` | 获取练习题详情 |
| POST | `/exercises/{id}/attempt` | 提交练习尝试 |
| GET | `/exercises/{id}/hints` | 获取提示 |
| GET | `/exercises/recommended` | 获取推荐练习 |
| GET | `/exercises/summary` | 获取练习统计 |

### 成就系统

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/achievements` | 获取成就列表 |
| GET | `/achievements/user` | 获取用户成就 |
| GET | `/achievements/progress` | 获取成就进度 |
| GET | `/achievements/leaderboard` | 获取排行榜 |
| GET | `/achievements/xp-leaderboard` | 获取经验值排行榜 |

### AI教学助手

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/explain` | 获取AI代码解释 |
| GET | `/issue/{issueId}/explanation` | 获取问题的教学解释 |

---

## 七、8个智能体说明

| 智能体 | 代码 | 职责 | 输出 |
|--------|------|------|------|
| **代码规范检查员** | `CODE_STANDARDS_INSPECTOR` | 检查命名规范、代码风格、注释完整性 | 规范问题列表 + 教学解释 |
| **架构守护者** | `ARCHITECTURE_GUARDIAN` | 分析设计模式、模块划分、依赖关系 | 架构改进建议 + 最佳实践 |
| **安全审计员** | `SECURITY_AUDITOR` | 检测SQL注入、XSS、依赖漏洞 | 安全问题报告 + 修复方案 |
| **性能优化师** | `PERFORMANCE_OPTIMIZER` | 分析性能瓶颈、资源使用 | 优化建议 + 性能指标 |
| **教学导师** | `TEACHING_MENTOR` | 代码解释、概念讲解、最佳实践 | 教学内容 + 学习建议 |
| **技能评估师** | `SKILL_ASSESSOR` | 评估编程技能水平 | 技能画像 + 成长轨迹 |
| **练习教练** | `EXERCISE_COACH` | 生成个性化练习题 | 练习题 + 评分标准 |
| **学习路径规划师** | `LEARNING_PATH_PLANNER` | 制定学习计划 | 学习路径图 + 里程碑 |

---

## 八、健康检查

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/actuator/health` | 健康检查 |
| GET | `/actuator/prometheus` | Prometheus指标 |

---

**项目名称：基于多智能体的智能代码审查与教学系统**

**核心技术：8个专门AI Agent协作的多智能体架构**
