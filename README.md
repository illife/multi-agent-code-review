# 基于多智能体的智能代码审查与教学系统

AI-powered code review system with multi-agent architecture.

基于8个专门AI Agent协作的智能代码审查与个性化编程教学系统。

## 功能特性

### 多智能体代码审查
- 🤖 **多个专门AI Agent** - 代码规范、架构、安全、性能全面检查
- 🔄 **智能体协作** - Agent间通信与信息共享
- 📊 **综合审查报告** - 多维度问题分析与改进建议
- 🎯 **教学解释** - 每个问题附带教学性解释

### 个性化AI教学
- 📈 **技能评估** - 实时跟踪编程技能水平
- 🛤️ **学习路径规划** - 根据目标自动生成学习计划
- ✍️ **智能练习题生成** - 根据技能点生成针对性练习
- 🏆 **成就系统** - 学习进度可视化与激励机制

### 项目管理
- 📦 **项目上传** - 支持ZIP格式项目包上传
- 🔍 **代码分析** - 自动扫描项目文件
- 📝 **审查历史** - 完整的审查记录与对比
- 📊 **项目报告** - 自动生成项目质量报告

### 知识库与智能问答
- 📚 **文档上传** - 支持多种格式（PDF, DOCX, TXT等）
- 🔍 **混合搜索** - BM25 + 向量搜索
- 💬 **AI问答** - 基于RAG的智能问答
- 📄 **来源引用** - 答案附带文档来源

## 技术栈

### 后端
- **框架**: Spring Boot 3.2
- **数据库**: PostgreSQL 15.7
- **搜索引擎**: Elasticsearch 8.11.0
- **缓存**: Redis 7.2.5
- **消息队列**: Apache Kafka 7.5.3
- **对象存储**: MinIO
- **AI服务**: 阿里云通义千问API
- **认证**: Spring Security + JWT

### 前端
- **框架**: React 18 + TypeScript
- **构建工具**: Vite 5
- **UI库**: Ant Design
- **状态管理**: Redux Toolkit
- **路由**: React Router 6
- **HTTP客户端**: Axios

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- Maven 3.8+
- Docker & Docker Compose

### 1. 配置环境变量

复制环境变量示例文件并修改配置：

```bash
cp .env.example .env
```

### 2. 启动依赖服务

```bash
docker-compose up -d
```

### 3. 启动后端服务

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### 4. 启动前端服务

```bash
cd frontend
npm install
npm run dev
```

### 5. 访问系统

打开浏览器访问 `http://localhost:5173`

## 项目结构

```
├── backend/               # 后端服务
│   ├── services/
│   │   ├── code-intelligence/  # 代码审查服务
│   │   ├── knowledge-mentor/   # 知识库服务
│   │   └── auth/               # 认证服务
│   └── shared/                 # 共享模块
├── frontend/              # 前端项目
│   └── src/
│       ├── components/    # React组件
│       ├── pages/         # 页面组件
│       ├── services/      # API服务
│       └── store/         # Redux状态
└── docker-compose.yml    # Docker编排文件
```

## 许可证

MIT License

## 贡献

欢迎提交Issue和Pull Request！

## 联系方式

如有问题，请联系开发团队。
