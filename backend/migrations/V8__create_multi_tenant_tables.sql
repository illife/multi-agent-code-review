-- ============================================================================
-- Multi-Tenant Enterprise Knowledge Base - Database Migration
-- Version: V8
-- Description: Create comprehensive multi-tenant database schema with
--              audit logging, soft deletes, version control, and performance optimization
-- ============================================================================
--
-- DESIGN DECISIONS:
-- 1. Multi-tenancy: Shared database, shared schema with tenant_id column
--    - More efficient resource utilization
--    - Easier maintenance and backup
--    - Row-level security via PostgreSQL policies
--
-- 2. Soft Deletes: deleted_at column with partial indexes
--    - Compliance requirements (audit trail)
--    - Data recovery capability
--    - Partial indexes exclude deleted rows for performance
--
-- 3. Audit Logging: Separate audit tables with triggers
--    - Complete audit trail without impacting query performance
--    - Async population via triggers
--    - Separate storage enables efficient archival
--
-- 4. Version Control: Document versions with parent-child relationship
--    - Revert capability
--    - Change tracking
--    - Storage optimization via delta compression
--
-- 5. Performance Optimization:
--    - Composite indexes for common query patterns
--    - Partial indexes for filtered queries
--    - Covering indexes to reduce table access
--    - JSONB for flexible metadata storage
-- ============================================================================

-- ============================================================================
-- TENANTS: Multi-tenant organization management
-- ============================================================================
CREATE TABLE IF NOT EXISTS tenants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    domain VARCHAR(255),
    logo_url VARCHAR(500),
    settings JSONB DEFAULT '{}',
    subscription_tier VARCHAR(20) NOT NULL DEFAULT 'FREE'
        CHECK (subscription_tier IN ('FREE', 'PRO', 'ENTERPRISE')),
    max_users INTEGER DEFAULT 5,
    max_storage_gb INTEGER DEFAULT 10,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for common queries
CREATE INDEX idx_tenants_slug ON tenants(slug);
CREATE INDEX idx_tenants_active ON tenants(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_tenants_tier ON tenants(subscription_tier);
CREATE INDEX idx_tenants_settings ON tenants USING GIN(settings);

-- Comments for documentation
COMMENT ON TABLE tenants IS 'Multi-tenant organization management';
COMMENT ON COLUMN tenants.slug IS 'Unique identifier for routing (subdomain)';
COMMENT ON COLUMN tenants.subscription_tier IS 'Subscription tier: FREE, PRO, ENTERPRISE';
COMMENT ON COLUMN tenants.settings IS 'Tenant-specific settings (JSONB)';

-- ============================================================================
-- USERS: Enhanced user table with tenant support
-- ============================================================================
CREATE TABLE IF NOT EXISTS users_v2 (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    department VARCHAR(100),
    phone VARCHAR(20),
    avatar_url VARCHAR(500),
    profile_data JSONB DEFAULT '{}',
    is_active BOOLEAN DEFAULT TRUE,
    is_email_verified BOOLEAN DEFAULT FALSE,
    last_login_at TIMESTAMPTZ,
    email_verified_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, username),
    UNIQUE(tenant_id, email)
);

