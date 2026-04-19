#!/bin/bash
# 启动代码审查服务 (Code Intelligence Service)
# Port: 8081

cd "$(dirname "$0")"
SERVICE_DIR="services/code-intelligence/ci-api"

echo "=================================="
echo "  Starting Code Intelligence Service"
echo "  Port: 8081"
echo "=================================="

if [ ! -d "$SERVICE_DIR" ]; then
    echo "Error: Service directory not found: $SERVICE_DIR"
    exit 1
fi

cd "$SERVICE_DIR"

# 检查端口是否被占用
if netstat -ano | grep ":8081" | grep -q "LISTENING"; then
    echo "Warning: Port 8081 is already in use"
    echo "Please stop the process using port 8081 first"
    exit 1
fi

echo "Compiling and starting Code Intelligence Service..."
mvn spring-boot:run

# 或者使用已编译的 JAR 启动：
# mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dserver.port=8081"
# 或者：java -jar target/ci-api-1.0.0-SNAPSHOT.jar --server.port=8081
