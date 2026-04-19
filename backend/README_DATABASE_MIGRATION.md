# 数据库迁移执行指南

## 概述

本指南提供了企业级知识库系统数据库迁移的完整步骤和注意事项。

## 迁移文件列表

### 核心架构迁移
- `V8__create_multi_tenant_tables.sql` - 多租户核心架构表
- `V9__create_tenant_isolation.sql` - 租户隔离和数据迁移
- `V10__create_performance_optimization.sql` - 性能优化和监控
- `V11__create_cache_tables.sql` - Redis 缓存集成

### 现有迁移文件（已存在）
- `V1__create_document_chunks_table.sql` - 文档块表
- `V2__add_uploaded_by_id_column.sql` - 上传者ID字段
- 其他服务特定迁移

## 迁移执行步骤

### 1. 执行前准备

#### 1.1 数据备份
```bash
# 备份当前数据库
pg_dump -Fc -f backup_before_migration_$(date +%Y%m%d).dump knowledge_base

# 或使用 SQL 备份
pg_dump -f backup_before_migration.sql knowledge_base
```

#### 1.2 健康检查
```bash
# 检查数据库连接
psql -U kb_user -d knowledge_base -c "SELECT version();"

# 检查表数量
psql -U kb_user -d knowledge_base -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';"

# 检查数据库大小
psql -U kb_user -d knowledge_base -c "SELECT pg_size_pretty(pg_database_size('knowledge_base'));"
```

### 2. 迁移执行

#### 2.1 使用 Flyway 执行（推荐）
```bash
# 从项目根目录执行
cd backend

# 检查 Flyway 状态
mvn flyway:info

# 执行迁移
mvn flyway:migrate

# 验证迁移
mvn flyway:validate
```

#### 2.2 手动执行（可选）
```bash
# 按顺序执行迁移文件
psql -U kb_user -d knowledge_base -f migrations/V8__create_multi_tenant_tables.sql
psql -U kb_user -d knowledge_base -f migrations/V9__create_tenant_isolation.sql
psql -U kb_user -d knowledge_base -f migrations/V10__create_performance_optimization.sql
psql -U kb_user -d knowledge_base -f migrations/V11__create_cache_tables.sql
```

### 3. 执行后验证

#### 3.1 表结构验证
```sql
-- 检查新表是否创建成功
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
AND table_name IN (
    'tenants',
    'users_v2',
    'documents_v2',
    'document_chunks_v2',
    'qa_history_v2',
    'code_reviews_v2',
    'audit_logs_partitioned',
    'document_analytics_mv'
)
ORDER BY table_name;

-- 检查索引是否创建
SELECT
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
AND tablename IN ('users_v2', 'documents_v2', 'document_chunks_v2')
ORDER BY tablename, indexname;
```

#### 3.2 数据完整性验证
```sql
-- 验证租户创建
SELECT COUNT(*) as tenant_count FROM tenants;

-- 验证用户迁移
SELECT
    (SELECT COUNT(*) FROM users) as old_users,
    (SELECT COUNT(*) FROM users_v2) as new_users;

-- 验证文档迁移
SELECT
    (SELECT COUNT(*) FROM documents) as old_documents,
    (SELECT COUNT(*) FROM documents_v2) as new_documents;

-- 验证权限数据
SELECT
    (SELECT COUNT(*) FROM user_roles) as old_user_roles,
    (SELECT COUNT(*) FROM user_roles_v2) as new_user_roles;
```

#### 3.3 性能验证
```sql
-- 检查物化视图
SELECT relname, pg_size_pretty(pg_total_relation_size(oid)) as size
FROM pg_class
WHERE relname IN ('document_analytics_mv', 'user_activity_mv');

-- 检查分区表
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables
WHERE tablename LIKE '%_2024_%'
ORDER BY tablename;

-- 分析表统计信息
SELECT analyze_table_statistics();
```

## 回滚计划

### 1. 回滚准备
```bash
# 如果迁移失败，从备份恢复
pg_restore -d knowledge_base backup_before_migration_$(date +%Y%m%d).dump

# 或使用 SQL 备份
psql -d knowledge_base -f backup_before_migration.sql
```

### 2. 部分回滚
```sql
-- 删除新创建的表（谨慎操作）
DROP TABLE IF EXISTS audit_logs_partitioned CASCADE;
DROP TABLE IF EXISTS qa_history_partitioned CASCADE;
DROP MATERIALIZED VIEW IF EXISTS document_analytics_mv;
DROP MATERIALIZED VIEW IF EXISTS user_activity_mv;

-- 删除新版本表（如果需要）
DROP TABLE IF EXISTS users_v2 CASCADE;
DROP TABLE IF EXISTS documents_v2 CASCADE;
DROP TABLE IF EXISTS document_chunks_v2 CASCADE;
```

## 性能优化建议

### 1. PostgreSQL 配置调整
```ini
# postgresql.conf
shared_buffers = 256MB
effective_cache_size = 1GB
work_mem = 16MB
maintenance_work_mem = 128MB

# 连接配置
max_connections = 100
shared_preload_libraries = 'pg_stat_statements'

# 日志配置
log_min_duration_statement = 1000
log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d,app=%a,client=%h '

# 自动清理
autovacuum = on
autovacuum_max_workers = 4
```

