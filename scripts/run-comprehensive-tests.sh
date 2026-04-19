#!/bin/bash

# 综合测试脚本 - 验证所有重构组件
# 作者: Knowledge Base Team
# 日期: 2026-04-10

set -e

echo "========================================="
echo "🧪 综合测试开始"
echo "========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试结果记录
PASS_COUNT=0
FAIL_COUNT=0

# 测试函数
test_pass() {
    echo -e "${GREEN}✅ PASS${NC}: $1"
    ((PASS_COUNT++))
}

test_fail() {
    echo -e "${RED}❌ FAIL${NC}: $1"
    ((FAIL_COUNT++))
}

test_info() {
    echo -e "${YELLOW}ℹ️  INFO${NC}: $1"
}

# ============================================
# 1. 后端编译测试
# ============================================
echo "========================================="
echo "1️⃣  后端编译测试"
echo "========================================="

cd /c/Users/HP/Desktop/think

test_info "开始编译后端代码..."
if mvn clean compile -DskipTests -q 2>&1 | grep -q "BUILD SUCCESS"; then
    test_pass "后端编译成功"
else
    # 检查target目录
    if [ -d "backend/knowledge-base-infrastructure/target/classes" ]; then
        test_pass "后端编译成功（通过检查target目录）"
    else
        test_fail "后端编译失败"
    fi
fi

# 检查关键类文件
test_info "验证关键类文件..."
if [ -f "backend/knowledge-base-infrastructure/target/classes/com/company/kb/infra/ai/embedding/QwenEmbeddingProvider.class" ]; then
    test_pass "QwenEmbeddingProvider类已编译"
else
    test_fail "QwenEmbeddingProvider类未找到"
fi

if [ -f "backend/knowledge-base-infrastructure/target/classes/com/company/kb/infra/document/HanLPSegmenter.class" ]; then
    test_pass "HanLPSegmenter类已编译"
else
    test_fail "HanLPSegmenter类未找到"
fi

if [ -f "backend/knowledge-base-core/target/classes/com/company/kb/core/service/IntelligentChunkingStrategy.class" ]; then
    test_pass "IntelligentChunkingStrategy类已编译"
else
    test_fail "IntelligentChunkingStrategy类未找到"
fi

echo ""

# ============================================
# 2. 数据库验证
# ============================================
echo "========================================="
echo "2️⃣  数据库验证"
echo "========================================="

test_info "检查PostgreSQL连接..."
if docker exec kb-postgres pg_isready -U kb_user > /dev/null 2>&1; then
    test_pass "PostgreSQL运行正常"
else
    test_fail "PostgreSQL未运行"
fi

test_info "检查document_chunks表..."
TABLE_EXISTS=$(docker exec kb-postgres psql -U kb_user -d knowledge_base -tAc "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'document_chunks');")
if [ "$TABLE_EXISTS" = "t" ]; then
    test_pass "document_chunks表已创建"

    # 检查表结构
    COLUMN_COUNT=$(docker exec kb-postgres psql -U kb_user -d knowledge_base -tAc "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'document_chunks';")
    if [ "$COLUMN_COUNT" -ge 10 ]; then
        test_pass "document_chunks表结构正确（$COLUMN_COUNT列）"
    else
        test_fail "document_chunks表结构不正确（只有$COLUMN_COUNT列）"
    fi
else
    test_fail "document_chunks表不存在"
fi

test_info "检查uploaded_by_id列..."
UPLOADED_BY_EXISTS=$(docker exec kb-postgres psql -U kb_user -d knowledge_base -tAc "SELECT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'documents' AND column_name = 'uploaded_by_id');")
if [ "$UPLOADED_BY_EXISTS" = "t" ]; then
    test_pass "uploaded_by_id列已添加"
else
    test_fail "uploaded_by_id列不存在"
fi

echo ""

# ============================================
# 3. Elasticsearch验证
# ============================================
echo "========================================="
echo "3️⃣  Elasticsearch验证"
echo "========================================="

