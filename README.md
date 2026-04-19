# 基于多智能体的智能代码审查与教学系统

基于8个专门AI Agent协作的智能代码审查与个性化编程教学系统。

## 功能特性

### 多智能体代码审查
- 🤖 **8个专门AI Agent** - 代码规范、架构、安全、性能全面检查
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

## 技术栈

### 后端
- **框架**: Spring Boot 3.2
- **数据库**: PostgreSQL 15.7
- **搜索引擎**: Elasticsearch 8.11.0
- **缓存**: Redis 7.2.5
- **AI服务**: 阿里云通义千问API
- **认证**: Spring Security + JWT
- **文档解析**: Apache POI, PDFBox

### 前端
- **框架**: React 18 + TypeScript
- **构建工具**: Vite 5
- **UI库**: Ant Design 5
- **状态管理**: Redux Toolkit
- **路由**: React Router 6
- **HTTP客户端**: Axios

## 项目结构

```
code-intelligence-system/
├── backend/
│   ├── services/
│   │   ├── code-intelligence/    # 智能代码审查服务 ⭐
│   │   │   ├── ci-api/             # REST API层
│   │   │   ├── ci-domain/          # 业务逻辑层 + 多智能体框架
│   │   │   ├── ci-infrastructure/  # 外部集成
│   │   │   └── ci-common/          # 共享工具
│   │   └── knowledge-mentor/      # 知识库服务
│   │       ├── km-api/             # REST API层
│   │       ├── km-core/            # 业务逻辑层
│   │       ├── km-infrastructure/  # 外部集成
│   │       └── km-common/          # 共享工具
│   └── shared/                    # 共享模块
│       ├── shared-common/         # 通用工具
│       ├── shared-security/        # 安全模块
│       ├── shared-agent/           # Agent框架
│       ├── shared-ai/              # AI服务
│       └── shared-infrastructure/  # 基础设施
├── frontend/                      # 前端项目
│   └── src/
│       ├── components/            # React组件
│       ├── pages/                 # 页面组件
│       ├── services/              # API服务
│       ├── store/                 # Redux状态
│       └── hooks/                 # 自定义Hooks
└── docker-compose.yml             # Docker编排文件
```

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

修改 `.env` 文件，配置必要的环境变量：

```env
# 数据库配置
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/knowledge_base
SPRING_DATASOURCE_USERNAME=kb_user
SPRING_DATASOURCE_PASSWORD=kb_password

# Elasticsearch配置
ELASTICSEARCH_URIS=http://localhost:9200

# 千问API配置
QWEN_API_KEY=your_qwen_api_key_here

# JWT配置
JWT_SECRET=your_256_bit_secret_key_minimum_32_characters
```

### 2. 启动依赖服务

使用Docker Compose启动PostgreSQL和Elasticsearch：

```bash
docker-compose up -d
```

检查服务状态：

```bash
docker-compose ps
```

### 3. 启动后端服务

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动。

### 4. 启动前端服务

```bash
cd frontend
npm install
npm run dev
```

前端服务将在 `http://localhost:5173` 启动。

### 5. 访问系统

打开浏览器访问 `http://localhost:5173`，使用以下账号登录：

**管理员账号:**
- 用户名: `admin`
- 密码: `admin123`

**测试用户:**
- 用户名: `user`
- 密码: `user123`

## 开发指南

### 后端开发

#### 代码结构

- `controller/` - REST控制器
- `service/` - 业务逻辑服务
- `repository/` - 数据访问层
- `domain/` - JPA实体类
- `config/` - Spring配置类
- `security/` - 安全相关类

#### 添加新的API接口

1. 在 `controller/` 中创建新的控制器
2. 在 `service/` 中实现业务逻辑
3. 在 `repository/` 中定义数据访问方法
4. 在 `domain/` 中定义实体类
5. 配置Spring Security权限（如需要）

### 前端开发

#### 代码结构

- `components/` - 可复用组件
- `pages/` - 页面组件
- `services/` - API服务
- `store/` - Redux状态管理
- `hooks/` - 自定义React Hooks
- `types/` - TypeScript类型定义

#### 添加新页面

1. 在 `pages/` 中创建页面组件
2. 在 `App.tsx` 中添加路由
3. 在 `services/` 中创建API服务（如需要）
4. 在 `store/` 中创建Redux slice（如需要）

## 配置说明

### application.yml 配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `server.port` | 服务端口 | 8080 |
| `spring.datasource.url` | 数据库连接URL | - |
| `elasticsearch.uris` | ES地址 | http://localhost:9200 |
| `qwen.api-key` | 千问API密钥 | - |
| `jwt.expiration` | Token过期时间(毫秒) | 86400000 |

### 千问API配置

1. 访问 [阿里云百炼平台](https://bailian.console.aliyun.com/)
2. 创建API Key
3. 在 `.env` 文件中配置 `QWEN_API_KEY`

## 测试

### 后端测试

```bash
cd backend
mvn test
```

### 前端测试

```bash
cd frontend
npm run test
```

## 部署

### Docker部署

构建并启动所有服务：

```bash
docker-compose up -d
```

### 手动部署

1. 构建后端：

```bash
cd backend
mvn clean package
java -jar knowledge-base-api/target/knowledge-base-api-1.0.0-SNAPSHOT.jar
```

2. 构建前端：

```bash
cd frontend
npm run build
```

构建产物位于 `frontend/dist` 目录，可部署到Nginx或其他静态服务器。

## 故障排除

### 问题1: Elasticsearch连接失败

检查Elasticsearch是否正常运行：

```bash
curl http://localhost:9200
```

### 问题2: 数据库连接失败

检查PostgreSQL是否正常运行：

```bash
docker-compose logs postgres
```

### 问题3: 前端无法连接后端

检查后端服务是否在 `http://localhost:8080` 运行，并确认 `vite.config.ts` 中的代理配置正确。

## 许可证

[MIT License](LICENSE)

## 贡献

欢迎提交Issue和Pull Request！

## 联系方式

如有问题，请联系开发团队。