### 2. 连接池配置
```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### 3. Redis 缓存配置
```yaml
# application.yml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 5000
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

## 监控与维护

### 1. 日常监控查询
```sql
-- 慢查询监控
SELECT * FROM slow_queries LIMIT 20;

-- 表大小监控
SELECT * FROM table_sizes LIMIT 10;

-- 索引使用统计
SELECT * FROM index_usage_stats WHERE tablename = 'documents_v2';

-- 缓存命中率
SELECT * FROM cache_hit_ratio WHERE schemaname = 'public';

-- 租户统计
SELECT * FROM tenant_summary ORDER BY created_at DESC;
```

### 2. 定期维护任务
```sql
-- 更新统计信息（每天）
SELECT analyze_table_statistics();

-- 清理旧审计日志（每月）
SELECT cleanup_old_audit_logs(12);

-- 刷新物化视图（每15分钟）
SELECT refresh_analytics_views();

-- 收集性能指标（每小时）
SELECT collect_performance_metrics();
```

### 3. 性能优化建议
```sql
-- 识别未使用的索引
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0
AND schemaname = 'public'
AND indexname NOT LIKE '%_pkey';

-- 检查表膨胀
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as total_size,
    pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) as table_size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

## 应用程序更改

### 1. Java 实体更新
```java
// 用户实体示例
@Entity
@Table(name = "users_v2")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, unique = true)
    private String username;

    // ... 其他字段
}
```

### 2. 仓库层更新
```java
// 租户感知查询
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    @Query("SELECT d FROM Document d WHERE d.tenantId = :tenantId AND d.deletedAt IS NULL")
    List<Document> findByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT d FROM Document d WHERE d.tenantId = :tenantId AND d.status = :status")
    List<Document> findByTenantIdAndStatus(@Param("tenantId") Long tenantId,
                                           @Param("status") DocumentStatus status);
}
```

### 3. 服务层更新
```java
// 租户上下文管理
@Service
public class TenantContextService {
    private static final ThreadLocal<Long> currentTenantId = new ThreadLocal<>();

    public void setCurrentTenantId(Long tenantId) {
        currentTenantId.set(tenantId);
    }

    public Long getCurrentTenantId() {
        return currentTenantId.get();
    }

    public void clear() {
        currentTenantId.remove();
    }
}

// 拦截器设置租户上下文
@Component
public class TenantInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantSlug = request.getHeader("X-Tenant-Slug");
        Long tenantId = tenantService.getTenantIdBySlug(tenantSlug);
        tenantContextService.setCurrentTenantId(tenantId);
        return true;
    }
}
```

### 4. Redis 缓存集成
```java
// 缓存配置
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("user_permissions", config.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("document_metadata", config.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("search_results", config.entryTtl(Duration.ofMinutes(2)));

        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }
}

// 缓存使用
@Cacheable(value = "user_permissions", key = "#userId")
public List<Permission> getUserPermissions(Long userId) {
    return permissionRepository.findByUserId(userId);
}

@CacheEvict(value = "user_permissions", key = "#userId")
public void clearUserPermissionsCache(Long userId) {
    // 缓存自动失效
}
```

## 故障排除

### 1. 常见问题

#### 问题：迁移执行失败
```bash
# 检查 Flyway 状态
mvn flyway:info

# 查看错误详情
mvn flyway:migrate -X

# 手动执行失败脚本
psql -U kb_user -d knowledge_base -f migrations/failed_script.sql
```

#### 问题：性能下降
```sql
-- 检查查询计划
EXPLAIN ANALYZE SELECT * FROM documents_v2 WHERE tenant_id = 1;

-- 更新统计信息
ANALYZE documents_v2;

-- 检查索引使用
SELECT * FROM pg_stat_user_indexes WHERE tablename = 'documents_v2';
```

#### 问题：存储空间不足
```sql
-- 检查表大小
SELECT * FROM table_sizes;

-- 清理旧数据
SELECT cleanup_old_audit_logs(6);

-- 清理未使用空间
VACUUM FULL documents_v2;
```

### 2. 紧急恢复
```bash
# 停止应用程序
systemctl stop knowledge-base-api

# 恢复数据库
pg_restore -d knowledge_base backup_before_migration.dump

# 重启应用程序
systemctl start knowledge-base-api
```

## 升级路径

### 1. 下一步迁移计划
- V12: Elasticsearch 同步机制
- V13: 高级分析功能
- V14: 国际化支持
- V15: API 限流和配额

### 2. 持续改进
- 监控数据库性能指标
- 收集用户反馈
- 定期审查索引使用情况
- 优化慢查询

## 支持和帮助

### 1. 联系方式
- 数据库管理员: dba@company.com
- 技术支持: support@company.com
- 紧急联系人: oncall@company.com

### 2. 相关文档
- [数据库设计文档](DATABASE_DESIGN.md)
- [性能优化指南](PERFORMANCE_GUIDE.md)
- [运维手册](OPERATIONS_GUIDE.md)

---

**重要提示**:
1. 始终在生产环境执行前先在测试环境验证
2. 确保有完整的备份和回滚计划
3. 监控迁移过程中的系统资源使用
4. 准备好应急响应团队
5. 通知所有相关人员迁移计划和时间表
