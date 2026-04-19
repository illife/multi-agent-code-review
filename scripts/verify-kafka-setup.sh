#!/bin/bash

# Kafka功能验证脚本
# 用于验证企业知识库系统的Kafka功能是否完整配置

echo "========================================"
echo "Kafka功能验证"
echo "========================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查计数器
PASS_COUNT=0
FAIL_COUNT=0

# 检查函数
check_pass() {
    echo -e "${GREEN}✓${NC} $1"
    ((PASS_COUNT++))
}

check_fail() {
    echo -e "${RED}✗${NC} $1"
    ((FAIL_COUNT++))
}

check_warn() {
    echo -e "${YELLOW}⚠${NC} $1"
}

echo "1. 检查Docker服务"
echo "----------------------------------------"

# 检查Kafka容器
if docker ps | grep -q "kb-kafka"; then
    check_pass "Kafka容器正在运行"
else
    check_fail "Kafka容器未运行，请执行: docker-compose up -d kafka"
fi

# 检查Zookeeper容器
if docker ps | grep -q "kb-zookeeper"; then
    check_pass "Zookeeper容器正在运行"
else
    check_fail "Zookeeper容器未运行"
fi

echo ""
echo "2. 检查Kafka Topics"
echo "----------------------------------------"

# 检查document-processing topic
if docker exec kb-kafka kafka-topics.sh --list --bootstrap-server localhost:9093 2>/dev/null | grep -q "document-processing"; then
    check_pass "Topic 'document-processing' 已创建"
else
    check_warn "Topic 'document-processing' 尚未创建（将在应用启动时自动创建）"
fi

# 检查document-processing-dlt topic
if docker exec kb-kafka kafka-topics.sh --list --bootstrap-server localhost:9093 2>/dev/null | grep -q "document-processing-dlt"; then
    check_pass "Topic 'document-processing-dlt' 已创建"
else
    check_warn "Topic 'document-processing-dlt' 尚未创建（将在应用启动时自动创建）"
fi

echo ""
echo "3. 检查Kafka配置文件"
echo "----------------------------------------"

# 检查KafkaConfig.java
if [ -f "backend/knowledge-base-infrastructure/src/main/java/com/company/kb/infra/kafka/KafkaConfig.java" ]; then
    check_pass "KafkaConfig.java 存在"

    # 检查是否包含DefaultErrorHandler
    if grep -q "DefaultErrorHandler" "backend/knowledge-base-infrastructure/src/main/java/com/company/kb/infra/kafka/KafkaConfig.java"; then
        check_pass "KafkaConfig包含DefaultErrorHandler配置"
    else
        check_fail "KafkaConfig缺少DefaultErrorHandler配置"
    fi

    # 检查是否包含DeadLetterPublishingRecoverer
    if grep -q "DeadLetterPublishingRecoverer" "backend/knowledge-base-infrastructure/src/main/java/com/company/kb/infra/kafka/KafkaConfig.java"; then
        check_pass "KafkaConfig包含DeadLetterPublishingRecoverer配置"
    else
        check_fail "KafkaConfig缺少DeadLetterPublishingRecoverer配置"
    fi
else
    check_fail "KafkaConfig.java 不存在"
fi

# 检查KafkaTopicConfig.java
if [ -f "backend/knowledge-base-infrastructure/src/main/java/com/company/kb/infra/kafka/KafkaTopicConfig.java" ]; then
    check_pass "KafkaTopicConfig.java 存在"
else
    check_fail "KafkaTopicConfig.java 不存在"
fi

# 检查DocumentProcessingConsumer.java
if [ -f "backend/knowledge-base-core/src/main/java/com/company/kb/core/consumer/DocumentProcessingConsumer.java" ]; then
    check_pass "DocumentProcessingConsumer.java 存在"

    # 检查是否正确抛出异常
    if grep -q "throw new RuntimeException" "backend/knowledge-base-core/src/main/java/com/company/kb/core/consumer/DocumentProcessingConsumer.java"; then
        check_pass "DocumentProcessingConsumer正确抛出异常"
    else
        check_fail "DocumentProcessingConsumer未正确抛出异常"
    fi
