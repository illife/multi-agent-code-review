-- ============================================================================
-- Multi-Tenant Enterprise Knowledge Base - Performance Optimization
-- Version: V10
-- Description: Advanced performance optimization with caching, partitioning,
--              and monitoring for high-scale deployments
-- ============================================================================

-- ============================================================================
-- EXTENSIONS: Enable required PostgreSQL extensions
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
CREATE EXTENSION IF NOT EXISTS pg_buffercache;
CREATE EXTENSION IF NOT EXISTS pg_trgm;     -- For trigram matching
CREATE EXTENSION IF NOT EXISTS btree_gin;   -- For B-tree indexes in GIN
CREATE EXTENSION IF NOT EXISTS pg_prewarm;  -- For cache warming

-- ============================================================================
-- PARTITIONING: Implement partitioning for high-volume tables
-- ============================================================================

-- ============================================================================
-- Partition audit_logs by month for better performance and archival
-- ============================================================================
-- First, create the partitioned table if it doesn't exist
DROP TABLE IF EXISTS audit_logs_partitioned CASCADE;

CREATE TABLE audit_logs_partitioned (
    id BIGSERIAL,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    user_id BIGINT REFERENCES users_v2(id),
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id BIGINT,
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    user_agent TEXT,
    request_id VARCHAR(100),
    session_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Create partitions for current and future months
CREATE TABLE audit_logs_2024_01 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE audit_logs_2024_02 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

CREATE TABLE audit_logs_2024_03 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');

CREATE TABLE audit_logs_2024_04 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-04-01') TO ('2024-05-01');

CREATE TABLE audit_logs_2024_05 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-05-01') TO ('2024-06-01');

CREATE TABLE audit_logs_2024_06 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-06-01') TO ('2024-07-01');

CREATE TABLE audit_logs_2024_07 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-07-01') TO ('2024-08-01');

CREATE TABLE audit_logs_2024_08 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-08-01') TO ('2024-09-01');

CREATE TABLE audit_logs_2024_09 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-09-01') TO ('2024-10-01');

CREATE TABLE audit_logs_2024_10 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-10-01') TO ('2024-11-01');

CREATE TABLE audit_logs_2024_11 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE TABLE audit_logs_2024_12 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-12-01') TO ('2025-01-01');

CREATE TABLE audit_logs_2025_01 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE audit_logs_2025_02 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

CREATE TABLE audit_logs_2025_03 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');

CREATE TABLE audit_logs_2025_04 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2025-04-01') TO ('2025-05-01');

-- Create a default partition for future data
CREATE TABLE audit_logs_default PARTITION OF audit_logs_partitioned
    DEFAULT;

-- Create indexes on partitioned table
CREATE INDEX idx_audit_partitioned_tenant ON audit_logs_partitioned(tenant_id);
CREATE INDEX idx_audit_partitioned_user ON audit_logs_partitioned(user_id);
CREATE INDEX idx_audit_partitioned_action ON audit_logs_partitioned(action);
CREATE INDEX idx_audit_partitioned_resource ON audit_logs_partitioned(resource_type, resource_id);
CREATE INDEX idx_audit_partitioned_created ON audit_logs_partitioned(created_at DESC);

COMMENT ON TABLE audit_logs_partitioned IS 'Partitioned audit log table by month for performance';

-- ============================================================================
-- Partition qa_history by month for high-volume systems
-- ============================================================================
DROP TABLE IF NOT EXISTS qa_history_partitioned CASCADE;

CREATE TABLE qa_history_partitioned (
    id BIGSERIAL,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    user_id BIGINT NOT NULL REFERENCES users_v2(id),
    session_id VARCHAR(100),
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    search_type VARCHAR(20) CHECK (search_type IN ('BM25', 'KNN', 'HYBRID')),
    sources JSONB,
    context_used JSONB,
    chunks_accessed JSONB,
    model_used VARCHAR(50),
    tokens_consumed INTEGER,
    response_time_ms INTEGER,
    feedback INTEGER CHECK (feedback >= 1 AND feedback <= 5),
    feedback_comment TEXT,
    is_bookmarked BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Create similar partitions for QA history (same pattern as audit_logs)
CREATE TABLE qa_history_2024_01 PARTITION OF qa_history_partitioned
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE qa_history_2024_02 PARTITION OF qa_history_partitioned
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

CREATE TABLE qa_history_2024_03 PARTITION OF qa_history_partitioned
    FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');

CREATE TABLE qa_history_2024_04 PARTITION OF qa_history_partitioned
    FOR VALUES FROM ('2024-04-01') TO ('2024-05-01');

CREATE TABLE qa_history_2024_05 PARTITION OF qa_history_partitioned
    FOR VALUES FROM ('2024-05-01') TO ('2024-06-01');

CREATE TABLE qa_history_2024_06 PARTITION OF qa_history_partitioned
    FOR VALUES FROM ('2024-06-01') TO ('2024-07-01');

CREATE TABLE qa_history_2024_07 PARTITION OF qa_history_partitioned
    FOR VALUES FROM ('2024-07-01') TO ('2024-08-01');

CREATE TABLE qa_history_2024_08 PARTITION OF qa_history_partitioned
    FOR VALUES FROM ('2024-08-01') TO ('2024-09-01');

CREATE TABLE qa_history_2024_09 PARTITION OF qa_history_partitioned
    FOR VALUES FROM ('2024-09-01') TO ('2024-10-01');

CREATE TABLE qa_history_2024_10 PARTITION OF qa_history_partitioned
    FOR VALUES FROM ('2024-10-01') TO ('2024-11-01');

CREATE TABLE qa_history_2024_11 PARTITION OF qa_history_partitioned
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE TABLE qa_history_2024_12 PARTITION OF qa_history_partitioned
    FOR VALUES FROM ('2024-12-01') TO ('2025-01-01');

CREATE TABLE qa_history_2025_01 PARTITION OF qa_history_partitioned
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE qa_history_2025_02 PARTITION OF qa_history_partitioned
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

CREATE TABLE qa_history_2025_03 PARTITION OF qa_history_partitioned
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');

CREATE TABLE qa_history_2025_04 PARTITION OF qa_history_partitioned
    FOR VALUES FROM ('2025-04-01') TO ('2025-05-01');

CREATE TABLE qa_history_default PARTITION OF qa_history_partitioned
    DEFAULT;

-- Create indexes on partitioned QA history
CREATE INDEX idx_qa_partitioned_tenant ON qa_history_partitioned(tenant_id);
CREATE INDEX idx_qa_partitioned_user ON qa_history_partitioned(user_id);
CREATE INDEX idx_qa_partitioned_session ON qa_history_partitioned(session_id);
CREATE INDEX idx_qa_partitioned_feedback ON qa_history_partitioned(feedback);
CREATE INDEX idx_qa_partitioned_bookmarked ON qa_history_partitioned(is_bookmarked);
CREATE INDEX idx_qa_partitioned_created ON qa_history_partitioned(created_at DESC);

COMMENT ON TABLE qa_history_partitioned IS 'Partitioned QA history table by month for performance';

-- ============================================================================
-- ADVANCED INDEXES: Create specialized indexes for performance
-- ============================================================================

-- Brin indexes for time-series data (very efficient for large tables)
CREATE INDEX idx_documents_created_brin ON documents_v2 USING BRIN(created_at);
CREATE INDEX idx_chunks_created_brin ON document_chunks_v2 USING BRIN(created_at);
CREATE INDEX idx_qa_created_brin ON qa_history_v2 USING BRIN(created_at);

-- Hash indexes for equality lookups (faster than B-tree for exact matches)
CREATE INDEX idx_users_email_hash ON users_v2 USING HASH(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_tenants_slug_hash ON tenants USING HASH(slug);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens USING HASH(token);

-- GIN indexes for array and JSONB operations
CREATE INDEX idx_documents_tags_gin ON documents_v2 USING GIN(tags);
CREATE INDEX idx_chunks_metadata_gin ON document_chunks_v2 USING GIN(metadata);
CREATE INDEX idx_users_profile_gin ON users_v2 USING GIN(profile_data);

-- Trigram indexes for fuzzy text search
CREATE INDEX idx_documents_title_trgm ON documents_v2 USING GIN(title gin_trgm_ops);
CREATE INDEX idx_documents_filename_trgm ON documents_v2 USING GIN(file_name gin_trgm_ops);
CREATE INDEX idx_users_fullname_trgm ON users_v2 USING GIN(full_name gin_trgm_ops);

-- Covering indexes (include columns) for index-only scans
CREATE INDEX idx_documents_list_covering_v2 ON documents_v2(tenant_id, created_at DESC)
    INCLUDE (title, file_type, status, visibility, uploaded_by)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_chunks_search_covering ON document_chunks_v2(document_id, position)
    INCLUDE (content, chunk_type, token_count)
    WHERE deleted_at IS NULL AND embedding_vector IS NOT NULL;

-- Partial indexes for common filtered queries
CREATE INDEX idx_documents_public_only ON documents_v2(tenant_id, is_public, created_at DESC)
    WHERE is_public = TRUE AND deleted_at IS NULL;

CREATE INDEX idx_documents_indexed_only ON documents_v2(tenant_id, status, indexed_at DESC)
    WHERE status = 'INDEXED' AND deleted_at IS NULL;

CREATE INDEX idx_users_active_only ON users_v2(tenant_id, is_active, email)
    WHERE is_active = TRUE AND deleted_at IS NULL;

CREATE INDEX idx_qa_with_feedback ON qa_history_v2(user_id, feedback, created_at DESC)
    WHERE feedback IS NOT NULL AND deleted_at IS NULL;

-- ============================================================================
-- FUNCTIONAL INDEXES: For computed values
-- ============================================================================

-- Index on lowercased email for case-insensitive searches
CREATE INDEX idx_users_email_lower ON users_v2(LOWER(email))
    WHERE deleted_at IS NULL;

-- Index on extracted year-month for date-based queries
CREATE INDEX idx_documents_year_month ON documents_v2(
    tenant_id,
    DATE_TRUNC('month', created_at)
) WHERE deleted_at IS NULL;

-- Index on file size categories
CREATE INDEX idx_documents_size_category ON documents_v2(
    tenant_id,
    CASE
        WHEN file_size < 1024 THEN 'SMALL'
        WHEN file_size < 1024*1024 THEN 'MEDIUM'
        WHEN file_size < 10*1024*1024 THEN 'LARGE'
        ELSE 'XLARGE'
    END
) WHERE deleted_at IS NULL;

-- ============================================================================
-- PERFORMANCE FUNCTIONS: Utility functions for performance optimization
-- ============================================================================

-- Function to update document access count efficiently
CREATE OR REPLACE FUNCTION increment_document_access(p_document_id BIGINT)
RETURNS void AS $$
BEGIN
    UPDATE documents_v2
    SET
        access_count = access_count + 1,
        last_accessed_at = NOW()
    WHERE id = p_document_id;
END;
$$ LANGUAGE plpgsql;

-- Function to batch update document access counts
CREATE OR REPLACE FUNCTION batch_increment_document_access(p_document_ids BIGINT[])
RETURNS void AS $$
BEGIN
    UPDATE documents_v2
    SET
        access_count = access_count + 1,
        last_accessed_at = NOW()
    WHERE id = ANY(p_document_ids);
END;
$$ LANGUAGE plpgsql;

-- Function to get热门 documents efficiently
CREATE OR REPLACE FUNCTION get_popular_documents(
    p_tenant_id BIGINT,
    p_limit INTEGER DEFAULT 10,
    p_days INTEGER DEFAULT 30
)
RETURNS TABLE (
    document_id BIGINT,
    title VARCHAR(500),
    file_type VARCHAR(50),
    access_count INTEGER,
    uploaded_by BIGINT,
    created_at TIMESTAMPTZ
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        d.id,
        d.title,
        d.file_type,
        d.access_count,
        d.uploaded_by,
        d.created_at
    FROM documents_v2 d
    WHERE d.tenant_id = p_tenant_id
    AND d.deleted_at IS NULL
    AND d.created_at > NOW() - (p_days || ' days')::INTERVAL
    ORDER BY d.access_count DESC, d.created_at DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql STABLE;

-- Function to cleanup old audit logs (call periodically)
CREATE OR REPLACE FUNCTION cleanup_old_audit_logs(p_months_to_keep INTEGER DEFAULT 12)
RETURNS BIGINT AS $$
DECLARE
    v_deleted_count BIGINT;
BEGIN
    -- Archive to separate table instead of deleting
    CREATE TABLE IF NOT EXISTS audit_logs_archive AS
    SELECT * FROM audit_logs WHERE FALSE;

    -- Move old records to archive
    WITH archived_logs AS (
        INSERT INTO audit_logs_archive
        SELECT * FROM audit_logs
        WHERE created_at < NOW() - (p_months_to_keep || ' months')::INTERVAL
        RETURNING *
    )
    SELECT COUNT(*) INTO v_deleted_count FROM archived_logs;

    -- Delete from main table
    DELETE FROM audit_logs
    WHERE created_at < NOW() - (p_months_to_keep || ' months')::INTERVAL;

    RETURN v_deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to analyze table statistics (run after major data changes)
CREATE OR REPLACE FUNCTION analyze_table_statistics()
RETURNS void AS $$
BEGIN
    ANALYZE tenants;
    ANALYZE users_v2;
    ANALYZE roles_v2;
    ANALYZE documents_v2;
    ANALYZE document_chunks_v2;
    ANALYZE qa_history_v2;
    ANALYZE search_history_v2;
    ANALYZE code_reviews_v2;
    ANALYZE code_issues_v2;
    ANALYZE document_permissions_v2;
    ANALYZE user_roles_v2;
    ANALYZE audit_logs;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- MONITORING VIEWS: Views for performance monitoring
-- ============================================================================

-- View for monitoring slow queries (requires pg_stat_statements)
CREATE OR REPLACE VIEW slow_queries AS
SELECT
    query,
    calls,
    total_exec_time / 1000 / 60 as total_exec_time_minutes,
    mean_exec_time as avg_exec_time_ms,
    stddev_exec_time as stddev_exec_time_ms,
    max_exec_time as max_exec_time_ms,
    total_exec_time / calls as avg_time_per_call_ms
FROM pg_stat_statements
WHERE calls > 100
ORDER BY mean_exec_time DESC
LIMIT 20;

COMMENT ON VIEW slow_queries IS 'View showing top 20 slowest queries by average execution time';

-- View for table size monitoring
CREATE OR REPLACE VIEW table_sizes AS
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
    pg_total_relation_size(schemaname||'.'||tablename) AS size_bytes
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

COMMENT ON VIEW table_sizes IS 'View showing table sizes for monitoring';

-- View for index usage statistics
CREATE OR REPLACE VIEW index_usage_stats AS
SELECT
    schemaname,
    tablename,
    indexname,
    idx_tup_read,
    idx_tup_fetch,
    idx_scan,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;

COMMENT ON VIEW index_usage_stats IS 'View showing index usage statistics';

-- View for cache hit ratios
CREATE OR REPLACE VIEW cache_hit_ratio AS
SELECT
    schemaname,
    tablename,
    heap_blks_read,
    heap_blks_hit,
    round(
        (heap_blks_hit::numeric / NULLIF(heap_blks_hit + heap_blks_read, 0) * 100)::numeric,
        2
    ) as cache_hit_ratio_percent
FROM pg_statio_user_tables
WHERE schemaname = 'public'
ORDER BY (heap_blks_hit::numeric / NULLIF(heap_blks_hit + heap_blks_read, 0)) DESC;

COMMENT ON VIEW cache_hit_ratio IS 'View showing buffer cache hit ratios for tables';

-- ============================================================================
-- PERFORMANCE TRIGGERS: Triggers for performance optimization
-- ============================================================================

-- Trigger to update document analytics on document changes
CREATE OR REPLACE FUNCTION update_document_analytics_trigger()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        -- Refresh materialized view asynchronously
        -- (In production, use NOTIFY/LISTEN or job queue)
        REFRESH MATERIALIZED VIEW CONCURRENTLY document_analytics_mv;
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        REFRESH MATERIALIZED VIEW CONCURRENTLY document_analytics_mv;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        REFRESH MATERIALIZED VIEW CONCURRENTLY document_analytics_mv;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for document analytics (disabled by default - enable if needed)
-- DROP TRIGGER IF EXISTS document_analytics_update_trigger ON documents_v2;
-- CREATE TRIGGER document_analytics_update_trigger
--     AFTER INSERT OR UPDATE OR DELETE ON documents_v2
--     FOR EACH STATEMENT EXECUTE FUNCTION update_document_analytics_trigger();

-- ============================================================================
-- AUTOMATED MAINTENANCE: Functions for automated maintenance
-- ============================================================================

-- Function to reindex fragmented indexes
CREATE OR REPLACE FUNCTION reindex_fragmented_indexes(p_bloat_threshold DECIMAL DEFAULT 0.3)
RETURNS TABLE (
    tablename TEXT,
    indexname TEXT,
    bloat_ratio DECIMAL,
    reindexed BOOLEAN
) AS $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT
            schemaname||'.'||tablename as tablename,
            indexname,
            pg_relation_size(indexrelid) as index_size,
            pg_stat_get_dead_tuples(c.oid) as dead_tuples
        FROM pg_stat_user_indexes s
        JOIN pg_class c ON c.oid = s.indexrelid
        JOIN pg_class t ON t.oid = s.relid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE schemaname = 'public'
    LOOP
        -- Reindex concurrently to avoid locks
        BEGIN
            EXECUTE format('REINDEX INDEX CONCURRENTLY %I', r.indexname);
            RETURN NEXT SELECT r.tablename::TEXT, r.indexname, 0::DECIMAL, TRUE::BOOLEAN;
        EXCEPTION WHEN OTHERS THEN
            RETURN NEXT SELECT r.tablename::TEXT, r.indexname, 0::DECIMAL, FALSE::BOOLEAN;
        END;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Function to update table statistics based on data changes
CREATE OR REPLACE FUNCTION auto_analyze_tables()
RETURNS void AS $$
BEGIN
    -- Analyze tables with high modification rates
    ANALYZE documents_v2;
    ANALYZE document_chunks_v2;
    ANALYZE qa_history_v2;
    ANALYZE audit_logs;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- PERFORMANCE CONFIGURATION: Recommended PostgreSQL settings
-- ============================================================================

-- Note: These settings should be configured in postgresql.conf or via ALTER SYSTEM
-- Include these in your PostgreSQL configuration tuning:

/*
-- Memory Configuration
shared_buffers = 256MB              -- 25% of system RAM
effective_cache_size = 1GB          -- 50-75% of system RAM
work_mem = 16MB                     -- Per-operation memory
maintenance_work_mem = 128MB        -- For maintenance operations

-- Query Optimization
random_page_cost = 1.1              -- For SSD storage
effective_io_concurrency = 200      -- For SSD storage
max_worker_processes = 8
max_parallel_workers_per_gather = 4
max_parallel_workers = 8
max_parallel_maintenance_workers = 4

-- Connection Settings
max_connections = 100
shared_preload_libraries = 'pg_stat_statements'

-- Logging
log_min_duration_statement = 1000   -- Log queries taking >1s
log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d,app=%a,client=%h '
log_checkpoints = on
log_connections = on
log_disconnections = on
log_duration = off
log_lock_waits = on

-- Autovacuum Tuning
autovacuum = on
autovacuum_max_workers = 4
autovacuum_naptime = 30s
autovacuum_vacuum_threshold = 50
autovacuum_analyze_threshold = 50
autovacuum_vacuum_scale_factor = 0.2
autovacuum_analyze_scale_factor = 0.1
autovacuum_vacuum_cost_delay = 10ms
autovacuum_vacuum_cost_limit = 200

-- WAL Configuration
wal_buffers = 16MB
min_wal_size = 1GB
max_wal_size = 4GB
wal_compression = on
checkpoint_completion_target = 0.9
*/

-- ============================================================================
-- PERFORMANCE MONITORING: Setup performance monitoring
-- ============================================================================

-- Create a performance metrics table
CREATE TABLE IF NOT EXISTS performance_metrics (
    id BIGSERIAL PRIMARY KEY,
    metric_name VARCHAR(100) NOT NULL,
    metric_value NUMERIC NOT NULL,
    metric_unit VARCHAR(20),
    tags JSONB,
    collected_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_perf_metrics_name ON performance_metrics(metric_name, collected_at DESC);
CREATE INDEX idx_perf_metrics_collected ON performance_metrics(collected_at DESC);

COMMENT ON TABLE performance_metrics IS 'Performance metrics storage for monitoring';

-- Function to collect performance metrics
CREATE OR REPLACE FUNCTION collect_performance_metrics()
RETURNS void AS $$
BEGIN
    -- Collect database size
    INSERT INTO performance_metrics (metric_name, metric_value, metric_unit, tags)
    SELECT
        'database_size',
        pg_database_size('knowledge_base')::NUMERIC,
        'bytes',
        '{"database": "knowledge_base"}'::JSONB;

    -- Collect active connections
    INSERT INTO performance_metrics (metric_name, metric_value, metric_unit, tags)
    SELECT
        'active_connections',
        count(*)::NUMERIC,
        'count',
        '{}'::JSONB
    FROM pg_stat_activity
    WHERE state = 'active';

    -- Collect cache hit ratio
    INSERT INTO performance_metrics (metric_name, metric_value, metric_unit, tags)
    SELECT
        'cache_hit_ratio',
        round(
            (sum(heap_blks_hit)::numeric / NULLIF(sum(heap_blks_hit + heap_blks_read), 0) * 100)::numeric,
            2
        ),
        'percent',
        '{}'::JSONB
    FROM pg_statio_user_tables;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION collect_performance_metrics IS 'Collect database performance metrics for monitoring';

-- ============================================================================
-- PERFORMANCE OPTIMIZATION NOTES
-- ============================================================================
/*
PARTITIONING BENEFITS:
- Improved query performance on time-series data
- Easier data archival and cleanup
- Better parallel query execution
- Reduced index maintenance overhead

INDEX STRATEGY:
- Use BRIN indexes for time-series data (small footprint)
- Use GIN indexes for JSONB and array columns
- Use partial indexes to reduce index size
- Use covering indexes to enable index-only scans
- Use functional indexes for computed values

MONITORING RECOMMENDATIONS:
- Monitor slow queries using pg_stat_statements
- Track index usage and remove unused indexes
- Monitor cache hit ratios (target >95%)
- Track table bloat and reindex when needed
- Monitor connection pool utilization

MAINTENANCE TASKS:
- Weekly: Analyze table statistics
- Monthly: Reindex fragmented indexes
- Quarterly: Review and optimize indexes
- Annually: Archive old audit logs

VACUUM TUNING:
- For high-write tables, reduce autovacuum_vacuum_scale_factor to 0.1
- For large tables, increase autovacuum_vacuum_cost_limit to 1000
- Monitor autovacuum activity and adjust parameters
- Consider manual VACUUM ANALYZE after major data changes

CONNECTION POOLING:
- Use PgBouncer with transaction pooling mode
- Set pool size based on application concurrency
- Monitor pool utilization and adjust accordingly
- Use prepared statements carefully with pooling

QUERY OPTIMIZATION:
- Use EXPLAIN ANALYZE for slow queries
- Consider query plan caching
- Use appropriate joins (prefer INNER JOIN over subqueries)
- Limit result sets with pagination
- Use covering indexes for frequent queries

MATERIALIZED VIEWS:
- Refresh during low-traffic periods
- Use CONCURRENTLY to avoid locks
- Monitor refresh time and optimize queries
- Consider partial refreshes for large views

REDIS CACHING STRATEGY:
- User permissions: 5-minute TTL
- Document metadata: 10-minute TTL
- Search results: 2-minute TTL
- User sessions: 30-minute TTL
- Popular documents: 1-hour TTL
- Cache invalidation on data changes

ELASTICSEARCH SYNC:
- Sync only indexed documents
- Use bulk API for efficiency
- Sync on document status changes
- Reindex on schema changes
- Monitor sync lag
*/