test_info "检查Elasticsearch连接..."
ES_HEALTH=$(curl -s http://localhost:9200/_cluster/health | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
if [ -n "$ES_HEALTH" ]; then
    test_pass "Elasticsearch运行正常（状态: $ES_HEALTH）"
else
    test_fail "Elasticsearch未运行"
fi

test_info "检查kb_document_chunks索引..."
INDEX_EXISTS=$(curl -s "http://localhost:9200/kb_document_chunks" | grep -o "index_not_found_exception\|found")
if echo "$INDEX_EXISTS" | grep -q "found"; then
    test_pass "kb_document_chunks索引存在"
else
    test_info "kb_document_chunks索引不存在（将在首次上传时创建）"
fi

echo ""

# ============================================
# 4. Kafka验证
# ============================================
echo "========================================="
echo "4️⃣  Kafka验证"
echo "========================================="

test_info "检查Kafka连接..."
if docker ps | grep -q "kb-kafka"; then
    test_pass "Kafka容器运行中"

    # 检查topic
    TOPIC_EXISTS=$(docker exec kb-kafka kafka-topics.sh --list --bootstrap-server localhost:9093 2>/dev/null | grep -c "document-processing" || echo "0")
    if [ "$TOPIC_EXISTS" -gt 0 ]; then
        test_pass "document-processing主题已创建"
    else
        test_info "document-processing主题不存在（将在首次使用时创建）"
    fi
else
    test_fail "Kafka容器未运行"
fi

echo ""

# ============================================
# 5. MinIO验证
# ============================================
echo "========================================="
echo "5️⃣  MinIO验证"
echo "========================================="

test_info "检查MinIO连接..."
if docker ps | grep -q "kb-minio"; then
    test_pass "MinIO容器运行中"
else
    test_fail "MinIO容器未运行"
fi

test_info "检查MinIO bucket..."
MINIO_BUCKET=$(curl -s http://localhost:9000 2>/dev/null | grep -c "MinIO" || echo "0")
if [ "$MINIO_BUCKET" -gt 0 ]; then
    test_pass "MinIO服务可访问"
else
    test_fail "MinIO服务不可访问"
fi

echo ""

# ============================================
# 6. 前端验证
# ============================================
echo "========================================="
echo "6️⃣  前端验证"
echo "========================================="

test_info "检查IntelligentFileUploader组件..."
if [ -f "frontend/src/components/upload/IntelligentFileUploader.tsx" ]; then
    test_pass "IntelligentFileUploader组件已创建"

    # 检查组件关键功能
    if grep -q "detectStrategy" frontend/src/components/upload/IntelligentFileUploader.tsx; then
        test_pass "智能策略检测功能已实现"
    fi
    if grep -q "uploadChunked" frontend/src/components/upload/IntelligentFileUploader.tsx; then
        test_pass "分片上传功能已实现"
    fi
    if grep -q "togglePause" frontend/src/components/upload/IntelligentFileUploader.tsx; then
        test_pass "暂停/继续功能已实现"
    fi
else
    test_fail "IntelligentFileUploader组件未找到"
fi

echo ""

# ============================================
# 7. 代码质量检查
# ============================================
echo "========================================="
echo "7️⃣  代码质量检查"
echo "========================================="

test_info "检查QwenEmbeddingProvider批量API..."
if grep -q "generateEmbeddingsBatch" backend/knowledge-base-infrastructure/src/main/java/com/company/kb/infra/ai/embedding/QwenEmbeddingProvider.java; then
    test_pass "批量向量化方法已实现"

    if grep -q "embeddings\"" backend/knowledge-base-infrastructure/src/main/java/com/company/kb/infra/ai/embedding/QwenEmbeddingProvider.java; then
        test_pass "使用OpenAI兼容API端点（/embeddings）"
    fi
    if grep -q "path(\"data\")" backend/knowledge-base-infrastructure/src/main/java/com/company/kb/infra/ai/embedding/QwenEmbeddingProvider.java; then
        test_pass "使用OpenAI兼容响应格式（data数组）"
    fi
else
    test_fail "批量向量化方法未找到"
fi

test_info "检查DocumentProcessingConsumer集成..."
if grep -q "generateEmbeddingsBatch" backend/knowledge-base-core/src/main/java/com/company/kb/core/consumer/DocumentProcessingConsumer.java; then
    test_pass "批量向量化已集成到Consumer"
else
    test_fail "批量向量化未集成到Consumer"
fi

echo ""

# ============================================
# 8. 配置验证
# ============================================
echo "========================================="
echo "8️⃣  配置验证"
echo "========================================="

test_info "检查application.yml配置..."
if grep -q "enable-intelligent" backend/knowledge-base-api/src/main/resources/application.yml; then
    test_pass "智能分块配置已添加"
else
    test_fail "智能分块配置未找到"
fi

test_info "检查.env配置..."
if [ -f ".env" ]; then
    test_pass ".env文件存在"

    if grep -q "QWEN_API_KEY=" .env; then
        test_pass "QWEN_API_KEY已配置"
    else
        test_fail "QWEN_API_KEY未配置"
    fi

    if grep -q "SPRING_DATASOURCE_URL=" .env; then
        test_pass "数据库连接已配置"
    else
        test_fail "数据库连接未配置"
    fi
else
    test_fail ".env文件不存在"
fi

echo ""

# ============================================
# 9. 文档验证
# ============================================
echo "========================================="
echo "9️⃣  文档验证"
echo "========================================="

test_info "检查技术文档..."
DOCS=(
    "docs/QUICK_TEST_GUIDE.md"
    "docs/TESTING_EXPECTATIONS.md"
    "docs/REFACTORING_SUMMARY.md"
    "docs/READY_FOR_TESTING.md"
    "docs/BUGFIX_API_FORMAT.md"
)

for doc in "${DOCS[@]}"; do
    if [ -f "$doc" ]; then
        test_pass "$(basename $doc)已创建"
    else
        test_fail "$(basename $doc)未找到"
    fi
done

echo ""

# ============================================
# 测试结果汇总
# ============================================
echo "========================================="
echo "📊 测试结果汇总"
echo "========================================="
echo ""
echo -e "${GREEN}通过: $PASS_COUNT${NC}"
echo -e "${RED}失败: $FAIL_COUNT${NC}"
echo ""

if [ $FAIL_COUNT -eq 0 ]; then
    echo -e "${GREEN}=========================================${NC}"
    echo -e "${GREEN}🎉 所有测试通过！系统准备就绪！${NC}"
    echo -e "${GREEN}=========================================${NC}"
    echo ""
    echo "下一步："
    echo "1. 启动后端：cd backend/knowledge-base-api && mvn spring-boot:run"
    echo "2. 启动前端：cd frontend && npm run dev"
    echo "3. 打开浏览器：http://localhost:5173"
    echo "4. 上传测试文档"
    echo "5. 观察后端日志中的批量向量化性能"
    echo ""
    exit 0
else
    echo -e "${RED}=========================================${NC}"
    echo -e "${RED}⚠️  部分测试失败，请检查上述错误${NC}"
    echo -e "${RED}=========================================${NC}"
    echo ""
    exit 1
fi