else
    check_fail "DocumentProcessingConsumer.java 不存在"
fi

# 检查DeadLetterQueueConsumer.java
if [ -f "backend/knowledge-base-core/src/main/java/com/company/kb/core/consumer/DeadLetterQueueConsumer.java" ]; then
    check_pass "DeadLetterQueueConsumer.java 存在"
else
    check_fail "DeadLetterQueueConsumer.java 不存在"
fi

# 检查KafkaHealthIndicator.java
if [ -f "backend/knowledge-base-api/src/main/java/com/company/kb/config/KafkaHealthIndicator.java" ]; then
    check_pass "KafkaHealthIndicator.java 存在"
else
    check_fail "KafkaHealthIndicator.java 不存在"
fi

echo ""
echo "4. 检查application.yml配置"
echo "----------------------------------------"

# 检查Kafka bootstrap-servers配置
if grep -q "spring.kafka.bootstrap-servers" "backend/knowledge-base-api/src/main/resources/application.yml"; then
    check_pass "Kafka bootstrap-servers 已配置"
else
    check_fail "Kafka bootstrap-servers 未配置"
fi

# 检查Kafka topic配置
if grep -q "spring.kafka.topic.document-processing" "backend/knowledge-base-api/src/main/resources/application.yml"; then
    check_pass "Kafka topic配置已添加"
else
    check_fail "Kafka topic配置缺失"
fi

# 检查Kafka健康检查配置
if grep -q "management.health.kafka.enabled" "backend/knowledge-base-api/src/main/resources/application.yml"; then
    check_pass "Kafka健康检查已启用"
else
    check_warn "Kafka健康检查未明确配置（将使用默认值）"
fi

echo ""
echo "5. 检查Maven依赖"
echo "----------------------------------------"

# 检查API模块是否有actuator依赖
if grep -q "spring-boot-starter-actuator" "backend/knowledge-base-api/pom.xml"; then
    check_pass "API模块包含actuator依赖"
else
    check_fail "API模块缺少actuator依赖"
fi

# 检查infrastructure模块是否有Kafka依赖
if grep -q "spring-kafka" "backend/knowledge-base-infrastructure/pom.xml"; then
    check_pass "Infrastructure模块包含Kafka依赖"
else
    check_fail "Infrastructure模块缺少Kafka依赖"
fi

echo ""
echo "6. 检查消息发送配置"
echo "----------------------------------------"

# 检查DocumentServiceImpl是否发送Kafka消息
if grep -q "kafkaTemplate.send" "backend/knowledge-base-core/src/main/java/com/company/kb/core/service/impl/DocumentServiceImpl.java"; then
    check_pass "DocumentServiceImpl发送Kafka消息"
else
    check_warn "未找到Kafka消息发送代码（可能在其他Service中）"
fi

echo ""
echo "========================================"
echo "验证结果汇总"
echo "========================================"
echo -e "${GREEN}通过检查: $PASS_COUNT${NC}"
echo -e "${RED}失败检查: $FAIL_COUNT${NC}"
echo ""

if [ $FAIL_COUNT -eq 0 ]; then
    echo -e "${GREEN}✓ Kafka功能配置完整！${NC}"
    echo ""
    echo "下一步操作："
    echo "1. 启动应用: cd backend && mvn spring-boot:run"
    echo "2. 上传文档测试: 使用Postman或前端上传文档"
    echo "3. 检查健康状态: curl http://localhost:8080/api/actuator/health/kafka"
    echo "4. 查看Kafka消息: docker exec -it kb-kafka kafka-console-consumer.sh --bootstrap-server localhost:9093 --topic document-processing --from-beginning"
    exit 0
else
    echo -e "${RED}✗ Kafka功能配置存在问题，请修复后重试${NC}"
    exit 1
fi
