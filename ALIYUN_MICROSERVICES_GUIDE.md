# 阿里系微服务生态详解

## 目录
1. [Higress 云原生网关](#higress-云原生网关)
2. [Nacos 服务注册与发现](#nacos-服务注册与发现)
3. [Dubbo RPC 框架](#dubbo-rpc-框架)
4. [Sentinel 流量控制](#sentinel-流量控制)
5. [Seata 分布式事务](#seata-分布式事务)
6. [RocketMQ 消息队列](#rocketmq-消息队列)
7. [完整架构示例](#完整架构示例)

---

## Higress 云原生网关

### 什么是 Higress？

Higress 是阿里云开源的**云原生 API 网关**，基于 Envoy 和 Istio 构建。

### 与 Spring Cloud Gateway 对比

| 特性 | Spring Cloud Gateway | Higress |
|------|---------------------|---------|
| **技术栈** | Spring WebFlux (Netty) | Envoy (C++/高性能) |
| **性能** | 一般 (~5k QPS) | 高性能 (~100k QPS) |
| **协议支持** | HTTP/gRPC | HTTP/gRPC/WebSocket/Dubbo |
| **热更新** | 需要重启 | 无需重启 |
| **控制台** | ❌ | ✅ 可视化配置 |
| **WSGI/Dubbo** | ❌ | ✅ 原生支持 |
| **K8s集成** | 一般 | 优秀 |
| **开源程度** | 完全开源 | 完全开源 |

### Higress 核心特性

#### 1. 高性能路由
```
┌─────────────────────────────────────────────┐
│              Higress Gateway                │
│  ┌───────────────────────────────────────┐  │
│  │  路由表 (路由匹配)                    │  │
│  │  - /api/auth/* → auth-service        │  │
│  │  - /api/doc/*  → doc-service         │  │
│  │  - /dubbo/*   → dubbo-provider       │  │
│  └───────────────────────────────────────┘  │
│  ┌───────────────────────────────────────┐  │
│  │  Envoy 代理 (C++高性能)              │  │
│  │  - 连接池管理                         │  │
│  │  - 负载均衡                           │  │
│  │  - 熔断重试                           │  │
│  └───────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

#### 2. 插件机制

```
请求处理流程：

请求 → [认证插件] → [限流插件] → [路由插件] → [Dubbo插件] → 后端服务
         ↓            ↓            ↓            ↓
      JWT验证     Redis计数    URL匹配      Dubbo协议转换
```

#### 3. Dubbo 直接支持

```
传统方案：
前端 → HTTP网关 → HTTP转Dubbo → Dubbo服务
                  ↓
            额外转换层，性能损耗

Higress方案：
前端 → Higress → Dubbo服务
         ↓
      直接Dubbo协议，无转换损耗
```

### Higress vs Spring Cloud Gateway 架构对比

```
┌──────────────────────────────────────────────────────────┐
│         Spring Cloud Gateway 架构                        │
│  ┌──────────┐      ┌──────────┐      ┌──────────┐       │
│  │ 前端应用  │ ───→ │ Gateway  │ ───→ │ Dubbo转  │ ───→ │ Dubbo   │
│  └──────────┘      └──────────┘      │ HTTP层   │       │ Provider│
│                                      └──────────┘       └──────────┘│
│  ┌──────────┐      ┌──────────┐                              │
│  │ 前端应用  │ ───→ │ Gateway  │ ────────────────────→ │ HTTP    │
│  └──────────┘      └──────────┘                              │
│                                                             服务        │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│              Higress 架构                                │
│  ┌──────────┐      ┌──────────┐                           │
│  │ 前端应用  │ ───→ │ Higress  │ ─────────────────→ │ Dubbo   │
│  └──────────┘      └──────────┘                    Provider │
│  ┌──────────┐      ┌──────────┐                           │
│  │ 前端应用  │ ───→ │ Higress  │ ──────→ │ HTTP    │
│  └──────────┘      └──────────┘       Service        │
└──────────────────────────────────────────────────────────┘
```

### Higress 安装和配置

#### 方式1：Docker 安装（推荐）

```bash
# 拉取 Higress 镜像
docker pull higress-registry.cn-hangzhou.cr.aliyuncs.com/higress/gateway:1.3.0

# 运行 Higress
docker run -d \
  --name higress-gateway \
  -p 8080:8080 \
  -p 8443:8443 \
  -v /path/to/config:/etc/higress \
  higress-registry.cn-hangzhou.cr.aliyuncs.com/higress/gateway:1.3.0

# 访问控制台
http://localhost:8080/
```

#### 方式2：Kubernetes 安装

```bash
# 添加 Higress Helm 仓库
helm repo add higress https://higress.io/helm-charts
helm repo update

# 安装 Higress
helm install higress higress/higress \
  --namespace higress-system \
  --create-namespace \
  --set controlPlane.replicas=1 \
  --set gateway.replicas=2
```

### Higress 配置示例

#### HTTP 路由配置

```yaml
# routes.yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: auth-service-route
spec:
  parentRefs:
    - name: higress-gateway
  hostnames:
    - "api.example.com"
  rules:
    - matches:
        - path:
            type: PathPrefix
            value: /api/auth
      backendRefs:
        - group: default
          kind: Service
          name: auth-service
          port: 8083
```

#### Dubbo 路由配置

```yaml
apiVersion: networking.higress.io/v1
kind: DubboRoute
metadata:
  name: dubbo-user-service
spec:
  services:
    - name: user-service
      registry:
        type: nacos
      nacos:
        service-name: user-service
        namespace-id: public
        group: DEFAULT_GROUP
  methods:
    - name: getUser
      params:
        - name: userId
          type: string
```

### Higress 控制台功能

```
┌─────────────────────────────────────────────┐
│          Higress Console                    │
│  ┌─────────────────────────────────────┐  │
│  │  路由管理                             │  │
│  │  - 可视化添加/编辑路由               │  │
│  │  - 支持HTTP/WebSocket/gRPC           │  │
│  │  - 支持Dubbo直接路由                  │  │
│  ├─────────────────────────────────────┤  │
│  │  插件管理                             │  │
│  │  - 认证授权                           │  │
│  │  - 限流熔断                           │  │
│  │  - 自定义插件                         │  │
│  ├─────────────────────────────────────┤  │
│  │  服务发现                             │  │
│  │  - 集成Nacos/Eureka                   │  │
│  │  - 健康检查                           │  │
│  ├─────────────────────────────────────┤  │
│  │  监控告警                             │  │
│  │  - 实时QPS监控                        │  │
│  │  - 延迟监控                           │  │
│  └─────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

---

## Nacos 服务注册与发现

### Nacos 架构深度解析

```
┌─────────────────────────────────────────────────────┐
│                  Nacos Server                        │
│  ┌────────────────────────────────────────────────┐  │
│  │              Raft一致性协议                  │  │
│  │  ┌────────┐  ┌────────┐  ┌────────┐      │  │
│  │  │Leader │  │Follower│  │Follower│      │  │
│  │  └────────┘  └────────┘  └────────┘      │  │
│  └────────────────────────────────────────────────┘  │
│  ┌────────────────────────────────────────────────┐  │
│  │              服务注册模块                     │  │
│  │  - Distro协议 (gRPC)                       │  │
│  │  - SDK: Java, Go, Python, Node.js...       │  │
│  └────────────────────────────────────────────────┘  │
│  ┌────────────────────────────────────────────────┐  │
│  │              配置管理模块                     │  │
│  │  - 配置CRUD                                │  │
│  │  - 配置监听/推送                            │  │
│  │  - 灰度发布                                │  │
│  └────────────────────────────────────────────────┘  │
│  ┌────────────────────────────────────────────────┐  │
│  │              控制台 (8848)                   │  │
│  │  - 服务列表/详情                           │  │
│  │  - 配置列表/详情                           │  │
│  │  - 命名空间管理                            │  │
│  └────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

### 服务注册流程

```
1. 服务启动
   │
   ├─> 读取配置文件
   │   spring.application.name=auth-service
   │   spring.cloud.nacos.server-addr=localhost:8848
   │
2. 连接 Nacos Server
   │
   ├─> 发起注册请求
   │   POST /nacos/v1/ns/instance
   │   {
   │     "serviceName": "auth-service",
   │     "ip": "192.168.1.10",
   │     "port": 8083,
   │     "weight": 1,
   │     "healthy": true,
   │     "metadata": {
   │       "version": "1.0.0",
   │       "region": "beijing"
   │     }
   │   }
   │
3. 心跳维持
   │   每5秒发送心跳
   │   PUT /nacos/v1/ns/instance/beat
   │
4. 服务下线
   │   DELETE /nacos/v1/ns/instance
```

### 服务发现流程

```
服务A需要调用服务B

1. 服务A从Nacos获取服务B的实例列表
   GET /nacos/v1/ns/instance/list?serviceName=doc-service

   响应:
   {
     "hosts": [
       {
         "ip": "192.168.1.20",
         "port": 8080,
         "weight": 1,
         "healthy": true,
         "metadata": {...}
       },
       {
         "ip": "192.168.1.21",
         "port": 8080,
         "weight": 1,
         "healthy": true,
         "metadata": {...}
       }
     ]
   }

2. 服务A使用负载均衡策略选择一个实例
   - 随机
   - 加权随机
   - 最少连接
   - 一致性哈希

3. 发起调用
   HTTP GET http://192.168.1.20:8080/api/documents/123
```

### Nacos 高级特性

#### 1. 命名空间 (Namespace)

```
开发环境 Namespace: dev
  ├── auth-service (dev实例)
  ├── doc-service (dev实例)

测试环境 Namespace: test
  ├── auth-service (test实例)
  ├── doc-service (test实例)

生产环境 Namespace: prod
  ├── auth-service (prod实例)
  ├── doc-service (prod实例)
```

```yaml
spring:
  cloud:
    nacos:
      discovery:
        namespace: dev  # 开发环境
```

#### 2. 配置管理

```java
// 动态刷新配置
@RefreshScope  // 支持配置自动刷新
@RestController
public class ConfigController {

    @Value("${app.feature.enabled:false}")
    private Boolean featureEnabled;

    @GetMapping("/feature")
    public Boolean isFeatureEnabled() {
        return featureEnabled;
    }
}
```

在Nacos控制台修改配置后，应用会自动刷新。

#### 3. 配置监听器

```java
@Component
public class ConfigListener {

    @NacosConfigListener(dataId = "app.properties", groupId = "DEFAULT_GROUP")
    public void onConfigChange(String newConfig) {
        System.out.println("配置已更新: " + newConfig);
        // 触发业务逻辑更新
    }
}
```

---

## Dubbo RPC 框架

### 什么是 RPC？

RPC (Remote Procedure Call) 远程过程调用

```
本地调用：
User user = userService.getUser(123L);

RPC调用（像本地调用一样）：
User user = remoteUserService.getUser(123L);
        ↑
        │ 实际上是网络调用
        ▼
[HTTP/TCP] → 远程服务 → 返回结果
```

### HTTP vs RPC 对比

| 特性 | HTTP REST | Dubbo RPC |
|------|-----------|-----------|
| **协议** | HTTP | TCP (自定义协议) |
| **序列化** | JSON | Hessian2 (二进制) |
| **性能** | 一般 | 高性能 |
| **传输** | 文本大 | 二进制小 |
| **复杂度** | 简单易懂 | 需要接口定义 |
| **调用方式** | 同步 | 同步/异步 |
| **负载均衡** | 客户端 | 客户端/服务端 |

### Dubbo 架构

```
┌─────────────────────────────────────────────────────┐
│                  Dubbo 架构                          │
│                                                      │
│  ┌──────────────────────────────────────────────┐   │
│  │  Registry (注册中心)                         │   │
│  │  - Nacos / Zookeeper / Redis                │   │
│  │  - 服务地址注册                             │   │
│  │  - 订阅服务变化                             │   │
│  └──────────────────────────────────────────────┘   │
│         ↑                      ↑                      │
│         │                      │                      │
│    [注册]              [订阅]                      │
│         │                      │                      │
│  ┌──────┴──────┐      ┌─────┴────┐                   │
│  │ Provider  │      │ Consumer │                   │
│  │ (服务提供) │      │ (服务调用)│                   │
│  └───────────┘      └───────────┘                   │
│         ▲                      │                      │
│         │                      │                      │
│    [调用]                  [调用]                     │
│         │                      │                      │
│  ┌──────┴──────┐      ┌─────┴────┐                   │
│  │ Provider  │      │ Consumer │                   │
│  │  服务B   │      │  服务A  │                   │
│  └───────────┘      └───────────┘                   │
│                                                      │
└─────────────────────────────────────────────────────┘
```

### Dubbo 调用链路

```
服务调用链路：

Consumer ──→ [TCP直连] ──→ Provider
  │                           │
  ├─> 1. 从注册中心获取Provider地址
  ├─> 2. 建立TCP连接 (长连接池)
  ├─> 3. 发送Dubbo协议请求 (Hessian2序列化)
  ├─> 4. Provider处理并返回结果
  └─> 5. Consumer反序列化结果

对比 HTTP：

HTTP调用链路：

Consumer ──→ [HTTP请求] ──→ Gateway ──→ [HTTP] ──→ Provider
  │                                            │
  ├─> 1. DNS解析
  ├─> 2. 建立TCP连接
  ├─> 3. 发送HTTP请求 (JSON序列化)
  ├─> 4. Gateway路由转发
  ├─> 5. Provider处理并返回
  └─> 6. 网关返回给Consumer
```

### Dubbo 接口定义

#### 定义服务接口

```java
// API模块 (服务提供者 + 消费者共用)
public interface UserService {
    User getUser(Long userId);
    List<User> listUsers();
}
```

#### 服务提供者实现

```java
// Provider服务实现
@Service(version = "1.0.0")
public class UserServiceImpl implements UserService {

    @Override
    public User getUser(Long userId) {
        // 实现逻辑
        return user;
    }

    @Override
    public List<User> listUsers() {
        return userDao.findAll();
    }
}
```

```xml
<!-- Provider配置 -->
<dubbo:service
    interface="com.example.UserService"
    version="1.0.0"
    ref="userServiceImpl"
    timeout="3000"
/>
```

#### 服务消费者调用

```java
// Consumer服务
@RestController
public class ConsumerController {

    @Reference(version = "1.0.0")  // Dubbo注解，注入远程服务
    private UserService userService;

    @GetMapping("/user/{id}")
    public User getUser(@PathVariable Long id) {
        // 像调用本地方法一样
        return userService.getUser(id);
    }
}
```

### Dubbo 与 Spring Cloud 对比

```
┌──────────────────────────────────────────────────────┐
│           Spring Cloud (HTTP REST)                     │
│  ┌──────────┐      ┌──────────┐      ┌──────────┐     │
│  │ 服务A    │ ───→ │ Gateway  │ ───→ │ 服务B    │     │
│  └──────────┘      └──────────┘      └──────────┘     │
│      HTTP 请求      路由转发       HTTP 请求          │
│      JSON           额外层        JSON                │
│      同步调用        延迟增加      同步调用            │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│              Dubbo (RPC)                               │
│  ┌──────────┐                    ┌──────────┐          │
│  │ 服务A    │ ───────────────────→ │ 服务B    │          │
│  │Consumer │                    │ Provider  │          │
│  └──────────┘                    └──────────┘          │
│       TCP直连，Dubbo协议，Hessian2序列化              │
│       高性能，低延迟，长连接池                          │
└──────────────────────────────────────────────────────┘
```

### Dubbo 3.x 新特性

#### 三种调用方式

```java
// 1. 接口调用 (推荐)
@Reference
private UserService userService;

// 2. 泛化调用
GenericService genericService = ...;
Object result = genericService.$invoke("getUser", new Object[]{userId});

// 3. REST调用 (Dubbo 3.x支持)
@Reference
@DubboTransport("rest")
private UserService userService;
```

#### 支持 HTTP/REST

```java
@DubboService(version = "1.0.0", protocol = "rest")
@RestController
@RequestMapping("/users")
public class UserServiceImpl implements UserService {

    @GetMapping("/{id}")
    @Override
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
}
```

---

## Sentinel 流量控制

### 什么是流量控制？

```
┌─────────────────────────────────────────────────────┐
│              流量控制系统                             │
│                                                       │
│  正常情况：每秒1000个请求                             │
│  ┌─────────────────────────────────────────────┐   │
│  │  请求 │ 请求 │ 请求 │ ... │ 请求 │         │   │
│  └─────────────────────────────────────────────┘   │
│         │        │     │                        │
│         ▼        ▼     ▼                        ▼
│  ┌─────────────────────────────────────────────┐   │
│  │              后端服务                       │   │
│  └─────────────────────────────────────────────┘   │
│                                                       │
│  流量突增：每秒10000个请求 (系统崩溃❌)               │
│  ┌─────────────────────────────────────────────┐   │
│  │请求请求请求请求请求请求请求...请求...请求  │   │
│  └─────────────────────────────────────────────┘   │
│         │        │     │                        │
│         ▼        ▼     ▼                        ▼
│  ┌─────────────────────────────────────────────┐   │
│  │              后端服务 (崩溃💥)              │   │
│  └─────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘

使用Sentinel流量控制：

┌─────────────────────────────────────────────────────┐
│  每秒10000个请求 → Sentinel → 每秒放行1000个           │
│                         │                         │
│                         │                         ▼
│                    其余9000个被拒绝              ┌─────────────────┐
│                         │                         │  后端服务(正常)  │
│                         ▼                         └─────────────────┘
│                    返回"系统繁忙"                 │
└─────────────────────────────────────────────────────┘
```

### Sentinel 核心概念

#### 1. 资源 (Resource)

```
资源 = 任何需要保护的对象

示例：
- 代码：getUser() 方法
- 接口：/api/users/{id}
- 服务：整个应用
```

#### 2. 规则 (Rule)

```
流控规则：
  - QPS (每秒查询数)
  - 线程数
  - 平均响应时间

熔断规则：
  - 慢调用比例
  - 异常比例
  - 熔断时长

系统规则：
  - CPU使用率
  - 系统负载
```

#### 3. 流控策略

```
1. 直接拒绝
   请求 → [QPS检查] → 超过阈值 → 拒绝 ❌
                     ↓
                  返回 "系统繁忙"

2. Warm Up (预热)
   请求 → [预热] → 逐渐放行
   系统冷启动时保护

3. 匀速排队
   请求 → [队列] → 匀速通过
   削峰填谷
```

### Sentinel 配置示例

#### 代码方式定义规则

```java
@RestController
public class UserController {

    @GetMapping("/user/{id}")
    @SentinelResource(
        value = "getUser",
        blockHandler = "handleBlock"  // 降级方法
    )
    public Result<User> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return Result.success(user);
    }

    // 降级方法
    public Result<User> handleBlock(Long id, BlockException ex) {
        return Result.error("系统繁忙，请稍后再试");
    }
}
```

#### 配置方式定义规则

```java
@Configuration
public class SentinelRuleConfig {

    @PostConstruct
    public void initRules() {
        // 流控规则
        List<FlowRule> rules = new ArrayList<>();
        FlowRule rule = new FlowRule()
            .setResource("getUser")  // 资源名
            .setGrade(RuleConstant.FLOW_GRADE_QPS)  // QPS限流
            .setCount(100);  // 每秒100个请求
        rules.add(rule);

        FlowRuleManager.loadRules(rules);
    }
}
```

#### 控制台动态配置

```bash
# 启动 Sentinel 控制台
java -Dserver.port=8080 -Dcsp.sentinel.dashboard.server.addr=127.0.0.1 \
     -jar sentinel-dashboard-1.8.6.jar
```

访问控制台：http://localhost:8080

### Sentinel 与 Nacos 集成

```
┌─────────────┐         ┌──────────────┐
│  微服务应用   │         │  Sentinel    │
│             │────────→│  Dashboard   │
│  │ @Sentinel  │         │              │
│  │  资源       │         │  动态配置     │
│  └───────────┘         └──────┬───────┘
│         ▲                     │
│         │ 拉照规则推送配置     │
│         ▼                     │
│  ┌─────────────┐         ┌───┴──────┐
│  │   Nacos     │←────────│ Nacos   │
│  │ (配置中心)  │ 规则持久化│ (存储)   │
│  └─────────────┘         └──────────┘
```

**配置步骤：**

1. 应用配置：
```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8080  # Sentinel控制台
      datasource:
        nacos:  # 使用Nacos存储规则
          server-addr: localhost:8848
          dataId: ${spring.application.name}-sentinel-rules
          groupId: SENTINEL_GROUP
          rule-type: flow
```

2. 在控制台配置规则，自动同步到Nacos

3. 应用监听Nacos配置变化，更新本地规则

---

## Seata 分布式事务

### 什么是分布式事务问题？

```
┌──────────────────────────────────────────────────────┐
│              分布式事务场景                            │
│                                                       │
│  用户下单：                                           │
│  1. 创建订单 → 订单服务 ✓                             │
│  2. 扣减库存 → 库存服务 ✓                            │
│  3. 增加积分 → 积分服务 ❌ (服务宕机)                  │
│                                                       │
│  结果：订单和库存都扣减了，但积分没加 → 数据不一致     │
│                                                       │
│  需要分布式事务保证一致性                             │
└──────────────────────────────────────────────────────┘
```

### Seata 分布式事务模式

#### 1. AT 模式（默认，推荐）

```
两阶段提交协议：

阶段一：业务执行和SQL解析
  ┌─────────────┐
  │  订单服务    │
  │ INSERT INTO │
  │  orders...   │
  └──────┬──────┘
         │
         ▼
  [Seata自动拦截SQL]
         │
  [解析SQL]
  SELECT * FROM orders WHERE id = ?
  → 记录: before image (修改前数据)
         │
  [执行业务SQL]
  INSERT INTO orders ...
         │
  [记录: after image (修改后数据)]
         │
  [获取锁]
  INSERT INTO undo_log ...

阶段二：提交或回滚
  如果所有服务都成功：
    [删除undo_log] [释放锁] [提交事务]

  如果任何服务失败：
    [读取undo_log] [生成反向SQL] [执行回滚] [释放锁]
```

#### 2. TCC 模式

```
Try-Confirm-Cancel 三阶段：

Try阶段：
  try {
      // 冻结库存
      inventoryService.freeze(userId, productId, quantity);
      return true;
  }

Confirm阶段：
  confirm {
      // 扣减库存
      inventoryService.deduct(productId, quantity);
      // 释放冻结
      inventoryService.release(userId, productId, quantity);
  }

Cancel阶段：
  cancel {
      // 释放冻结
      inventoryService.release(userId, productId, quantity);
  }
```

### Seata 配置

#### 启动 Seata Server

```yaml
# file.conf
store:
  mode: file
  file:
    dir: "./sessionStore"

# registry.conf
registry {
  type: nacos
  nacos {
    application: seata-server
    serverAddr: localhost:8848
    namespace: public
    group: SEATA_GROUP
    cluster: default
  }
}
```

```bash
# 启动 Seata Server
docker run -d --name seata-server \
  -p 8091:8091 \
  -e SEATA_IP=192.168.1.10 \
  seataio/seata-server:1.7.0
```

#### 微服务配置

```yaml
# application.yml
seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: default_tx_group
  registry:
    nacos:
      server-addr: localhost:8848
      namespace: public
      group: SEATA_GROUP
      cluster: default
      username: nacos
      password: nacos
  config:
    nacos:
      server-addr: localhost:8848
      namespace: public
      group: SEATA_GROUP
      username: nacos
      password: nacos
```

#### 使用 @GlobalTransactional

```java
@GlobalTransactional(name = "create-order")
public void createOrder(OrderRequest request) {
    // 1. 创建订单
    orderService.createOrder(request);

    // 2. 扣减库存
    inventoryService.deduct(request.getProductId(), request.getQuantity());

    // 3. 增加积分
    pointService.add(request.getUserId(), request.getPoints());

    // 任何步骤失败，Seata自动回滚所有操作
}
```

---

## RocketMQ 消息队列

### RocketMQ 特性

| 特性 | Kafka | RabbitMQ | RocketMQ |
|------|-------|---------|---------|
| **吞吐量** | 最高 | 中等 | 高 |
| **延迟消息** | ❌ | ✅ (插件) | ✅ (原生) |
| **事务消息** | ❌ | ✅ | ✅ (原生) |
| **定时消息** | ❌ | ❌ | ✅ (原生) |
| **消息回溯** | ❌ | ✅ | ✅ (原生) |
| **国内生态** | 一般 | 一般 | 优秀 |

### RocketMQ 核心概念

```
NameServer: 注册中心
  ┌─────────────┐
  │ NameServer  │
  │  :9876      │
  └──────┬──────┘
         │
    [Broker注册/心跳]

Broker: 消息服务器
  ┌─────────────┐
  │  Broker     │
  │  :10911     │
  └──────┬──────┘
         │
    [存储消息]

Producer: 消息生产者
  ┌─────────────┐
  │  Producer   │
  └──────┬──────┘
         │
    [发送消息]

Consumer: 消息消费者
  ┌─────────────┐
  │  Consumer   │
  └──────┬──────┘
         │
    [拉取消息]
```

### RocketMQ 消息模型

```
Topic: 主题 (消息类别)
  └── OrderCreatedEvent (订单创建事件)

Tag: 标签 (子类别)
  ├── electronics (电子产品)
  ├── books (图书)
  └── clothing (服装)

Message: 消息体
  {
    "orderId": "123",
    "userId": "456",
    "amount": 99.99
  }

消费模式：
  - 集群消费 (相同GroupId只消费一次)
  - 广播消费 (所有消费者都消费)
```

### RocketMQ 配置

#### 启动 RocketMQ

```bash
# 下载 RocketMQ
wget https://archive.apache.org/rocketmq/5.1.0/rocketmq-all-5.1.0-bin-release.zip

# 启动 NameServer
cd rocketmq-all-5.1.0-bin-release/bin
nohup sh mqnamesrv -n localhost:9876 &

# 启动 Broker
nohup sh mqbroker -n localhost:9876 &
```

#### 生产者配置

```java
@RestController
public class OrderController {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @PostMapping("/order")
    public Result createOrder(@RequestBody Order order) {
        // 1. 创建订单
        orderService.create(order);

        // 2. 发送消息
        Message<Order> msg = MessageBuilder
            .withPayload(order)
            .build();

        rocketMQTemplate.syncSend(
            "OrderCreatedTopic:electronics",  // Topic:Tag
            msg,
            "order-created-key-" + order.getId()
        );

        return Result.success();
    }
}
```

#### 消费者配置

```java
@RocketMQMessageListener(
    topic = "OrderCreatedTopic",
    consumerGroup = "inventory-service-group",
    selectorExpression = "electronics"  // 只消费电子产品
)
public class OrderConsumer implements RocketMQListener<Order> {

    @Override
    public void onMessage(Order order) {
        // 处理订单
        inventoryService.deduct(order.getProductId(), order.getQuantity());
    }
}
```

---

## 完整架构示例

### 阿里系微服务完整架构

```
┌───────────────────────────────────────────────────────────┐
│                         前端层                               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                 │
│  │  Web应用  │  │  移动App  │  │  小程序   │                 │
│  └─────┬────┘  └─────┬────┘  └─────┬────┘                 │
└────────┼──────────┼──────────┼──────────────────────────────┘
         │          │          │
         ▼          ▼          ▼
┌───────────────────────────────────────────────────────────┐
│                     Higress 网关                            │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  插件：认证、限流、日志、监控                          │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  路由：HTTP、gRPC、WebSocket、Dubbo                    │   │
│  └─────────────────────────────────────────────────────┘   │
└────────────────────────────┬────────────────────────────┘
                                 │
                ┌──────────────┴──────────────┐
                │     Nacos Server     │
                │    (8848)             │
                │  ┌────────────────────┐│
                │  │ 服务注册           ││
                │  │ 配置管理           ││
                │  │ 配置监听           ││
                │  └────────────────────┘│
                └──────────────────────┘
                 ▲                    ▲
    ┌────────────────────┐    ┌────────────────────┐
    │  Sentinel Dashboard │    │   Seata Server    │
    │      (8080)          │    │      (8091)        │
    │  流量控制、熔断       │    │  分布式事务        │
    └────────────────────┘    └────────────────────┘
         ▲
         │
    ┌────┴─────┬─────────┬─────────┐
    │         │         │         │
┌───┴────┐ ┌──┴────┐ ┌────┴───┐ ┌───┴────┐
│Auth │ │User │ │Order   │ │Inventory│
│Svc  │ │Svc  │ │Svc    │ │Svc     │
└─────┘ └──────┘ └────────┘ └────────┘
    │        │         │         │
    └────────┴─────────┴─────────┘
                        │
                  ┌──────────────────────┐
                  │   RocketMQ Broker     │
                  │     (10911)            │
                  │  [消息队列]            │
                  └──────────────────────┘
```

### 服务职责划分

| 服务 | 职责 | 端口 | 使用技术 |
|------|------|------|---------|
| **Auth Service** | 用户认证授权 | 8083 | Spring Security + JWT |
| **User Service** | 用户管理 | 8083 | Dubbo 3.x |
| **Order Service** | 订单管理 | 8081 | Spring Boot + OpenFeign |
| **Inventory Service** | 库存管理 | 8082 | Spring Boot + Dubbo |
| **MQ Consumer** | 消息处理 | 8084 | RocketMQ |
| **Scheduler** | 定时任务 | 8085 | XXL-Job |

### 通信方式

```
同步通信 (RPC):
  Order ──(Dubbo)──→ User
  Order ──(Dubbo)──→ Inventory
  Higress ──(Dubbo)──→ All Services

异步通信 (MQ):
  Order ──(RocketMQ)──→ Inventory
  Order ──(RocketMQ)──→ Point
  Order ──(RocketMQ)──→ Notification
```

---

## 你的项目如何选择

### 当前项目：3个服务，中等复杂度

**推荐方案：**
```
1. API Gateway (Spring Cloud Gateway) ✓ 已创建
2. Nacos (服务注册+配置)
3. Sentinel (流量控制)
4. 暂不需要 Dubbo (HTTP REST够用)
5. 暂不需要 Seata (可先手动补偿)
```

### 进阶方案：服务增多后

```
1. Higress (替换 Gateway) - 高性能
2. Nacos (已使用)
3. Dubbo 3.x - 核心服务RPC调用
4. Sentinel - 流量保护
5. Seata - 分布式事务
6. RocketMQ - 事件驱动
```

---

## 下一步

需要我帮你配置哪个组件？
1. **Nacos** - 完整配置示例
2. **Sentinel** - 流控规则配置
3. **Dubbo 3.x** - RPC调用示例
4. **完整Demo** - 搭建完整的阿里系微服务示例项目
