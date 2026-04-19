# 微服务核心组件详解

## 目录
1. [什么是微服务？](#什么是微服务)
2. [为什么需要服务发现？](#为什么需要服务发现)
3. [Eureka 服务发现](#eureka-服务发现)
4. [Nacos 配置中心](#nacos-配置中心)
5. [Spring Cloud Gateway 网关](#spring-cloud-gateway-网关)
6. [Resilience4j 熔断降级](#resilience4j-熔断降级)
7. [Sleuth + Zipkin 链路追踪](#sleuth--zipkin-链路追踪)
8. [完整架构示例](#完整架构示例)

---

## 什么是微服务？

### 传统单体架构 vs 微服务架构

```
┌─────────────────────────────────────┐
│         单体架构                      │
│  ┌─────────────────────────────┐    │
│  │   用户 + 文档 + 审查 + 教学   │    │
│  └─────────────────────────────┘    │
│           ↓                          │
│      [单一大包部署]                   │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│         微服务架构                    │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐│
│  │ 用户  │ │ 文档  │ │ 审查  │ │ 教学  ││
│  │ 服务  │ │ 服务  │ │ 服务  │ │ 服务  ││
│  └──────┘ └──────┘ └──────┘ └──────┘│
│     ↓        ↓        ↓        ↓      │
│  [独立部署][独立部署][独立部署][独立部署]│
└─────────────────────────────────────┘
```

### 微服务的特点

| 特点 | 单体架构 | 微服务架构 |
|------|---------|-----------|
| **部署** | 一个大包 | 每个服务独立部署 |
| **扩展** | 整体扩展 | 按需扩展特定服务 |
| **技术栈** | 统一技术栈 | 每个服务可选用不同技术 |
| **故障影响** | 一处故障，全站崩溃 | 单个服务故障不影响其他服务 |
| **开发团队** | 大团队协作 | 小团队独立开发 |

---

## 为什么需要服务发现？

### 问题场景

假设你有以下微服务：

```
前端 → 调用 → 用户服务 (8083)
              → 文档服务 (8080)
              → 审查服务 (8081)
```

**没有服务发现的问题：**

1. **硬编码IP地址**
   ```java
   // 前端或服务调用时需要写死地址
   String userServiceUrl = "http://localhost:8083";
   String documentServiceUrl = "http://192.168.1.100:8080";
   ```

2. **服务迁移时需要修改代码**
   - 文档服务从 `192.168.1.100:8080` 迁移到 `192.168.1.101:8080`
   - 所有调用它的服务都需要修改代码并重新部署

3. **动态扩缩容困难**
   - 双倍部署文档服务应对流量高峰
   - 需要手动配置负载均衡器

4. **健康检查复杂**
   - 服务A如何知道服务B是否还活着？

### 服务发现的解决方案

```
┌─────────────────────────────────────────┐
│          服务注册中心                     │
│      (Service Registry)                  │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │ 服务列表：                         │    │
│  │ - user-service: 192.168.1.10:8083│
│  │ - user-service: 192.168.1.11:8083│
│  │ - doc-service:  192.168.1.20:8080│
│  │ - review-service: 192.168.1.30:8081│
│  └─────────────────────────────────┘    │
└─────────────────────────────────────────┘
         ▲               ▲
         │               │
    [注册]          [发现]
         │               │
┌────────┐        ┌────────┐
│服务启动时│        │ 服务A   │
│自动注册 │        │ 需要B时 │
└────────┘        └────────┘
```

**工作流程：**

1. **服务启动** → 自动注册到注册中心
2. **服务A调用服务B** → 先问注册中心："B在哪？"
3. **注册中心返回** → B的所有可用实例地址
4. **服务A调用** → 使用负载均衡选择一个实例

---

## Eureka 服务发现

### 什么是 Eureka？

Eureka 是 Netflix 开发的服务注册与发现组件，现在是 Spring Cloud 生态系统的一部分。

### Eureka 架构

```
┌──────────────────────────────────────────┐
│           Eureka Server                   │
│         (服务注册中心)                     │
│  ┌─────────────────────────────────┐     │
│  │  服务注册表 (Registry)            │     │
│  │  - 所有在线服务实例               │     │
│  │  - 心跳状态                       │     │
│  └─────────────────────────────────┘     │
└──────────────────────────────────────────┘
         ▲                           ▲
         │                           │
    [注册]                      [获取服务列表]
         │                           │
    ┌────┴────┐              ┌─────┴────┐
    │ Eureka  │              │ Eureka   │
    │ Client  │              │ Client   │
    │ (内嵌在  │              │ (内嵌在  │
    │  微服务) │              │  微服务) │
    └─────────┘              └──────────┘
```

### 核心概念

#### 1. Eureka Server（注册中心）
- 维护所有服务实例的信息
- 提供服务发现接口
- 默认端口：8761

#### 2. Eureka Client（服务客户端）
- 每个微服务内嵌的客户端
- 自动注册到Eureka Server
- 自动拉取服务列表
- 自动发送心跳

#### 3. 服务实例 (Service Instance)
```
user-service
  ├── instance-1: 192.168.1.10:8083  (状态: UP)
  ├── instance-2: 192.168.1.11:8083  (状态: UP)
  └── instance-3: 192.168.1.12:8083  (状态: DOWN)  // 故障实例
```

### 实战配置

#### 步骤1：创建 Eureka Server

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

```java
// EurekaServerApplication.java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

```yaml
# application.yml
server:
  port: 8761

spring:
  application:
    name: eureka-server

eureka:
  client:
    register-with-eureka: false  # 不注册自己
    fetch-registry: false         # 不拉取注册信息
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

#### 步骤2：配置微服务作为 Eureka Client

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

```yaml
# application.yml (例如 auth-service)
spring:
  application:
    name: auth-service  # 服务名称，很重要！

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true
    lease-renewal-interval-in-seconds: 10    # 心跳间隔
    lease-expiration-duration-in-seconds: 30 # 过期时间
```

#### 步骤3：服务间调用

```java
// 传统方式（硬编码）
String url = "http://localhost:8083/api/users/" + userId;
// ❌ 服务迁移时需要改代码

// 使用 Eureka（服务发现）
@LoadBalanced
private RestTemplate restTemplate;

public User getUser(Long userId) {
    // 只需要服务名，不需要知道具体地址
    String url = "http://auth-service/api/users/" + userId;
    // ✅ Eureka会自动解析 auth-service 为实际地址
    return restTemplate.getForObject(url, User.class);
}
```

### Eureka 的高可用配置

```
        ┌────────────────┐
        │  Eureka Server │
        │      (1)        │
        └───────┬────────┘
                │ 相互注册
        ┌───────┴────────┐
        │  Eureka Server │
        │      (2)        │
        └────────────────┘
         ▲           ▲
         │           │
    [所有服务注册到两个Server]
```

```yaml
# Eureka Server 1
eureka:
  client:
    service-url:
      defaultZone: http://peer2:8761/eureka/,http://peer1:8761/eureka/

# Eureka Server 2
eureka:
  client:
    service-url:
      defaultZone: http://peer1:8761/eureka/,http://peer2:8761/eureka/
```

---

## Nacos 配置中心

### 为什么需要配置中心？

**问题场景：**

```
开发环境 → application-dev.yml  (数据库: localhost:5432)
测试环境 → application-test.yml (数据库: 192.168.1.100:5432)
生产环境 → application-prod.yml  (数据库: 生产库地址)
```

**传统方式的痛点：**
- 每次打包需要包含不同环境的配置
- 配置变更需要重新打包部署
- 不同服务的公共配置需要复制多份

### Nacos 的功能

```
┌────────────────────────────────────────┐
│           Nacos Server                 │
│  ┌────────────────────────────────┐   │
│  │  配置管理                        │   │
│  │  - auth-service.yml            │   │
│  │  - doc-service.yml             │   │
│  │  - review-service.yml          │   │
│  │  - 支持版本管理                 │   │
│  │  - 支持灰度发布                 │   │
│  ├────────────────────────────────┤   │
│  │  服务注册                        │   │
│  │  - 替代 Eureka                   │   │
│  └────────────────────────────────┘   │
└────────────────────────────────────────┘
         ▲               ▲
         │               │
   [配置推送]      [服务拉取]
         │               │
    ┌────┴────┐    ┌─────┴────┐
    │ 服务A   │    │  服务B   │
    │(客户端) │    │  (客户端) │
    └─────────┘    └──────────┘
```

### Nacos vs Eureka

| 功能 | Eureka | Nacos |
|------|--------|-------|
| **服务注册发现** | ✅ | ✅ |
| **配置中心** | ❌ 需要配合 Spring Cloud Config | ✅ 内置 |
| **控制台** | 简单 | 功能丰富（Web UI）|
| **AP/Namespace** | ❌ | ✅ 支持多环境 |
| **动态配置** | ❌ | ✅ 实时推送 |
| **国内生态** | 一般 | 阿里维护，国内友好 |

### Nacos 配置示例

#### 步骤1：启动 Nacos Server

```bash
# 下载 Nacos
wget https://github.com/alibaba/nacos/releases/download/2.2.3/nacos-server-2.2.3.zip
unzip nacos-server-2.2.3.zip
cd nacos/bin

# 单机模式启动
sh startup.sh -m standalone

# 访问控制台
http://localhost:8848/nacos
用户名/密码: nacos/nacos
```

#### 步骤2：微服务配置

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
```

```yaml
# bootstrap.yml (注意：配置优先级高于 application.yml)
spring:
  application:
    name: auth-service
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        namespace: public
      config:
        server-addr: localhost:8848
        file-extension: yml
        namespace: public
        group: DEFAULT_GROUP
```

```java
// 使用 @Value 注入动态配置
@RefreshScope  // 支持配置刷新
@RestController
public class ConfigController {

    @Value("${custom.config:value}")
    private String configValue;

    @GetMapping("/config")
    public String getConfig() {
        return configValue;
    }
}
```

#### 步骤3：在 Nacos 控制台管理配置

1. 登录 Nacos 控制台 (http://localhost:8848/nacos)
2. 进入「配置管理」→「配置列表」
3. 点击「+」创建配置：
   - Data ID: `auth-service.yml`
   - Group: `DEFAULT_GROUP`
   - 配置内容：
   ```yaml
   database:
     url: jdbc:postgresql://localhost:5432/multi_agent_platform
     username: kb_user
     password: kb_password
   ```

---

## Spring Cloud Gateway 网关

### 网关的作用

```
前端请求
    │
    ▼
┌─────────────────────────────────────┐
│         API Gateway                   │  ← 统一入口
│  ┌────────────────────────────────┐  │
│  │  路由转发                       │  │
│  │  /api/auth/* → auth-service   │  │
│  │  /api/doc/* → doc-service     │  │
│  └────────────────────────────────┘  │
│  ┌────────────────────────────────┐  │
│  │  认证授权                       │  │
│  │  - JWT验证                      │  │
│  │  - 权限校验                      │  │
│  └────────────────────────────────┘  │
│  ┌────────────────────────────────┐  │
│  │  限流熔断                       │  │
│  │  - 防止雪崩                     │  │
│  └────────────────────────────────┘  │
└─────────────────────────────────────┘
    │       │        │
    ▼       ▼        ▼
┌────────┐ ┌──────┐ ┌────────┐
│ Auth   │ │ Doc  │ │ Review │
│Service │ │Service│ │Service │
└────────┘ └──────┘ └────────┘
```

### 路由配置示例

```yaml
spring:
  cloud:
    gateway:
      routes:
        # 路由1: 认证服务
        - id: auth-route
          uri: lb://auth-service  # lb = LoadBalancer
          predicates:
            - Path=/api/auth/**

        # 路由2: 文档服务（需要限流）
        - id: doc-route
          uri: lb://doc-service
          predicates:
            - Path=/api/documents/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10  # 每秒恢复10个令牌
                redis-rate-limiter.burstCapacity: 20  # 桶容量20

        # 路由3: 审查服务（需要认证）
        - id: review-route
          uri: lb://review-service
          predicates:
            - Path=/api/review/**
          filters:
            - StripPrefix=0  # 不去掉前缀
            - name: AuthenticationFilter  # 自定义认证过滤器
```

---

## Resilience4j 熔断降级

### 什么是熔断？

```
正常情况：
服务A → [调用] → 服务B → [返回] → 服务A
         ↓
      正常响应

服务B故障：
服务A → [调用] → 服务B ❌
         ↓
      长时间等待...系统卡死

使用熔断器：
服务A → [调用] → [熔断器] → 服务B ❌
                  ↓
               [快速失败] → 返回降级响应
```

### 熔断器状态

```
        ┌──────────┐
        │   关闭   │ ← 正常请求通过
        └─────┬────┘
              │ 故障率 > 阈值
              ↓
        ┌──────────┐
        │   打开   │ ← 快速失败，返回降级数据
        └─────┬────┘
              │ 半开状态尝试
              ↓
        ┌──────────┐
        │   半开   │ ← 尝试少量请求
        └─────┬────┘
              │ 成功 → 关闭，失败 → 打开
              ↓
        [循环...]
```

### 配置示例

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
</dependency>
```

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-size: 100  # 滑动窗口大小
        minimumNumberOfCalls: 10    # 最小调用次数
        failureRateThreshold: 50     # 失败率阈值(%)
        waitDurationInOpenState: 10000  # 打开状态等待时间(ms)
        permittedNumberOfCallsInHalfOpenState: 5  # 半开状态允许调用次数

  instances:
    auth-service:  # 针对特定服务的配置
      baseConfig: default
      failureRateThreshold: 60

  timelimiter:
    configs:
      default:
        timeoutDuration: 3000  # 超时时间(ms)
```

```java
// 使用熔断器
@CircuitBreaker(name = "auth-service", fallbackMethod = "fallback")
public User getUser(Long userId) {
    return restTemplate.getForObject(
        "http://auth-service/api/users/" + userId,
        User.class
    );
}

// 降级方法
public User fallback(Long userId, Exception e) {
    log.error("Auth service unavailable, using fallback", e);
    return User.builder()
        .id(userId)
        .username("unknown")
        .build();
}
```

---

## Sleuth + Zipkin 链路追踪

### 为什么需要链路追踪？

**问题场景：**

```
用户请求 → 网关 → 认证服务 → 文档服务 → 审查服务 → 返回结果
                     ↓
                 响应慢！哪个服务出问题了？
```

### 链路追踪原理

```
每个请求生成一个 TraceID，贯穿所有服务：

请求: GET /api/documents/123
  TraceID: abc123-def456...

┌─────────┐   ┌─────────┐   ┌─────────┐
│ Gateway │   │  Auth   │   │  Doc    │
│         │──→│         │──→│         │
│ abc123  │   │ abc123  │   │ abc123  │
└─────────┘   └─────────┘   └─────────┘

Zipkin 收集所有服务的日志，可视化展示调用链
```

### 配置示例

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-sleuth-zipkin</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  sleuth:
    zipkin:
      base-url: http://localhost:9411
      sender:
        type: web
  zipkin:
    sender:
      type: web
```

启动 Zipkin Server：
```bash
docker run -d -p 9411:9411 openzipkin/zipkin
```

访问 Zipkin UI: http://localhost:9411

---

## 完整架构示例

### 推荐架构（国内生态）

```
┌──────────────────────────────────────────────────────┐
│                    前端应用                         │
│                   (localhost:5173)                  │
└────────────────────┬───────────────────────────────┘
                     │
                     ▼
┌──────────────────────────────────────────────────────┐
│              Nacos Gateway (网关+路由)                │
│                   (localhost:8080)                  │
└────────────────────┬───────────────────────────────┘
                     │
                     ▼
┌──────────────────────────────────────────────────────┐
│              Nacos Server (注册+配置中心)              │
│                   (localhost:8848)                  │
│  ┌────────────────────────────────────────────────┐ │
│  │  服务注册表                                      │ │
│  │  - auth-service: 192.168.1.10:8083             │ │
│  │  - doc-service: 192.168.1.20:8080              │ │
│  │  - review-service: 192.168.1.30:8081           │ │
│  ├────────────────────────────────────────────────┤ │
│  │  配置管理                                        │ │
│  │  - 数据库连接配置                               │ │
│  │  - Redis配置                                     │ │
│  │  - 第三方API密钥                                 │ │
│  └────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────┘
         ▲              ▲              ▲
         │              │              │
    ┌────┴────┐   ┌────┴────┐   ┌────┴────┐
    │  Auth   │   │  Doc    │   │ Review  │
    │Service  │   │Service  │   │Service  │
    │ :8083   │   │ :8080   │   │ :8081   │
    └─────────┘   └─────────┘   └─────────┘
         │              │              │
         └──────────────┴──────────────┘
                        │
                        ▼
        ┌─────────────────────────────────┐
        │  PostgreSQL (multi_agent_...    │
        │  Redis                           │
        │  Elasticsearch                   │
        │  Kafka                           │
        │  MinIO                           │
        └─────────────────────────────────┘
```

### 启动顺序

```bash
# 1. 启动基础设施
docker-compose up -d postgres redis elasticsearch kafka minio

# 2. 启动 Nacos
cd nacos/bin
sh startup.sh -m standalone

# 3. 启动后端服务（会自动注册到Nacos）
cd backend/services/auth
mvn spring-boot:run

cd backend/services/knowledge-mentor
mvn spring-boot:run

cd backend/services/code-intelligence
mvn spring-boot:run

# 4. 启动网关
cd backend/services/api-gateway
mvn spring-boot:run
```

---

## 你的项目建议

### 最小化方案（当前适用）

```
前端 → API Gateway (8080) → 后端服务
```

**优点：**
- 简单，快速上手
- 网关统一处理认证、跨域
- 减少前端配置

**建议：**
- 暂时不引入 Eureka/Nacos
- 使用静态服务配置即可
- 等服务增多后再引入服务发现

### 进阶方案（服务增多后）

引入 **Nacos** 同时解决：
- 服务注册发现
- 配置统一管理

### 完整方案（生产环境）

- **Nacos**：服务注册 + 配置中心
- **Gateway**：统一网关
- **Resilience4j**：熔断降级
- **Sleuth + Zipkin**：链路追踪
- **Prometheus + Grafana**：监控告警

---

## 下一步

需要我帮你：
1. **配置 Eureka** - 创建注册中心
2. **配置 Nacos** - 替代静态配置
3. **配置 Resilience4j** - 添加熔断保护
4. **完整示例项目** - 搭建完整的微服务示例

选择哪个方向继续？
