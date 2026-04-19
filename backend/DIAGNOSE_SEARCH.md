# RAG 检索问题诊断脚本

## 1. 检查 Elasticsearch 索引状态

```bash
# 检查索引是否存在
curl -X GET "localhost:9200/kb_document_chunks?pretty"

# 检查文档数量
curl -X GET "localhost:9200/kb_document_chunks/_count?pretty"

# 查看索引映射
curl -X GET "localhost:9200/kb_document_chunks/_mapping?pretty"
```

## 2. 搜索包含"版权"的文档

```bash
curl -X GET "localhost:9200/kb_document_chunks/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "query": {
    "match": {
      "content": "版权"
    }
  },
  "size": 10,
  "_source": ["chunkId", "documentId", "title", "content", "uploadedBy"]
}'
```

## 3. 检查特定文档是否在索引中

```bash
# 假设文档 ID 是 36（从之前的日志看到）
curl -X GET "localhost:9200/kb_document_chunks/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "query": {
    "term": {
      "documentId": 36
    }
  },
  "size": 10
}'
```

## 4. 测试 KNN 向量搜索

```bash
curl -X GET "localhost:9200/kb_document_chunks/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "knn": {
    "field": "content_vector",
    "query_vector": [0.1, 0.2, ...],  # 需要实际的1024维向量
    "k": 5,
    "num_candidates": 100
  }
}'
```

## 5. 测试 BM25 关键词搜索

```bash
curl -X GET "localhost:9200/kb_document_chunks/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "content": "版权进度"
          }
        }
      ]
    }
  },
  "size": 5
}'
```

## 6. 检查用户可访问的文档

在浏览器 Console 执行：

```javascript
// 获取 token
const token = localStorage.getItem('token');

// 调用 debug 接口
fetch('http://localhost:8080/api/questions/debug/accessible-docs', {
  headers: {
    'Authorization': 'Bearer ' + token
  }
})
  .then(r => r.json())
  .then(data => {
    console.log('用户可访问的文档ID:', data.data);

    // 检查这些文档是否在 Elasticsearch 中
    data.data.forEach(docId => {
      fetch(`http://localhost:9200/kb_document_chunks/_search?pretty`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          query: { term: { documentId: docId } },
          size: 1
        })
      })
      .then(r => r.json())
      .then(result => {
        console.log(`文档 ${docId} 的块数量:`, result.hits.total.value);
      });
    });
  });
```

## 常见问题排查

### 问题 1: 索引中没有数据
**症状**: `_count` 返回 0
**原因**: 文档上传后没有正确处理
**解决**: 检查 Kafka 消费者日志

### 问题 2: 向量维度不匹配
**症状**: KNN 搜索报错 "illegal_argument_exception"
**原因**: Elasticsearch 配置的维度与向量实际维度不符
**解决**: 确保配置一致

### 问题 3: 权限过滤导致无结果
**症状**: 直接搜索 ES 有结果，但通过 API 查询无结果
**原因**: 用户没有文档访问权限
**解决**: 检查权限配置

### 问题 4: 文档分块后关键词丢失
**症状**: 上传的文档有"版权"，但搜索"版权进度"无结果
**原因**: 分块太大或分块位置不当
**解决**: 调整分块策略