-- Performance indexes
CREATE INDEX idx_users_tenant_id ON users_v2(tenant_id);
CREATE INDEX idx_users_email ON users_v2(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_username ON users_v2(username) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_active ON users_v2(is_active) WHERE is_active = TRUE AND deleted_at IS NULL;
CREATE INDEX idx_users_department ON users_v2(department) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_last_login ON users_v2(last_login_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_profile ON users_v2 USING GIN(profile_data);

-- Full-text search index
CREATE INDEX idx_users_fulltext ON users_v2 USING GIN(
    to_tsvector('simple',
        COALESCE(full_name, '') || ' ' ||
        COALESCE(email, '') || ' ' ||
        COALESCE(department, '')
    )
) WHERE deleted_at IS NULL;

COMMENT ON TABLE users_v2 IS 'Enhanced user table with multi-tenant support';
COMMENT ON COLUMN users_v2.tenant_id IS 'Tenant organization ID';
COMMENT ON COLUMN users_v2.deleted_at IS 'Soft delete timestamp (NULL = active)';
COMMENT ON COLUMN users_v2.profile_data IS 'Flexible user profile data (JSONB)';

-- ============================================================================
-- ROLES: Enhanced role management with tenant support
-- ============================================================================
CREATE TABLE IF NOT EXISTS roles_v2 (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    name VARCHAR(50) NOT NULL,
    description TEXT,
    is_system_role BOOLEAN DEFAULT FALSE,
    permissions JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, name)
);

CREATE INDEX idx_roles_tenant_id ON roles_v2(tenant_id);
CREATE INDEX idx_roles_system ON roles_v2(is_system_role) WHERE is_system_role = TRUE;
CREATE INDEX idx_roles_permissions ON roles_v2 USING GIN(permissions);

COMMENT ON TABLE roles_v2 IS 'Role definitions with tenant isolation';
COMMENT ON COLUMN roles_v2.is_system_role IS 'System roles cannot be deleted';

-- ============================================================================
-- USER_ROLES: User-role assignments with audit trail
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_roles_v2 (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    user_id BIGINT NOT NULL REFERENCES users_v2(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles_v2(id) ON DELETE CASCADE,
    assigned_by BIGINT REFERENCES users_v2(id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ,
    UNIQUE(tenant_id, user_id, role_id)
);

CREATE INDEX idx_user_roles_tenant ON user_roles_v2(tenant_id);
CREATE INDEX idx_user_roles_user ON user_roles_v2(user_id) WHERE expires_at IS NULL OR expires_at > NOW();
CREATE INDEX idx_user_roles_role ON user_roles_v2(role_id);
CREATE INDEX idx_user_roles_assigned_by ON user_roles_v2(assigned_by);

COMMENT ON TABLE user_roles_v2 IS 'User-role assignments with expiration and audit trail';

-- ============================================================================
-- PERMISSIONS: Granular permission definitions
-- ============================================================================
CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(resource, action)
);

CREATE INDEX idx_permissions_resource ON permissions(resource);
CREATE INDEX idx_permissions_action ON permissions(action);

COMMENT ON TABLE permissions IS 'Granular permission definitions for RBAC';

-- Seed default permissions
INSERT INTO permissions (resource, action, description) VALUES
('document', 'read', 'Read documents'),
('document', 'write', 'Create and edit documents'),
('document', 'delete', 'Delete documents'),
('document', 'share', 'Share documents with others'),
('document', 'admin', 'Full administrative access to documents'),
('code_review', 'read', 'Read code reviews'),
('code_review', 'write', 'Create and edit code reviews'),
('code_review', 'delete', 'Delete code reviews'),
('code_review', 'admin', 'Full administrative access to code reviews'),
('user', 'read', 'Read user information'),
('user', 'write', 'Create and edit users'),
('user', 'delete', 'Delete users'),
('user', 'admin', 'Full administrative access to users'),
('tenant', 'admin', 'Full administrative access to tenant settings')
ON CONFLICT (resource, action) DO NOTHING;

-- ============================================================================
-- DOCUMENTS: Enhanced document management with versioning
-- ============================================================================
CREATE TABLE IF NOT EXISTS documents_v2 (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    parent_version_id BIGINT REFERENCES documents_v2(id),
    version_number INTEGER DEFAULT 1,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500),
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT,
    file_md5 VARCHAR(64),
    storage_type VARCHAR(20) NOT NULL DEFAULT 'LOCAL'
        CHECK (storage_type IN ('LOCAL', 'MINIO', 'S3', 'OSS')),
    storage_path VARCHAR(500),
    upload_id VARCHAR(64),
    total_chunks INTEGER,
    chunk_count INTEGER,
    uploaded_by BIGINT NOT NULL REFERENCES users_v2(id),
    department VARCHAR(100),
    tags VARCHAR(500)[],
    metadata JSONB DEFAULT '{}',
    is_public BOOLEAN DEFAULT FALSE,
    visibility VARCHAR(20) DEFAULT 'PRIVATE'
        CHECK (visibility IN ('PRIVATE', 'DEPARTMENT', 'PUBLIC')),
    status VARCHAR(20) DEFAULT 'PROCESSING'
        CHECK (status IN ('PENDING', 'PROCESSING', 'INDEXED', 'FAILED', 'ARCHIVED')),
    error_message TEXT,
    indexed_at TIMESTAMPTZ,
    last_accessed_at TIMESTAMPTZ,
    access_count INTEGER DEFAULT 0,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Core performance indexes
CREATE INDEX idx_documents_tenant_id ON documents_v2(tenant_id);
CREATE INDEX idx_documents_parent_version ON documents_v2(parent_version_id);
CREATE INDEX idx_documents_uploaded_by ON documents_v2(uploaded_by);
CREATE INDEX idx_documents_file_type ON documents_v2(file_type);
CREATE INDEX idx_documents_status ON documents_v2(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_documents_visibility ON documents_v2(visibility) WHERE deleted_at IS NULL;
CREATE INDEX idx_documents_public ON documents_v2(is_public) WHERE is_public = TRUE AND deleted_at IS NULL;
CREATE INDEX idx_documents_department ON documents_v2(department) WHERE deleted_at IS NULL;
CREATE INDEX idx_documents_tags ON documents_v2 USING GIN(tags);
CREATE INDEX idx_documents_metadata ON documents_v2 USING GIN(metadata);
CREATE INDEX idx_documents_indexed ON documents_v2(indexed_at DESC) WHERE status = 'INDEXED' AND deleted_at IS NULL;
CREATE INDEX idx_documents_accessed ON documents_v2(last_accessed_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_documents_access_count ON documents_v2(access_count DESC) WHERE deleted_at IS NULL;

-- Composite indexes for common query patterns
CREATE INDEX idx_documents_tenant_status ON documents_v2(tenant_id, status)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_documents_tenant_visibility ON documents_v2(tenant_id, visibility)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_documents_user_public ON documents_v2(uploaded_by, is_public)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_documents_tenant_created ON documents_v2(tenant_id, created_at DESC)
    WHERE deleted_at IS NULL;

-- Covering index for document list queries
CREATE INDEX idx_documents_list_covering ON documents_v2(tenant_id, status, created_at DESC)
    INCLUDE (title, file_type, uploaded_by, visibility)
    WHERE deleted_at IS NULL;

-- Full-text search on title and description
CREATE INDEX idx_documents_fulltext ON documents_v2 USING GIN(
    to_tsvector('simple',
        COALESCE(title, '') || ' ' ||
        COALESCE(description, '')
    )
) WHERE deleted_at IS NULL;

COMMENT ON TABLE documents_v2 IS 'Enhanced document management with versioning and soft deletes';
COMMENT ON COLUMN documents_v2.parent_version_id IS 'Parent document version ID (NULL = original)';
COMMENT ON COLUMN documents_v2.version_number IS 'Document version number';
COMMENT ON COLUMN documents_v2.metadata IS 'Flexible document metadata (JSONB)';
COMMENT ON COLUMN documents_v2.access_count IS 'Document access counter for analytics';
COMMENT ON COLUMN documents_v2.deleted_at IS 'Soft delete timestamp (NULL = active)';

-- ============================================================================
-- DOCUMENT_PERMISSIONS: Fine-grained document access control
-- ============================================================================
CREATE TABLE IF NOT EXISTS document_permissions_v2 (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    document_id BIGINT NOT NULL REFERENCES documents_v2(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users_v2(id),
    role_id BIGINT REFERENCES roles_v2(id),
    permission_type VARCHAR(20) NOT NULL
        CHECK (permission_type IN ('READ', 'WRITE', 'DELETE', 'ADMIN')),
    granted_by BIGINT NOT NULL REFERENCES users_v2(id),
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, document_id, user_id, permission_type)
);

CREATE INDEX idx_doc_perm_tenant ON document_permissions_v2(tenant_id);
CREATE INDEX idx_doc_perm_document ON document_permissions_v2(document_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_doc_perm_user ON document_permissions_v2(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_doc_perm_role ON document_permissions_v2(role_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_doc_perm_granted_by ON document_permissions_v2(granted_by);
CREATE INDEX idx_doc_perm_type ON document_permissions_v2(permission_type);
CREATE INDEX idx_doc_perm_expires ON document_permissions_v2(expires_at)
    WHERE expires_at IS NOT NULL AND deleted_at IS NULL;

-- Covering index for permission checks
CREATE INDEX idx_doc_perm_check ON document_permissions_v2(document_id, user_id, permission_type)
    INCLUDE (expires_at)
    WHERE deleted_at IS NULL AND (expires_at IS NULL OR expires_at > NOW());

COMMENT ON TABLE document_permissions_v2 IS 'Fine-grained document access control';
COMMENT ON COLUMN document_permissions_v2.deleted_at IS 'Soft delete timestamp (NULL = active)';

-- ============================================================================
-- DOCUMENT_CHUNKS: Optimized chunk storage for RAG
-- ============================================================================
CREATE TABLE IF NOT EXISTS document_chunks_v2 (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    document_id BIGINT NOT NULL REFERENCES documents_v2(id) ON DELETE CASCADE,
    parent_chunk_id BIGINT REFERENCES document_chunks_v2(id) ON DELETE SET NULL,
    chunk_type VARCHAR(10) NOT NULL CHECK (chunk_type IN ('PARENT', 'CHILD')),
    content TEXT NOT NULL,
    position INTEGER NOT NULL,
    char_count INTEGER,
    token_count INTEGER,
    embedding_vector BYTEA,
    embedding_model VARCHAR(50),
    vector_dimensions INTEGER,
    embedding_hash VARCHAR(64),
    language VARCHAR(20),
    metadata JSONB DEFAULT '{}',
    access_count INTEGER DEFAULT 0,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Performance indexes for chunk queries
CREATE INDEX idx_chunks_tenant ON document_chunks_v2(tenant_id);
CREATE INDEX idx_chunks_document ON document_chunks_v2(document_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_chunks_parent ON document_chunks_v2(parent_chunk_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_chunks_type ON document_chunks_v2(chunk_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_chunks_language ON document_chunks_v2(language) WHERE deleted_at IS NULL;
CREATE INDEX idx_chunks_metadata ON document_chunks_v2 USING GIN(metadata);
CREATE INDEX idx_chunks_access_count ON document_chunks_v2(access_count DESC)
    WHERE deleted_at IS NULL;

-- Composite indexes for RAG queries
CREATE INDEX idx_chunks_document_type ON document_chunks_v2(document_id, chunk_type)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_chunks_parent_type ON document_chunks_v2(parent_chunk_id, chunk_type)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_chunks_tenant_created ON document_chunks_v2(tenant_id, created_at DESC)
    WHERE deleted_at IS NULL;

-- Partial index for embedding-ready chunks (vector search candidates)
CREATE INDEX idx_chunks_for_embedding ON document_chunks_v2(document_id, position)
    WHERE chunk_type = 'CHILD'
    AND deleted_at IS NULL
    AND embedding_vector IS NOT NULL;

COMMENT ON TABLE document_chunks_v2 IS 'Optimized chunk storage for RAG with multi-tenant support';
COMMENT ON COLUMN document_chunks_v2.embedding_hash IS 'Hash of embedding content for deduplication';
COMMENT ON COLUMN document_chunks_v2.token_count IS 'Approximate token count for LLM context calculation';
COMMENT ON COLUMN document_chunks_v2.access_count IS 'Chunk access counter for analytics';

-- ============================================================================
-- QA_HISTORY: Enhanced Q&A history with analytics
-- ============================================================================
CREATE TABLE IF NOT EXISTS qa_history_v2 (
    id BIGSERIAL PRIMARY KEY,
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
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_qa_tenant ON qa_history_v2(tenant_id);
CREATE INDEX idx_qa_user ON qa_history_v2(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_qa_session ON qa_history_v2(session_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_qa_feedback ON qa_history_v2(feedback) WHERE feedback IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_qa_bookmarked ON qa_history_v2(is_bookmarked) WHERE is_bookmarked = TRUE AND deleted_at IS NULL;
CREATE INDEX idx_qa_model ON qa_history_v2(model_used) WHERE deleted_at IS NULL;
CREATE INDEX idx_qa_response_time ON qa_history_v2(response_time_ms) WHERE deleted_at IS NULL;
CREATE INDEX idx_qa_created ON qa_history_v2(created_at DESC) WHERE deleted_at IS NULL;

-- Composite indexes for analytics queries
CREATE INDEX idx_qa_tenant_created ON qa_history_v2(tenant_id, created_at DESC)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_qa_user_created ON qa_history_v2(user_id, created_at DESC)
    WHERE deleted_at IS NULL;

-- Full-text search on questions
CREATE INDEX idx_qa_question_fulltext ON qa_history_v2 USING GIN(
    to_tsvector('simple', question)
) WHERE deleted_at IS NULL;

COMMENT ON TABLE qa_history_v2 IS 'Enhanced Q&A history with analytics support';
COMMENT ON COLUMN qa_history_v2.session_id IS 'Conversation session ID for context grouping';
COMMENT ON COLUMN qa_history_v2.chunks_accessed IS 'Track which chunks were used for answer generation';
COMMENT ON COLUMN qa_history_v2.tokens_consumed IS 'LLM token consumption for cost tracking';
COMMENT ON COLUMN qa_history_v2.response_time_ms IS 'Query response time in milliseconds';

-- ============================================================================
-- SEARCH_HISTORY: Enhanced search analytics
-- ============================================================================
CREATE TABLE IF NOT EXISTS search_history_v2 (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    user_id BIGINT NOT NULL REFERENCES users_v2(id),
    query TEXT NOT NULL,
    search_type VARCHAR(20) CHECK (search_type IN ('BM25', 'KNN', 'HYBRID')),
    result_count INTEGER,
    filters JSONB,
    has_result_click BOOLEAN DEFAULT FALSE,
    clicked_document_id BIGINT REFERENCES documents_v2(id),
    click_position INTEGER,
    results_shown INTEGER,
    response_time_ms INTEGER,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_search_tenant ON search_history_v2(tenant_id);
CREATE INDEX idx_search_user ON search_history_v2(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_search_type ON search_history_v2(search_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_search_clicks ON search_history_v2(has_result_click, clicked_document_id)
    WHERE has_result_click = TRUE AND deleted_at IS NULL;
CREATE INDEX idx_search_response_time ON search_history_v2(response_time_ms) WHERE deleted_at IS NULL;
CREATE INDEX idx_search_created ON search_history_v2(created_at DESC) WHERE deleted_at IS NULL;

-- Full-text search on queries for analytics
CREATE INDEX idx_search_query_fulltext ON search_history_v2 USING GIN(
    to_tsvector('simple', query)
) WHERE deleted_at IS NULL;

COMMENT ON TABLE search_history_v2 IS 'Enhanced search analytics with click tracking';
COMMENT ON COLUMN search_history_v2.clicked_document_id IS 'Track which documents users click on';
COMMENT ON COLUMN search_history_v2.click_position IS 'Position of clicked result (1-based)';

-- ============================================================================
-- CODE_REVIEWS: Enhanced code review tracking
-- ============================================================================
CREATE TABLE IF NOT EXISTS code_reviews_v2 (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    user_id BIGINT NOT NULL REFERENCES users_v2(id),
    project_id BIGINT,
    code_content TEXT NOT NULL,
    language VARCHAR(50) NOT NULL,
    file_name VARCHAR(255),
    file_path VARCHAR(500),
    total_issues INTEGER DEFAULT 0,
    critical_issues INTEGER DEFAULT 0,
    high_issues INTEGER DEFAULT 0,
    medium_issues INTEGER DEFAULT 0,
    low_issues INTEGER DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    visibility VARCHAR(20) DEFAULT 'PRIVATE'
        CHECK (visibility IN ('PRIVATE', 'TEAM', 'PUBLIC')),
    shared_team_id BIGINT,
    analysis_summary TEXT,
    overall_score INTEGER CHECK (overall_score >= 0 AND overall_score <= 100),
    metrics JSONB DEFAULT '{}',
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reviews_tenant ON code_reviews_v2(tenant_id);
CREATE INDEX idx_reviews_user ON code_reviews_v2(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_project ON code_reviews_v2(project_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_status ON code_reviews_v2(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_language ON code_reviews_v2(language) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_visibility ON code_reviews_v2(visibility) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_score ON code_reviews_v2(overall_score) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_metrics ON code_reviews_v2 USING GIN(metrics);
CREATE INDEX idx_reviews_created ON code_reviews_v2(created_at DESC) WHERE deleted_at IS NULL;

COMMENT ON TABLE code_reviews_v2 IS 'Enhanced code review tracking with analytics';

-- ============================================================================
-- CODE_ISSUES: Enhanced issue tracking with teaching support
-- ============================================================================
CREATE TABLE IF NOT EXISTS code_issues_v2 (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    review_id BIGINT NOT NULL REFERENCES code_reviews_v2(id) ON DELETE CASCADE,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO')),
    category VARCHAR(50),
    rule_id VARCHAR(100),
    title VARCHAR(255),
    description TEXT,
    code_snippet TEXT,
    line_number INTEGER,
    column_number INTEGER,
    end_line_number INTEGER,
    end_column_number INTEGER,
    suggestion TEXT,
    teaching_explanation TEXT,
    related_lesson_id BIGINT,
    agent_type VARCHAR(50),
    tool_name VARCHAR(100),
    confidence_score DECIMAL(3,2),
    is_resolved BOOLEAN DEFAULT FALSE,
    resolved_by BIGINT REFERENCES users_v2(id),
    resolved_at TIMESTAMPTZ,
    resolution_comment TEXT,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_issues_tenant ON code_issues_v2(tenant_id);
CREATE INDEX idx_issues_review ON code_issues_v2(review_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_issues_severity ON code_issues_v2(severity) WHERE deleted_at IS NULL;
CREATE INDEX idx_issues_category ON code_issues_v2(category) WHERE deleted_at IS NULL;
CREATE INDEX idx_issues_resolved ON code_issues_v2(is_resolved, resolved_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_issues_agent ON code_issues_v2(agent_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_issues_rule ON code_issues_v2(tool_name, rule_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_issues_created ON code_issues_v2(created_at DESC) WHERE deleted_at IS NULL;

COMMENT ON TABLE code_issues_v2 IS 'Enhanced issue tracking with teaching and resolution tracking';

-- ============================================================================
-- AUDIT_LOGS: Comprehensive audit trail
-- ============================================================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
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
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_tenant ON audit_logs(tenant_id);
CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_resource ON audit_logs(resource_type, resource_id);
CREATE INDEX idx_audit_created ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_request ON audit_logs(request_id);

-- Partition audit logs by month for better performance and easier archival
CREATE TABLE audit_logs_partition_template (
    LIKE audit_logs INCLUDING ALL
) PARTITION BY RANGE (created_at);

COMMENT ON TABLE audit_logs IS 'Comprehensive audit trail for all system operations';
COMMENT ON COLUMN audit_logs.action IS 'Action performed: CREATE, READ, UPDATE, DELETE, LOGIN, LOGOUT, etc.';
COMMENT ON COLUMN audit_logs.resource_type IS 'Resource type: user, document, role, etc.';

-- ============================================================================
-- Create functions for automatic updated_at timestamps
-- ============================================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers for updated_at
CREATE TRIGGER update_tenants_updated_at BEFORE UPDATE ON tenants
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users_v2
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_roles_updated_at BEFORE UPDATE ON roles_v2
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_documents_updated_at BEFORE UPDATE ON documents_v2
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_chunks_updated_at BEFORE UPDATE ON document_chunks_v2
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_reviews_updated_at BEFORE UPDATE ON code_reviews_v2
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- Create audit trigger function
-- ============================================================================
CREATE OR REPLACE FUNCTION audit_trigger_func()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO audit_logs (tenant_id, user_id, action, resource_type, resource_id, new_values)
        VALUES (
            COALESCE(NEW.tenant_id, 0),
            current_setting('app.current_user_id', true)::BIGINT,
            'CREATE',
            TG_TABLE_NAME,
            NEW.id,
            to_jsonb(NEW)
        );
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO audit_logs (tenant_id, user_id, action, resource_type, resource_id, old_values, new_values)
        VALUES (
            COALESCE(NEW.tenant_id, OLD.tenant_id, 0),
            current_setting('app.current_user_id', true)::BIGINT,
            'UPDATE',
            TG_TABLE_NAME,
            NEW.id,
            to_jsonb(OLD),
            to_jsonb(NEW)
        );
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO audit_logs (tenant_id, user_id, action, resource_type, resource_id, old_values)
        VALUES (
            COALESCE(OLD.tenant_id, 0),
            current_setting('app.current_user_id', true)::BIGINT,
            'DELETE',
            TG_TABLE_NAME,
            OLD.id,
            to_jsonb(OLD)
        );
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- Performance optimization: Materialized view for document analytics
-- ============================================================================
CREATE MATERIALIZED VIEW IF NOT EXISTS document_analytics_mv AS
SELECT
    d.tenant_id,
    d.uploaded_by,
    d.department,
    d.status,
    d.visibility,
    d.file_type,
    COUNT(*) as document_count,
    SUM(d.file_size) as total_file_size,
    AVG(d.file_size) as avg_file_size,
    SUM(d.access_count) as total_access_count,
    COUNT(DISTINCT d.tags) as unique_tags,
    MIN(d.created_at) as earliest_document,
    MAX(d.created_at) as latest_document
FROM documents_v2 d
WHERE d.deleted_at IS NULL
GROUP BY d.tenant_id, d.uploaded_by, d.department, d.status, d.visibility, d.file_type;

CREATE UNIQUE INDEX idx_document_analytics_unique ON document_analytics_mv(tenant_id, uploaded_by, department, status, visibility, file_type);
CREATE INDEX idx_document_analytics_tenant ON document_analytics_mv(tenant_id);

COMMENT ON MATERIALIZED VIEW document_analytics_mv IS 'Pre-aggregated document analytics for dashboard queries';

-- ============================================================================
-- Performance optimization: Materialized view for user activity
-- ============================================================================
CREATE MATERIALIZED VIEW IF NOT EXISTS user_activity_mv AS
SELECT
    u.tenant_id,
    u.id as user_id,
    u.department,
    COUNT(DISTINCT d.id) as document_count,
    COUNT(DISTINCT qa.id) as qa_count,
    COUNT(DISTINCT cr.id) as code_review_count,
    SUM(d.access_count) as total_document_access,
    AVG(d.file_size) as avg_document_size,
    MAX(u.last_login_at) as last_login,
    MAX(qa.created_at) as last_qa_activity,
    MAX(cr.created_at) as last_review_activity
FROM users_v2 u
LEFT JOIN documents_v2 d ON d.uploaded_by = u.id AND d.deleted_at IS NULL
LEFT JOIN qa_history_v2 qa ON qa.user_id = u.id AND qa.deleted_at IS NULL
LEFT JOIN code_reviews_v2 cr ON cr.user_id = u.id AND cr.deleted_at IS NULL
WHERE u.deleted_at IS NULL
GROUP BY u.tenant_id, u.id, u.department;

CREATE UNIQUE INDEX idx_user_activity_unique ON user_activity_mv(tenant_id, user_id);
CREATE INDEX idx_user_activity_tenant ON user_activity_mv(tenant_id);
CREATE INDEX idx_user_activity_department ON user_activity_mv(department);

COMMENT ON MATERIALIZED VIEW user_activity_mv IS 'Pre-aggregated user activity metrics for dashboard queries';

-- ============================================================================
-- Row Level Security: Enable RLS and create policies
-- ============================================================================
ALTER TABLE users_v2 ENABLE ROW LEVEL SECURITY;
ALTER TABLE documents_v2 ENABLE ROW LEVEL SECURITY;
ALTER TABLE document_chunks_v2 ENABLE ROW LEVEL SECURITY;
ALTER TABLE qa_history_v2 ENABLE ROW LEVEL SECURITY;
ALTER TABLE search_history_v2 ENABLE ROW_LEVEL SECURITY;

-- Policy: Users can only see their own tenant's data
CREATE POLICY users_tenant_policy ON users_v2
    USING (tenant_id = current_setting('app.current_tenant_id')::BIGINT);

CREATE POLICY documents_tenant_policy ON documents_v2
    USING (tenant_id = current_setting('app.current_tenant_id')::BIGINT);

CREATE POLICY chunks_tenant_policy ON document_chunks_v2
    USING (tenant_id = current_setting('app.current_tenant_id')::BIGINT);

CREATE POLICY qa_tenant_policy ON qa_history_v2
    USING (tenant_id = current_setting('app.current_tenant_id')::BIGINT);

CREATE POLICY search_tenant_policy ON search_history_v2
    USING (tenant_id = current_setting('app.current_tenant_id')::BIGINT);

-- ============================================================================
-- Create views for common queries to simplify application code
-- ============================================================================
CREATE OR REPLACE VIEW active_users AS
SELECT * FROM users_v2 WHERE is_active = TRUE AND deleted_at IS NULL;

CREATE OR REPLACE VIEW active_documents AS
SELECT * FROM documents_v2 WHERE deleted_at IS NULL AND status != 'ARCHIVED';

CREATE OR REPLACE VIEW indexed_documents AS
SELECT * FROM documents_v2 WHERE status = 'INDEXED' AND deleted_at IS NULL;

CREATE OR REPLACE VIEW user_documents_with_permissions AS
SELECT
    d.*,
    CASE
        WHEN d.uploaded_by = current_setting('app.current_user_id')::BIGINT THEN 'ADMIN'
        WHEN dp.permission_type IS NOT NULL THEN dp.permission_type
        WHEN d.is_public = TRUE THEN 'READ'
        ELSE NULL
    END as effective_permission
FROM documents_v2 d
LEFT JOIN document_permissions_v2 dp ON dp.document_id = d.id
    AND dp.user_id = current_setting('app.current_user_id')::BIGINT
    AND dp.deleted_at IS NULL
WHERE d.deleted_at IS NULL;

COMMENT ON VIEW active_users IS 'View of active (non-deleted) users';
COMMENT ON VIEW active_documents IS 'View of active (non-deleted, non-archived) documents';
COMMENT ON VIEW indexed_documents IS 'View of documents ready for search';
COMMENT ON VIEW user_documents_with_permissions IS 'View of documents with effective user permissions';

-- ============================================================================
-- Create helper functions for common operations
-- ============================================================================
CREATE OR REPLACE FUNCTION get_document_permissions(p_document_id BIGINT, p_user_id BIGINT)
RETURNS TABLE(permission_type VARCHAR(20)) AS $$
BEGIN
    RETURN QUERY
    SELECT dp.permission_type
    FROM document_permissions_v2 dp
    WHERE dp.document_id = p_document_id
    AND dp.user_id = p_user_id
    AND dp.deleted_at IS NULL
    AND (dp.expires_at IS NULL OR dp.expires_at > NOW());
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION get_user_effective_permissions(p_user_id BIGINT)
RETURNS TABLE(resource VARCHAR(50), action VARCHAR(50)) AS $$
BEGIN
    RETURN QUERY
    SELECT DISTINCT
        p.resource,
        p.action
    FROM permissions p
    JOIN roles_v2 r ON p.action = ANY(
        SELECT jsonb_array_elements_text(r.permissions->'permissions')
    )
    JOIN user_roles_v2 ur ON ur.role_id = r.id
    WHERE ur.user_id = p_user_id
    AND (ur.expires_at IS NULL OR ur.expires_at > NOW());
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- Grant permissions (adjust as needed for your application user)
-- ============================================================================
GRANT USAGE ON SCHEMA public TO kb_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO kb_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO kb_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO kb_user;

-- ============================================================================
-- Add helpful comments for database administrators
-- ============================================================================
COMMENT ON DATABASE knowledge_base IS 'Enterprise Knowledge Base System - Multi-tenant Architecture';

-- ============================================================================
-- PERFORMANCE OPTIMIZATION NOTES
-- ============================================================================
--
-- INDEX MAINTENANCE:
-- - Run REINDEX CONCURRENTLY periodically to maintain index performance
-- - Monitor index usage with pg_stat_user_indexes
-- - Remove unused indexes to reduce write overhead
--
-- VACUUM AND ANALYZE:
-- - Autovacuum should be enabled and tuned for your workload
-- - For high-write tables, consider more aggressive autovacuum settings
-- - Run ANALYZE after major data imports
--
-- CONNECTION POOLING:
-- - Use PgBouncer for connection pooling in production
-- - Recommended: transaction pooling mode for high-concurrency workloads
--
-- MONITORING QUERIES:
-- - Enable pg_stat_statements for query performance monitoring
-- - Set log_min_duration_statement = 1000 to log slow queries
-- - Use EXPLAIN ANALYZE for query optimization
--
-- PARTITIONING STRATEGY (for future scale):
-- - Consider partitioning audit_logs by month (already prepared)
-- - Consider partitioning qa_history by month for high-volume systems
-- - Consider partitioning document_chunks by document_id range for huge datasets
--
-- CACHING STRATEGY:
-- - Cache user permissions in Redis with 5-minute TTL
-- - Cache document metadata in Redis with 10-minute TTL
-- - Cache frequently accessed chunks in Redis with 1-hour TTL
-- - Materialized views should be refreshed every 5-15 minutes
--
-- ELASTICSEARCH SYNC:
-- - Only sync indexed documents to ES
-- - Use document_chunks_v2 for ES indexing (not parent chunks)
-- - Sync on document status change to INDEXED
-- - Use tenant_id in ES for tenant isolation
-- - Update ES document access_count on each access
-- ============================================================================
