# IntelliJ IDEA 配置 .env 文件支持

## 🎯 问题：.env 文件不会自动加载

Spring Boot 默认**不会自动加载** `.env` 文件，需要额外配置。

## ✅ 解决方案

### 方案1：使用 EnvFile 插件（最推荐）

#### 安装插件

1. 打开 IntelliJ IDEA
2. `File` → `Settings` → `Plugins`
3. 搜索 `EnvFile`
4. 安装 `EnvFile` 插件
5. 重启 IDEA

#### 配置插件

1. `Run` → `Edit Configurations...`
2. 选择你的 Spring Boot 运行配置
3. 找到 `EnvFile` 标签页
4. 勾选 `Enable EnvFile`
5. 点击 `+` 添加 `.env` 文件
   - 路径：`$ProjectFileDir$/.env`

#### 完成！

现在直接点击 IDEA 的运行按钮，`.env` 文件会自动加载！

### 方案2：在运行配置中手动添加环境变量

如果不安装插件：

1. `Run` → `Edit Configurations...`
2. 选择 Spring Boot 运行配置
3. 找到 `Environment variables`
4. 添加环境变量：

```
QWEN_API_KEY=sk-14bcb8044cb047afa50852c40aefeb57
JWT_SECRET=7xC6H4acpIyLKfg1yKZzZZlbOrrbFhEQGIqh8kYhRfxtX+f7xWGyFcGyRjsfUVGHVm/dgysYInm33Bmidfc1mw==
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9093
SPRING_KAFKA_PRODUCER_VALUE_SERIALIZER=org.springframework.kafka.support.serializer.StringSerializer
SPRING_KAFKA_CONSUMER_VALUE_DESERIALIZER=org.springframework.kafka.support.serializer.StringDeserializer
```

### 方案3：命令行启动

**Git Bash / Linux / Mac**:
```bash
# 导出环境变量
export $(cat .env | grep -v '^#' | xargs)
cd backend/knowledge-base-api
mvn spring-boot:run
```

**Windows PowerShell**:
```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match "^([^#].+?)=(.*)$") {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2])
    }
}
cd backend\knowledge-base-api
mvn spring-boot:run
```

## 🧪 验证配置是否生效

启动应用后检查：

```bash
# 应该能加载到API密钥
grep "api-key" logs/*.log

# JWT密钥应该是你配置的值
grep "secret.*length" logs/*.log
```

如果看到空值或默认值，说明 `.env` 没有生效！

---

**推荐**：使用 EnvFile 插件，简单可靠！
