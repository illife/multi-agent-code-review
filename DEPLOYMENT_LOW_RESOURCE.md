# 2核2G服务器部署方案

## 架构优化策略

### 1. 微服务合并方案
由于资源限制，建议将部分服务合并：

| 服务 | 端口 | 内存分配 | 说明 |
|------|------|----------|------|
| api-gateway | 8082 | 256MB | 网关路由 |
| auth-service | 8083 | 256MB | 认证服务 |
| knowledge-mentor | 8080 | 384MB | 知识库服务（含ES客户端） |
| code-intelligence | 8088 | 256MB | 代码审查服务 |
| 前端 | 80 | 64MB | Nginx静态文件 |
| 基础设施 | - | 512MB | PostgreSQL(200) + Redis(100) + Kafka(150) + ES(62) |

**总计**: 约1.7GB，留300MB给系统

### 2. 熔断限流配置

使用 Resilience4j 实现：
- **熔断器 (Circuit Breaker)**: 防止级联故障
- **限流器 (Rate Limiter)**: 控制请求速率
- **舱壁隔离 (Bulkhead)**: 资源隔离
- **超时控制 (Timeout)**: 防止长时间阻塞

### 3. 环境配置管理

使用 Spring Profile + 外部配置：
- `application-dev.yml` - 开发环境
- `application-test.yml` - 测试环境
- `application-prod.yml` - 生产环境

## 配置文件结构

```
backend/
├── services/
│   ├── api-gateway/
│   │   └── src/main/resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-test.yml
│   │       └── application-prod.yml
│   └── ...
└── config/
    ├── resilience4j/
    │   ├── circuitbreaker.yml
    │   ├── ratelimiter.yml
    │   └── retry.yml
    └── docker/
        └── jvm-options.conf
```

## 环境变量配置

### 开发环境 (.env.dev)
```bash
SPRING_PROFILES_ACTIVE=dev
JAVA_OPTS="-Xms256m -Xmx512m"
```

### 生产环境 (.env.prod)
```bash
SPRING_PROFILES_ACTIVE=prod
JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

## 部署步骤

### 1. 构建镜像
```bash
docker-compose -f docker-compose.low-resource.yml build
```

### 2. 启动服务
```bash
docker-compose -f docker-compose.low-resource.yml up -d
```

### 3. 健康检查
```bash
curl http://localhost:8082/actuator/health
```

## 监控指标

- JVM 内存使用率 < 80%
- CPU 使用率 < 70%
- 响应时间 P99 < 2s
- 错误率 < 1%
