-- ============================================================================
-- Multi-Tenant Enterprise Knowledge Base - Tenant Isolation
-- Version: V9
-- Description: Migrate existing data to multi-tenant structure and create tenant isolation
-- ============================================================================

-- ============================================================================
-- Step 1: Create default tenant for existing data
-- ============================================================================
INSERT INTO tenants (name, slug, domain, subscription_tier, max_users, max_storage_gb, is_active)
VALUES (
    'Default Organization',
    'default',
    'localhost',
    'ENTERPRISE',
    1000,
    1000,
    TRUE
)
ON CONFLICT (slug) DO NOTHING
RETURNING id;

-- Get the default tenant ID (we'll use this in subsequent steps)
DO $$
DECLARE
    default_tenant_id BIGINT;
    default_role_id BIGINT;
BEGIN
    -- Get or create default tenant
    SELECT id INTO default_tenant_id
    FROM tenants
    WHERE slug = 'default'
    LIMIT 1;

    IF default_tenant_id IS NULL THEN
        INSERT INTO tenants (name, slug, domain, subscription_tier, max_users, max_storage_gb, is_active)
        VALUES ('Default Organization', 'default', 'localhost', 'ENTERPRISE', 1000, 1000, TRUE)
        RETURNING id INTO default_tenant_id;
    END IF;

    -- Create default roles for the tenant
    INSERT INTO roles_v2 (tenant_id, name, description, is_system_role, permissions)
    VALUES
        (default_tenant_id, 'SUPER_ADMIN', 'Full system administrator access', TRUE,
         '{"permissions": ["document.admin", "code_review.admin", "user.admin", "tenant.admin"]}'),
        (default_tenant_id, 'ADMIN', 'Administrator with full access to documents and code reviews', TRUE,
         '{"permissions": ["document.admin", "code_review.admin", "user.read"]}'),
        (default_tenant_id, 'USER', 'Standard user with basic access', TRUE,
         '{"permissions": ["document.read", "document.write", "code_review.read"]}'),
        (default_tenant_id, 'VIEWER', 'Read-only access', TRUE,
         '{"permissions": ["document.read", "code_review.read"]}')
    ON CONFLICT (tenant_id, name) DO NOTHING;

    -- Get the default USER role ID
    SELECT id INTO default_role_id
    FROM roles_v2
    WHERE tenant_id = default_tenant_id AND name = 'USER'
    LIMIT 1;

    -- ============================================================================
    -- Step 2: Migrate existing users to users_v2
    -- ============================================================================
    INSERT INTO users_v2 (
        tenant_id, username, email, password_hash, full_name, department,
        is_active, last_login_at, created_at, updated_at
    )
    SELECT
        default_tenant_id,
        username,
        email,
        password_hash,
        full_name,
        department,
        is_active,
        last_login_at,
        created_at,
        updated_at
    FROM users
    ON CONFLICT (tenant_id, email) DO NOTHING;

    -- Assign default USER role to migrated users
    INSERT INTO user_roles_v2 (tenant_id, user_id, role_id, assigned_at)
    SELECT
        default_tenant_id,
        u2.id,
        default_role_id,
        NOW()
    FROM users_v2 u2
    WHERE u2.tenant_id = default_tenant_id
    AND NOT EXISTS (
        SELECT 1 FROM user_roles_v2 ur
        WHERE ur.user_id = u2.id
    );

    RAISE NOTICE 'Successfully migrated users to multi-tenant structure';
END $$;

-- ============================================================================
-- Step 3: Migrate existing documents to documents_v2
-- ============================================================================
DO $$
DECLARE
    default_tenant_id BIGINT;
BEGIN
    SELECT id INTO default_tenant_id
    FROM tenants
    WHERE slug = 'default'
    LIMIT 1;

    IF default_tenant_id IS NOT NULL THEN
        -- Create a mapping of old document IDs to new document IDs
        WITH old_documents AS (
            SELECT
                id,
                title,
                description,
                file_name,
                file_path,
                file_type,
                file_size,
                storage_type,
                storage_path,
                uploaded_by_id,
                department,
                tags,
                is_public,
                status,
                indexed_at,
                created_at,
                updated_at,
                error_message,
                file_md5,
                upload_id,
                total_chunks,
                chunk_count
            FROM documents
        ),
        inserted_documents AS (
            INSERT INTO documents_v2 (
                tenant_id,
                title,
                description,
                file_name,
                file_path,
                file_type,
                file_size,
                storage_type,
                storage_path,
                uploaded_by,
                department,
                tags,
                is_public,
                status,
                indexed_at,
                created_at,
                updated_at,
                error_message,
                file_md5,
                upload_id,
                total_chunks,
                chunk_count,
                version_number
            )
            SELECT
                default_tenant_id,
                title,
                description,
                file_name,
                file_path,
                file_type,
                file_size,
                storage_type,
                storage_path,
                uploaded_by_id,
                department,
                tags,
                is_public,
                status::VARCHAR(20),
                indexed_at,
                created_at,
                updated_at,
                errorMessage,
                file_md5,
                upload_id,
                total_chunks,
                chunk_count,
                1
            FROM old_documents
            ON CONFLICT DO NOTHING
            RETURNING id, title
        )
        SELECT COUNT(*) INTO inserted_documents
        FROM inserted_documents;

        RAISE NOTICE 'Successfully migrated documents to multi-tenant structure';
    END IF;
END $$;

-- ============================================================================
-- Step 4: Migrate document permissions
-- ============================================================================
DO $$
DECLARE
    default_tenant_id BIGINT;
BEGIN
    SELECT id INTO default_tenant_id
    FROM tenants
    WHERE slug = 'default'
    LIMIT 1;

    IF default_tenant_id IS NOT NULL THEN
        INSERT INTO document_permissions_v2 (
            tenant_id,
            document_id,
            user_id,
            permission_type,
            granted_by,
            granted_at
        )
        SELECT
            default_tenant_id,
            d2.id,
            dp.user_id,
            dp.permission_type::VARCHAR(20),
            dp.granted_by,
            dp.granted_at
        FROM document_permissions dp
        JOIN documents d ON dp.document_id = d.id
        JOIN documents_v2 d2 ON d2.title = d.title
            AND d2.file_name = d.file_name
            AND d2.tenant_id = default_tenant_id
        WHERE dp.user_id IS NOT NULL
        ON CONFLICT (tenant_id, document_id, user_id, permission_type) DO NOTHING;

        RAISE NOTICE 'Successfully migrated document permissions';
    END IF;
END $$;

-- ============================================================================
-- Step 5: Migrate document chunks
-- ============================================================================
DO $$
DECLARE
    default_tenant_id BIGINT;
BEGIN
    SELECT id INTO default_tenant_id
    FROM tenants
    WHERE slug = 'default'
    LIMIT 1;

    IF default_tenant_id IS NOT NULL THEN
        INSERT INTO document_chunks_v2 (
            tenant_id,
            document_id,
            parent_chunk_id,
            chunk_type,
            content,
            position,
            char_count,
            embedding_vector,
            embedding_model,
            vector_dimensions,
            created_at
        )
        SELECT
            default_tenant_id,
            d2.id,
            dc2.parent_chunk_id,
            dc.chunk_type,
            dc.content,
            dc.position,
            dc.char_count,
            dc.embedding_vector,
            dc.embedding_model,
            dc.vector_dimensions,
            dc.created_at
        FROM document_chunks dc
        JOIN documents d ON dc.document_id = d.id
        JOIN documents_v2 d2 ON d2.title = d.title
            AND d2.file_name = d.file_name
            AND d2.tenant_id = default_tenant_id
        LEFT JOIN document_chunks_v2 dc2 ON dc2.id = dc.parent_chunk_id
        ON CONFLICT DO NOTHING;

        RAISE NOTICE 'Successfully migrated document chunks';
    END IF;
END $$;

-- ============================================================================
-- Step 6: Migrate QA history
-- ============================================================================
DO $$
DECLARE
    default_tenant_id BIGINT;
BEGIN
    SELECT id INTO default_tenant_id
    FROM tenants
    WHERE slug = 'default'
    LIMIT 1;

    IF default_tenant_id IS NOT NULL THEN
        INSERT INTO qa_history_v2 (
            tenant_id,
            user_id,
            question,
            answer,
            sources,
            context_used,
            feedback,
            created_at
        )
        SELECT
            default_tenant_id,
            qa.user_id,
            qa.question,
            qa.answer,
            qa.sources,
            qa.context_used,
            qa.feedback,
            qa.created_at
        FROM qa_history qa
        ON CONFLICT DO NOTHING;

        RAISE NOTICE 'Successfully migrated QA history';
    END IF;
END $$;

-- ============================================================================
-- Step 7: Migrate code reviews
-- ============================================================================
DO $$
DECLARE
    default_tenant_id BIGINT;
BEGIN
    SELECT id INTO default_tenant_id
    FROM tenants
    WHERE slug = 'default'
    LIMIT 1;

    IF default_tenant_id IS NOT NULL THEN
        INSERT INTO code_reviews_v2 (
            tenant_id,
            user_id,
            code_content,
            language,
            file_name,
            status,
            visibility,
            total_issues,
            created_at,
            updated_at
        )
        SELECT
            default_tenant_id,
            cr.user_id,
            cr.code_content,
            cr.language,
            cr.file_name,
            cr.status::VARCHAR(20),
            cr.visibility::VARCHAR(20),
            cr.total_issues,
            cr.created_at,
            cr.updated_at
        FROM code_reviews cr
        ON CONFLICT DO NOTHING;

        -- Migrate code issues
        INSERT INTO code_issues_v2 (
            tenant_id,
            review_id,
            severity,
            category,
            title,
            description,
            code_snippet,
            line_number,
            suggestion,
            agent_type,
            tool_name,
            rule_id,
            teaching_explanation,
            created_at
        )
        SELECT
            default_tenant_id,
            ci2.id,
            ci.severity::VARCHAR(20),
            ci.category,
            ci.title,
            ci.description,
            ci.code_snippet,
            ci.line_number,
            ci.suggestion,
            ci.agent_type,
            ci.tool_name,
            ci.rule_id,
            ci.teaching_explanation,
            ci.created_at
        FROM code_issues ci
        JOIN code_reviews cr ON ci.review_id = cr.id
        JOIN code_reviews_v2 ci2 ON ci2.user_id = cr.user_id
            AND ci2.code_content = cr.code_content
            AND ci2.tenant_id = default_tenant_id
        ON CONFLICT DO NOTHING;

        RAISE NOTICE 'Successfully migrated code reviews';
    END IF;
END $$;

-- ============================================================================
-- Step 8: Create helpful administrative views
-- ============================================================================
CREATE OR REPLACE VIEW tenant_summary AS
SELECT
    t.id,
    t.name,
    t.slug,
    t.subscription_tier,
    t.max_users,
    t.max_storage_gb,
    t.is_active,
    COUNT(DISTINCT u.id) as user_count,
    COUNT(DISTINCT d.id) as document_count,
    SUM(d.file_size) / (1024.0 * 1024.0 * 1024.0) as storage_used_gb,
    t.created_at,
    t.updated_at
FROM tenants t
LEFT JOIN users_v2 u ON u.tenant_id = t.id AND u.deleted_at IS NULL
LEFT JOIN documents_v2 d ON d.tenant_id = t.id AND d.deleted_at IS NULL
GROUP BY t.id, t.name, t.slug, t.subscription_tier, t.max_users, t.max_storage_gb, t.is_active, t.created_at, t.updated_at;

COMMENT ON VIEW tenant_summary IS 'Administrative view showing tenant usage statistics';

CREATE OR REPLACE VIEW user_document_access AS
SELECT
    u.tenant_id,
    u.id as user_id,
    u.username,
    u.email,
    u.department,
    COUNT(DISTINCT CASE WHEN dp.permission_type IS NOT NULL THEN d.id END) as documents_with_permission,
    COUNT(DISTINCT CASE WHEN d.uploaded_by = u.id THEN d.id END) as owned_documents,
    COUNT(DISTINCT CASE WHEN d.is_public = TRUE THEN d.id END) as public_documents,
    COUNT(DISTINCT d.id) as total_accessible_documents
FROM users_v2 u
LEFT JOIN document_permissions_v2 dp ON dp.user_id = u.id AND dp.deleted_at IS NULL
LEFT JOIN documents_v2 d ON d.id = dp.document_id OR d.uploaded_by = u.id OR d.is_public = TRUE
WHERE u.deleted_at IS NULL AND d.deleted_at IS NULL
GROUP BY u.tenant_id, u.id, u.username, u.email, u.department;

COMMENT ON VIEW user_document_access IS 'View showing user document access statistics';

-- ============================================================================
-- Step 9: Create administrative functions
-- ============================================================================
CREATE OR REPLACE FUNCTION add_user_to_tenant(
    p_tenant_slug VARCHAR(100),
    p_username VARCHAR(50),
    p_email VARCHAR(100),
    p_password_hash VARCHAR(255),
    p_full_name VARCHAR(100),
    p_role_name VARCHAR(50) DEFAULT 'USER'
)
RETURNS BIGINT AS $$
DECLARE
    v_tenant_id BIGINT;
    v_user_id BIGINT;
    v_role_id BIGINT;
BEGIN
    -- Get tenant ID
    SELECT id INTO v_tenant_id
    FROM tenants
    WHERE slug = p_tenant_slug AND is_active = TRUE;

    IF v_tenant_id IS NULL THEN
        RAISE EXCEPTION 'Tenant not found or inactive: %', p_tenant_slug;
    END IF;

    -- Check if user already exists
    SELECT id INTO v_user_id
    FROM users_v2
    WHERE tenant_id = v_tenant_id
    AND email = p_email
    LIMIT 1;

    IF v_user_id IS NOT NULL THEN
        RAISE EXCEPTION 'User already exists with email: %', p_email;
    END IF;

    -- Create user
    INSERT INTO users_v2 (
        tenant_id,
        username,
        email,
        password_hash,
        full_name,
        is_active,
        is_email_verified
    )
    VALUES (
        v_tenant_id,
        p_username,
        p_email,
        p_password_hash,
        p_full_name,
        TRUE,
        FALSE
    )
    RETURNING id INTO v_user_id;

    -- Get role ID
    SELECT id INTO v_role_id
    FROM roles_v2
    WHERE tenant_id = v_tenant_id
    AND name = p_role_name
    LIMIT 1;

    IF v_role_id IS NULL THEN
        RAISE EXCEPTION 'Role not found: %', p_role_name;
    END IF;

    -- Assign role
    INSERT INTO user_roles_v2 (tenant_id, user_id, role_id)
    VALUES (v_tenant_id, v_user_id, v_role_id);

    RETURN v_user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

COMMENT ON FUNCTION add_user_to_tenant IS 'Administrative function to add user to tenant with role assignment';

CREATE OR REPLACE FUNCTION get_tenant_storage_usage(p_tenant_id BIGINT)
RETURNS TABLE(
    total_documents BIGINT,
    total_file_size BIGINT,
    total_file_size_gb NUMERIC,
    avg_file_size NUMERIC,
    largest_document_size BIGINT,
    storage_percentage NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        COUNT(*)::BIGINT,
        COALESCE(SUM(d.file_size), 0)::BIGINT,
        COALESCE(SUM(d.file_size), 0)::NUMERIC / (1024 * 1024 * 1024) as size_gb,
        COALESCE(AVG(d.file_size), 0)::NUMERIC,
        COALESCE(MAX(d.file_size), 0)::BIGINT,
        CASE
            WHEN t.max_storage_gb > 0 THEN
                (COALESCE(SUM(d.file_size), 0)::NUMERIC / (1024 * 1024 * 1024) / t.max_storage_gb) * 100
            ELSE 0
        END
    FROM tenants t
    LEFT JOIN documents_v2 d ON d.tenant_id = t.id AND d.deleted_at IS NULL
    WHERE t.id = p_tenant_id
    GROUP BY t.id, t.max_storage_gb;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

COMMENT ON FUNCTION get_tenant_storage_usage IS 'Get storage usage statistics for a tenant';

-- ============================================================================
-- Step 10: Setup automatic refresh of materialized views
-- ============================================================================
-- Create a function to refresh materialized views
CREATE OR REPLACE FUNCTION refresh_analytics_views()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY document_analytics_mv;
    REFRESH MATERIALIZED VIEW CONCURRENTLY user_activity_mv;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION refresh_analytics_views IS 'Refresh all analytics materialized views concurrently';

-- Note: Schedule this function to run every 5-15 minutes using pg_cron or external scheduler
-- Example with pg_cron:
-- SELECT cron.schedule('refresh-analytics', '*/10 * * * *', 'SELECT refresh_analytics_views();');

-- ============================================================================
-- DATA MIGRATION NOTES
-- ============================================================================
--
-- This migration script:
-- 1. Creates a default tenant for all existing data
-- 2. Migrates all existing users to the new multi-tenant structure
-- 3. Migrates all documents with proper tenant assignment
-- 4. Migrates all permissions, chunks, QA history, and code reviews
-- 5. Creates administrative views and functions for tenant management
--
-- IMPORTANT CONSIDERATIONS:
--
-- 1. BACKUP FIRST: Always backup your database before running this migration
--
-- 2. RUN IN TRANSACTION: Consider running this within a transaction for rollback capability
--
-- 3. VERIFY DATA: After migration, verify:
--    - All users have been migrated
--    - All documents maintain their relationships
--    - Permissions are correctly assigned
--    - No orphaned records exist
--
-- 4. UPDATE APPLICATION: Update application code to:
--    - Set tenant context on login
--    - Include tenant_id in all queries
--    - Use views instead of base tables where appropriate
--    - Implement tenant isolation in application layer
--
-- 5. PERFORMANCE: For large datasets, consider:
--    - Running migration in batches
--    - Disabling triggers during migration
--    - Running during low-traffic periods
--    - Monitoring performance during migration
--
-- 6. CLEANUP: After successful migration:
--    - Drop old tables (keep backup for a while)
--    - Update connection pooling settings
--    - Remove legacy code references
--    - Update monitoring and alerting
-- ============================================================================
