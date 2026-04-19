# API网关服务使用指南

## 概述

API网关服务（api-gateway）是多智能体平台的统一API入口，负责：

- **请求路由**：将请求转发到相应的后端服务
- **认证授权**：统一JWT token验证
- **跨域处理**：统一配置CORS
- **限流熔断**：防止服务过载
- **API文档聚合**：统一展示所有服务的API文档

## 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| **API网关** | 8080 | 统一入口 |
| Auth服务 | 8083 | 认证授权 |
| Code Intelligence服务 | 8081 | 代码智能 |
| Knowledge Mentor服务 | 原端口需要调整 | 知识库 |

## 路由规则

### 认证服务 (Auth Service)
- `/api/auth/**` → `auth-service:8083`
- `/api/users/**` → `auth-service:8083` (需要ADMIN角色)

### 代码智能服务 (Code Intelligence)
- `/api/agent/**` → `code-intelligence-service:8081` (多智能体)
- `/api/learning/**` → `code-intelligence-service:8081` (AI教学)
- `/api/projects/**` → `code-intelligence-service:8081` (项目管理)
- `/api/review/**` → `code-intelligence-service:8081` (代码审查)
- `/api/analysis/**` → `code-intelligence-service:8081` (代码分析)
- `/api/teaching/**` → `code-intelligence-service:8081` (教学)

### 知识库服务 (Knowledge Mentor)
- `/api/documents/**` → `knowledge-mentor-service:8080` (文档管理)
- `/api/qa/**` → `knowledge-mentor-service:8080` (问答)
- `/api/search/**` → `knowledge-mentor-service:8080` (搜索)
- `/api/lessons/**` → `knowledge-mentor-service:8080` (课程)
- `/api/exercises/**` → `knowledge-mentor-service:8080` (练习)
- `/api/ws/**` → `knowledge-mentor-service:8080` (WebSocket)

## 认证流程

### 1. 无需认证的接口
- `/api/auth/login` - 登录
- `/api/auth/register` - 注册
- `/api/auth/forgot-password` - 忘记密码
- `/api/auth/reset-password` - 重置密码
- `/api/auth/validate` - Token验证
- `/actuator/**` - 健康检查
- `/doc.html` - API文档

### 2. 需要认证的接口
其他所有接口都需要JWT token。网关会：
1. 从`Authorization`头提取token
2. 验证token有效性
3. 提取用户信息（userId, username, role）
4. 添加到请求头转发给后端服务

### 3. 需要管理员权限的接口
- `/api/users/**` - 用户管理（需要ADMIN角色）

## 启动网关服务

### 方式1：Maven命令行
```bash
cd backend/services/api-gateway
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 方式2：Docker Compose
```bash
docker-compose up -d api-gateway
```

## API文档访问

启动网关后，访问：
- **Knife4j UI**: http://localhost:8080/doc.html
- **Swagger UI**: http://localhost:8080/swagger-ui.html

网关会自动聚合所有后端服务的API文档。

## 前端配置

前端已配置为通过网关访问所有API：

```typescript
// vite.config.ts
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
    ws: true,
  }
}
```

## 故障排查

### 1. 网关无法启动
- 检查8080端口是否被占用
- 确认Redis服务已启动（用于限流）
- 检查后端服务是否都已启动

### 2. API请求401
- 确认JWT token格式正确：`Bearer <token>`
- 检查token是否过期
- 查看网关日志中的认证错误

### 3. API请求403
- 确认用户角色是否正确
- 检查是否需要ADMIN权限

### 4. 服务间通信失败
- 确认后端服务都已启动
- 检查服务地址配置是否正确
- 查看网关日志中的路由错误

## 监控端点

- `/actuator/health` - 健康检查
- `/actuator/metrics` - Prometheus指标
- `/actuator/gateway/routes` - 查看所有路由

## 生产环境配置

生产环境建议：
1. 使用服务发现（Eureka/Consul）替代静态服务实例
2. 配置真实的Redis集群
3. 启用HTTPS/TLS
4. 配置请求限流参数
5. 关闭Knife4j文档（设置`knife4j.production=true`）
