# 简化版重构方案

## 目标：扁平化包结构，减少层级

```
com.think.platform
├── api              # 所有 API 控制器
├── domain           # 领域模型和服务
├── infra            # 基础设施
├── shared           # 共享工具类
└── agent            # Agent 框架
```

## 简化原则：
1. 不区分 ci/km 子包
2. 所有 controller 直接放在 api 包下
3. 所有 service 直接放在 domain 包下
4. 使用注解区分不同功能模块