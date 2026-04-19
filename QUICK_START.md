# 🚀 Postman测试 - 5分钟快速开始

## 📦 已创建的文件

1. **Postman Collection**: `Postman_AI_Test_Collection.json` ⭐ 导入这个
2. **测试指南**: `POSTMAN_TEST_GUIDE.md` 📖 详细说明
3. **测试文件**: `test-doc.txt` 📄 上传这个文件
4. **测试脚本**: `PostmanTest_Script.js` 💡 参考脚本

## ⚡ 3步开始测试

### 步骤1：导入Collection (1分钟)

1. 打开Postman
2. 点击"Import"
3. 选择文件：`Postman_AI_Test_Collection.json`
4. 点击Import

### 步骤2：准备测试文件 (30秒)

确保文件 `test-doc.txt` 在项目根目录：
```
C:\Users\HP\Desktop\think\test-doc.txt
```

### 步骤3：执行测试 (3分钟)

在Postman中按顺序点击：

1. **1. 用户登录** → Send
2. **2. 初始化分片上传** → Send
3. **3. 上传分片** → Send
   - 点击"Select Files"选择test-doc.txt
4. **4. 完成上传（触发AI）** → Send ⚡ **这里会调用Qwen API！**
5. **5. 检查文档状态** → Send
   - 如果显示"PROCESSING"，等5秒再点一次
   - 直到显示"INDEXED"
6. **6. 搜索验证** → Send

## 🔍 验证AI调用成功

### ✅ 成功标志

**Postman Console**应该显示：
```
✅ 完成上传
✅ 使用MinIO对象存储
🤖 正在触发AI处理...
💰 这将消耗Qwen API tokens！
✅ AI处理完成
💰 Qwen API已调用
```

**Qwen控制台**（https://bailian.console.aliyun.com/）：
- 调用次数：+1
- Token消耗：+100~200
- 费用：+¥0.005~0.01

**后端日志**：
```bash
tail -f backend/knowledge-base-api/logs/knowledge-base-api.log | grep -i "qwen"
```

应该看到：
```
Calling Qwen API for embedding
Generated embedding for chunk 1/1
Document processing completed
```

## ❌ 如果失败

### 问题：登录失败
```bash
# 检查后端是否运行
curl http://localhost:8080/api/actuator/health
```

### 问题：MinIO错误
```bash
# 检查MinIO
docker-compose ps minio
```

### 问题：文档一直是PROCESSING
```bash
# 检查Kafka
docker-compose ps kafka

# 查看后端日志
tail -50 backend/knowledge-base-api/logs/knowledge-base-api.log
```

## 💰 费用说明

- **每次测试**：约¥0.005-0.01元
- **文件大小**：788字节
- **Token消耗**：约100-200 tokens
- **真实API**：是的，会真实调用Qwen API

## 📊 测试对比

| 测试类型 | 费用 | AI调用 | 用途 |
|---------|------|--------|------|
| 单元测试 | ¥0 | ❌ Mock | 验证代码逻辑 |
| **Postman测试** | **¥0.005** | **✅ 真实** | **验证完整流程** |

---

**准备好了吗？开始测试吧！** 🚀

测试完成后告诉我结果：
- ✅ 成功：AI调用了，token消耗了
- ❌ 失败：告诉我哪个步骤出错了
