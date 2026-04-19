#!/bin/bash
# 知识库搜索问题诊断脚本

echo "===================================="
echo "知识库系统诊断"
echo "===================================="

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. 检查 Elasticsearch
echo ""
echo "1. 检查 Elasticsearch..."
ES_HEALTH=$(curl -s http://localhost:9200/_cluster/health 2>/dev/null | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
if [ "$ES_HEALTH" = "green" ] || [ "$ES_HEALTH" = "yellow" ]; then
    echo -e "${GREEN}✓ Elasticsearch 状态: $ES_HEALTH${NC}"
else
    echo -e "${RED}✗ Elasticsearch 不可用${NC}"
fi

# 检查索引是否存在
INDEX_EXISTS=$(curl -s "http://localhost:9200/_cat/indices/kb_document_chunks?h=index" 2>/dev/null)
if [ -n "$INDEX_EXISTS" ]; then
    DOC_COUNT=$(curl -s "http://localhost:9200/_cat/indices/kb_document_chunks?h=docs.count" 2>/dev/null)
    echo -e "${GREEN}✓ ES 索引存在，文档数: $DOC_COUNT${NC}"
else
    echo -e "${RED}✗ ES 索引不存在${NC}"
fi

# 2. 检查 Kafka
echo ""
echo "2. 检查 Kafka..."
if docker exec kb-kafka kafka-topics.sh --list --bootstrap-server localhost:9092 2>/dev/null | grep -q "document-processing"; then
    echo -e "${GREEN}✓ Kafka topic 'document-processing' 存在${NC}"

    # 检查消费者组
    CONSUMER_LAG=$(docker exec kb-kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group document-processing-group --describe 2>/dev/null | grep -v "GROUP" | awk '{sum+=$5} END {print sum+0}')
    if [ -n "$CONSUMER_LAG" ] && [ "$CONSUMER_LAG" -gt 0 ]; then
        echo -e "${YELLOW}⚠ 消费者组有积压: $CONSUMER_LAG 条消息${NC}"
    else
        echo -e "${GREEN}✓ 消费者组正常，无积压${NC}"
    fi
else
    echo -e "${RED}✗ Kafka topic 'document-processing' 不存在${NC}"
fi

# 3. 检查数据库
echo ""
echo "3. 检查数据库文档状态..."
DB_CHECK=$(docker exec kb-postgres psql -U kb_user -U postgres -d knowledge_base -t -c "SELECT status, COUNT(*) FROM documents GROUP BY status;" 2>/dev/null)
if [ -n "$DB_CHECK" ]; then
    echo "文档状态统计:"
    echo "$DB_CHECK" | while read STATUS COUNT; do
        if [ "$STATUS" = "INDEXED" ]; then
            echo -e "${GREEN}  $STATUS: $COUNT${NC}"
        elif [ "$STATUS" = "PROCESSING" ]; then
            echo -e "${YELLOW}  $STATUS: $COUNT${NC}"
        elif [ "$STATUS" = "FAILED" ]; then
            echo -e "${RED}  $STATUS: $COUNT${NC}"
        else
            echo "  $STATUS: $COUNT"
        fi
    done
else
    echo -e "${RED}✗ 无法连接数据库${NC}"
fi

# 4. 检查后端服务日志
echo ""
echo "4. 检查后端服务日志..."
echo "最近的 Kafka 消费记录:"
docker logs kb-km-api 2>&1 | grep -i "documentprocessingconsumer" | tail -5 || echo "  无日志"

echo ""
echo "最近的错误:"
docker logs kb-km-api 2>&1 | grep -i "error" | tail -5 || echo "  无错误"

# 5. 测试搜索API
echo ""
echo "5. 测试搜索API..."
SEARCH_RESULT=$(curl -s -X POST http://localhost:8080/api/search/hybrid \
  -H "Content-Type: application/json" \
  -d '{"query":"test"}' 2>/dev/null)
if echo "$SEARCH_RESULT" | grep -q "total"; then
    TOTAL=$(echo "$SEARCH_RESULT" | grep -o '"total":[0-9]*' | cut -d':' -f2)
    echo -e "${GREEN}✓ 搜索API响应正常，结果数: $TOTAL${NC}"
else
    echo -e "${RED}✗ 搜索API无响应${NC}"
fi

# 6. 检查 Qwen API 配置
echo ""
echo "6. 检查 Qwen API 配置..."
QWEN_KEY=$(grep "QWEN_API_KEY" .env 2>/dev/null || grep "QWEN_API_KEY" .env.production 2>/dev/null)
if [ -n "$QWEN_KEY" ]; then
    if echo "$QWEN_KEY" | grep -q "sk-"; then
        echo -e "${GREEN}✓ Qwen API Key 已配置${NC}"
    else
        echo -e "${YELLOW}⚠ Qwen API Key 可能无效${NC}"
    fi
else
    echo -e "${RED}✗ Qwen API Key 未配置${NC}"
fi

echo ""
echo "===================================="
echo "诊断完成"
echo "===================================="
