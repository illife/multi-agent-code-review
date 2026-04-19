-- ============================================================================
-- Multi-Tenant Enterprise Knowledge Base - Redis Cache Integration
-- Version: V11
-- Description: Create cache-related tables and functions for Redis integration
-- ============================================================================

-- ============================================================================
-- CACHE_INVALIDATION_LOG: Track cache invalidation events
-- ============================================================================
CREATE TABLE IF NOT EXISTS cache_invalidation_log (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    cache_key VARCHAR(500) NOT NULL,
    cache_type VARCHAR(50) NOT NULL,
    invalidation_reason VARCHAR(100),
    triggered_by VARCHAR(50),
    entity_type VARCHAR(50),
    entity_id BIGINT,
    old_value JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cache_inv_tenant ON cache_invalidation_log(tenant_id);
CREATE INDEX idx_cache_inv_type ON cache_invalidation_log(cache_type);
CREATE INDEX idx_cache_inv_entity ON cache_invalidation_log(entity_type, entity_id);
CREATE INDEX idx_cache_inv_created ON cache_invalidation_log(created_at DESC);

COMMENT ON TABLE cache_invalidation_log IS 'Log of cache invalidation events for debugging and analytics';

-- ============================================================================
-- REDIS_CACHE_STATS: Cache performance statistics
-- ============================================================================
CREATE TABLE IF NOT EXISTS redis_cache_stats (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    cache_type VARCHAR(50) NOT NULL,
    hit_count BIGINT DEFAULT 0,
    miss_count BIGINT DEFAULT 0,
    eviction_count BIGINT DEFAULT 0,
    total_keys BIGINT DEFAULT 0,
    memory_used_bytes BIGINT DEFAULT 0,
    avg_ttl_seconds INTEGER,
    collected_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_redis_stats_tenant ON redis_cache_stats(tenant_id);
CREATE INDEX idx_redis_stats_type ON redis_cache_stats(cache_type);
CREATE INDEX idx_redis_stats_collected ON redis_cache_stats(collected_at DESC);

COMMENT ON TABLE redis_cache_stats IS 'Redis cache performance statistics for monitoring';

-- ============================================================================
-- CACHE_WARMING_SCHEDULE: Schedule for cache warming
-- ============================================================================
CREATE TABLE IF NOT EXISTS cache_warming_schedule (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    cache_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50),
    query_pattern TEXT,
    priority INTEGER DEFAULT 5,
    warm_up_interval_minutes INTEGER DEFAULT 60,
    last_warmed_at TIMESTAMPTZ,
    next_warm_at TIMESTAMPTZ,
    is_enabled BOOLEAN DEFAULT TRUE,
    config JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cache_warm_tenant ON cache_warming_schedule(tenant_id);
CREATE INDEX idx_cache_warm_type ON cache_warming_schedule(cache_type);
CREATE INDEX idx_cache_warm_enabled ON cache_warming_schedule(is_enabled, next_warm_at)
    WHERE is_enabled = TRUE;

COMMENT ON TABLE cache_warming_schedule IS 'Schedule for automatic cache warming';

-- ============================================================================
-- Functions for cache management
-- ============================================================================

-- Function to log cache invalidation
CREATE OR REPLACE FUNCTION log_cache_invalidation(
    p_tenant_id BIGINT,
    p_cache_key VARCHAR(500),
    p_cache_type VARCHAR(50),
    p_invalidation_reason VARCHAR(100),
    p_entity_type VARCHAR(50),
    p_entity_id BIGINT
)
RETURNS void AS $$
BEGIN
    INSERT INTO cache_invalidation_log (
        tenant_id,
        cache_key,
        cache_type,
        invalidation_reason,
        entity_type,
        entity_id
    )
    VALUES (
        p_tenant_id,
        p_cache_key,
        p_cache_type,
        p_invalidation_reason,
        p_entity_type,
        p_entity_id
    );
END;
$$ LANGUAGE plpgsql;

-- Function to record cache statistics
CREATE OR REPLACE FUNCTION record_cache_stats(
    p_tenant_id BIGINT,
    p_cache_type VARCHAR(50),
    p_hit_count BIGINT,
    p_miss_count BIGINT,
    p_eviction_count BIGINT,
    p_total_keys BIGINT,
    p_memory_used_bytes BIGINT,
    p_avg_ttl_seconds INTEGER
)
RETURNS void AS $$
BEGIN
    INSERT INTO redis_cache_stats (
        tenant_id,
        cache_type,
        hit_count,
        miss_count,
        eviction_count,
        total_keys,
        memory_used_bytes,
        avg_ttl_seconds
    )
    VALUES (
        p_tenant_id,
        p_cache_type,
        p_hit_count,
        p_miss_count,
        p_eviction_count,
        p_total_keys,
        p_memory_used_bytes,
        p_avg_ttl_seconds
    );
END;
$$ LANGUAGE plpgsql;

-- Function to get cache hit ratio
CREATE OR REPLACE FUNCTION get_cache_hit_ratio(
    p_tenant_id BIGINT,
    p_cache_type VARCHAR(50),
    p_minutes INTEGER DEFAULT 60
)
RETURNS TABLE (
    cache_type VARCHAR(50),
    hit_ratio DECIMAL(5,2),
    total_requests BIGINT,
    hit_count BIGINT,
    miss_count BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        cache_type,
        CASE
            WHEN (hit_count + miss_count) > 0 THEN
                (hit_count::DECIMAL / (hit_count + miss_count) * 100)
            ELSE 0
        END as hit_ratio,
        (hit_count + miss_count) as total_requests,
        hit_count,
        miss_count
    FROM redis_cache_stats
    WHERE tenant_id = p_tenant_id
    AND (p_cache_type IS NULL OR cache_type = p_cache_type)
    AND collected_at > NOW() - (p_minutes || ' minutes')::INTERVAL
    ORDER BY cache_type;
END;
$$ LANGUAGE plpgsql;

-- Function to identify cache warming candidates
CREATE OR REPLACE FUNCTION get_cache_warming_candidates(
    p_tenant_id BIGINT,
    p_limit INTEGER DEFAULT 100
)
RETURNS TABLE (
    entity_type VARCHAR(50),
    entity_id BIGINT,
    access_count BIGINT,
    last_accessed_at TIMESTAMPTZ
) AS $$
BEGIN
    -- Get frequently accessed documents
    RETURN QUERY
    SELECT
        'document'::VARCHAR(50) as entity_type,
        d.id as entity_id,
        d.access_count,
        d.last_accessed_at
    FROM documents_v2 d
    WHERE d.tenant_id = p_tenant_id
    AND d.deleted_at IS NULL
    AND d.access_count > 10
    AND d.last_accessed_at > NOW() - INTERVAL '7 days'
    ORDER BY d.access_count DESC
    LIMIT p_limit;

    -- Get frequently accessed users
    RETURN QUERY
    SELECT
        'user'::VARCHAR(50) as entity_type,
        u.id as entity_id,
        COALESCE(SUM(d.access_count), 0) as access_count,
        GREATEST(u.last_login_at, MAX(d.last_accessed_at)) as last_accessed_at
    FROM users_v2 u
    LEFT JOIN documents_v2 d ON d.uploaded_by = u.id AND d.deleted_at IS NULL
    WHERE u.tenant_id = p_tenant_id
    AND u.deleted_at IS NULL
    GROUP BY u.id, u.last_login_at
    HAVING COALESCE(SUM(d.access_count), 0) > 50
    ORDER BY access_count DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- Triggers for automatic cache invalidation
-- ============================================================================

-- Cache invalidation trigger for documents
CREATE OR REPLACE FUNCTION document_cache_invalidation_trigger()
RETURNS TRIGGER AS $$
DECLARE
    cache_key TEXT;
BEGIN
    IF TG_OP = 'INSERT' THEN
        -- Invalidate document list cache
        PERFORM log_cache_invalidation(
            NEW.tenant_id,
            'documents:list:' || NEW.tenant_id,
            'document_list',
            'document_created',
            'document',
            NEW.id
        );
        RETURN NEW;

    ELSIF TG_OP = 'UPDATE' THEN
        -- Invalidate specific document cache
        cache_key := 'document:' || NEW.id;

        PERFORM log_cache_invalidation(
            NEW.tenant_id,
            cache_key,
            'document_metadata',
            'document_updated',
            'document',
            NEW.id
        );

        -- If status changed to INDEXED, trigger ES sync
        IF OLD.status IS DISTINCT FROM NEW.status THEN
            PERFORM log_cache_invalidation(
                NEW.tenant_id,
                'es_sync:document:' || NEW.id,
                'elasticsearch_sync',
                'document_status_changed',
                'document',
                NEW.id
            );
        END IF;

        RETURN NEW;

    ELSIF TG_OP = 'DELETE' THEN
        -- Invalidate document cache
        cache_key := 'document:' || OLD.id;

        PERFORM log_cache_invalidation(
            OLD.tenant_id,
            cache_key,
            'document_metadata',
            'document_deleted',
            'document',
            OLD.id
        );

        RETURN OLD;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Create triggers for cache invalidation
DROP TRIGGER IF EXISTS document_cache_invalidation ON documents_v2;
CREATE TRIGGER document_cache_invalidation
    AFTER INSERT OR UPDATE OR DELETE ON documents_v2
    FOR EACH ROW EXECUTE FUNCTION document_cache_invalidation_trigger();

-- Cache invalidation trigger for users
CREATE OR REPLACE FUNCTION user_cache_invalidation_trigger()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        -- Invalidate user permissions cache
        PERFORM log_cache_invalidation(
            NEW.tenant_id,
            'user:permissions:' || NEW.id,
            'user_permissions',
            'user_updated',
            'user',
            NEW.id
        );

        -- Invalidate user profile cache
        PERFORM log_cache_invalidation(
            NEW.tenant_id,
            'user:profile:' || NEW.id,
            'user_profile',
            'user_updated',
            'user',
            NEW.id
        );

        RETURN NEW;

    ELSIF TG_OP = 'DELETE' THEN
        -- Invalidate all user-related caches
        PERFORM log_cache_invalidation(
            OLD.tenant_id,
            'user:permissions:' || OLD.id,
            'user_permissions',
            'user_deleted',
            'user',
            OLD.id
        );

        RETURN OLD;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for user cache invalidation
DROP TRIGGER IF EXISTS user_cache_invalidation ON users_v2;
CREATE TRIGGER user_cache_invalidation
    AFTER UPDATE OR DELETE ON users_v2
    FOR EACH ROW EXECUTE FUNCTION user_cache_invalidation_trigger();

-- Cache invalidation trigger for user roles
CREATE OR REPLACE FUNCTION user_role_cache_invalidation_trigger()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' OR TG_OP = 'DELETE' THEN
        -- Invalidate user permissions cache when roles change
        PERFORM log_cache_invalidation(
            COALESCE(NEW.tenant_id, OLD.tenant_id),
            'user:permissions:' || COALESCE(NEW.user_id, OLD.user_id),
            'user_permissions',
            'role_changed',
            'user_role',
            COALESCE(NEW.user_id, OLD.user_id)
        );
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for user role cache invalidation
DROP TRIGGER IF EXISTS user_role_cache_invalidation ON user_roles_v2;
CREATE TRIGGER user_role_cache_invalidation
    AFTER INSERT OR UPDATE OR DELETE ON user_roles_v2
    FOR EACH ROW EXECUTE FUNCTION user_role_cache_invalidation_trigger();

-- ============================================================================
-- View for cache performance monitoring
-- ============================================================================

CREATE OR REPLACE VIEW cache_performance_summary AS
SELECT
    tenant_id,
    cache_type,
    DATE_TRUNC('hour', collected_at) as hour,
    AVG(hit_count::DECIMAL / NULLIF(hit_count + miss_count, 0) * 100) as avg_hit_ratio_percent,
    SUM(hit_count + miss_count) as total_requests,
    SUM(hit_count) as total_hits,
    SUM(miss_count) as total_misses,
    AVG(eviction_count) as avg_evictions,
    AVG(memory_used_bytes) as avg_memory_bytes
FROM redis_cache_stats
WHERE collected_at > NOW() - INTERVAL '24 hours'
GROUP BY tenant_id, cache_type, DATE_TRUNC('hour', collected_at)
ORDER BY tenant_id, cache_type, hour DESC;

COMMENT ON VIEW cache_performance_summary IS 'Hourly cache performance summary for monitoring';

-- ============================================================================
-- Insert default cache warming schedule
-- ============================================================================
INSERT INTO cache_warming_schedule (
    tenant_id,
    cache_type,
    entity_type,
    query_pattern,
    priority,
    warm_up_interval_minutes,
    next_warm_at,
    config
)
SELECT
    t.id,
    'hot_documents',
    'document',
    'SELECT * FROM documents_v2 WHERE tenant_id = ? ORDER BY access_count DESC LIMIT 100',
    8,
    30,
    NOW() + INTERVAL '30 minutes',
    '{"limit": 100, "min_access_count": 10}'::JSONB
FROM tenants t
ON CONFLICT DO NOTHING;

INSERT INTO cache_warming_schedule (
    tenant_id,
    cache_type,
    entity_type,
    query_pattern,
    priority,
    warm_up_interval_minutes,
    next_warm_at,
    config
)
SELECT
    t.id,
    'user_permissions',
    'user',
    'SELECT * FROM users_v2 WHERE tenant_id = ? AND is_active = TRUE',
    10,
    5,
    NOW() + INTERVAL '5 minutes',
    '{"batch_size": 100}'::JSONB
FROM tenants t
ON CONFLICT DO NOTHING;

-- ============================================================================
-- Helpful administrative functions
-- ============================================================================

-- Function to clear all caches for a tenant
CREATE OR REPLACE FUNCTION clear_tenant_caches(p_tenant_id BIGINT)
RETURNS TABLE (
    cache_type VARCHAR(50),
    keys_affected BIGINT
) AS $$
BEGIN
    -- This function should be called by application code that interacts with Redis
    -- It logs the invalidation events for tracking
    INSERT INTO cache_invalidation_log (
        tenant_id,
        cache_key,
        cache_type,
        invalidation_reason,
        triggered_by
    )
    SELECT
        p_tenant_id,
        'tenant:*:' || p_tenant_id::TEXT,
        'all_tenant_caches',
        'manual_tenant_clear',
        'admin_function'
    );

    -- Return expected cache types to clear
    RETURN QUERY
    SELECT
        cache_type::VARCHAR(50),
        0::BIGINT as keys_affected
    FROM (
        SELECT 'document_list' as cache_type
        UNION SELECT 'document_metadata'
        UNION SELECT 'user_permissions'
        UNION SELECT 'user_profile'
        UNION SELECT 'search_results'
        UNION SELECT 'qa_answers'
    ) types;
END;
$$ LANGUAGE plpgsql;

-- Function to get cache optimization recommendations
CREATE OR REPLACE FUNCTION get_cache_optimization_recommendations(p_tenant_id BIGINT)
RETURNS TABLE (
    cache_type VARCHAR(50),
    recommendation TEXT,
    priority VARCHAR(10),
    current_hit_ratio DECIMAL(5,2)
) AS $$
BEGIN
    RETURN QUERY
    WITH cache_stats AS (
        SELECT
            cache_type,
            AVG(hit_count::DECIMAL / NULLIF(hit_count + miss_count, 0) * 100) as hit_ratio
        FROM redis_cache_stats
        WHERE tenant_id = p_tenant_id
        AND collected_at > NOW() - INTERVAL '24 hours'
        GROUP BY cache_type
    )
    SELECT
        cs.cache_type,
        CASE
            WHEN cs.hit_ratio < 50 THEN
                'Low hit ratio (' || cs.hit_ratio::TEXT || '%). Consider increasing TTL or cache size.'
            WHEN cs.hit_ratio < 70 THEN
                'Moderate hit ratio (' || cs.hit_ratio::TEXT || '%). Review cache key patterns and expiration.'
            WHEN cs.hit_ratio < 85 THEN
                'Good hit ratio (' || cs.hit_ratio::TEXT || '%). Minor optimizations possible.'
            ELSE
                'Excellent hit ratio (' || cs.hit_ratio::TEXT || '%). No action needed.'
        END as recommendation,
        CASE
            WHEN cs.hit_ratio < 50 THEN 'HIGH'
            WHEN cs.hit_ratio < 70 THEN 'MEDIUM'
            ELSE 'LOW'
        END as priority,
        cs.hit_ratio
    FROM cache_stats cs
    ORDER BY cs.hit_ratio ASC;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION get_cache_optimization_recommendations IS 'Get cache optimization recommendations based on performance metrics';

-- ============================================================================
-- Redis Integration Notes
-- ============================================================================
/*
CACHE KEY PATTERNS:
- user:permissions:{userId} - User permissions (TTL: 5 min)
- user:profile:{userId} - User profile data (TTL: 10 min)
- document:metadata:{documentId} - Document metadata (TTL: 10 min)
- document:content:{documentId} - Document content (TTL: 1 hour)
- documents:list:{tenantId}:{filters} - Document lists (TTL: 2 min)
- search:results:{queryHash} - Search results (TTL: 2 min)
- qa:answer:{qaId} - QA answers (TTL: 10 min)
- es:sync:{entityType}:{entityId} - Elasticsearch sync flags (TTL: 1 hour)

CACHE INVALIDATION STRATEGY:
1. Database triggers log invalidation events
2. Application consumes logs and invalidates Redis keys
3. Fallback: TTL-based expiration

CACHE WARMING:
- Schedule based on access patterns
- Priority: user permissions > hot documents > search results
- Frequency: Every 5-30 minutes based on cache type

MONITORING:
- Track hit/miss ratios
- Monitor memory usage
- Log eviction events
- Alert on low hit ratios

CONNECTION POOLING:
- Lettuce pool with 8-16 connections
- Connection timeout: 10s
- Command timeout: 5s
*/
