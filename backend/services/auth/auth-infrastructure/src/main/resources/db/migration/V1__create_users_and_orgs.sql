-- ========================================
-- 企业知识库 MVP - V1: 用户和组织标签
-- 极简RBAC + 组织隔离
-- ========================================

-- ========================================
-- 组织标签表（树形结构）
-- ========================================
CREATE TABLE organization_tags (
    tag_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    parent_tag_id VARCHAR(50) REFERENCES organization_tags(tag_id) ON DELETE SET NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_org_tags_parent ON organization_tags(parent_tag_id);
CREATE INDEX idx_org_tags_creator ON organization_tags(created_by);

COMMENT ON TABLE organization_tags IS '组织标签表 - 树形结构用于数据隔离';

-- ========================================
-- 用户表（极简RBAC）
-- ========================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'USER'
        CHECK (role IN ('USER', 'ADMIN', 'TEACHER')),
    org_tags VARCHAR(255),  -- 逗号分隔：'dept1,dept2'
    primary_org_tag VARCHAR(50) REFERENCES organization_tags(tag_id),
    avatar_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_org_tag ON users(primary_org_tag) WHERE primary_org_tag IS NOT NULL;

COMMENT ON TABLE users IS '用户表 - 极简RBAC设计';
COMMENT ON COLUMN users.role IS '用户角色: USER, ADMIN, TEACHER';
COMMENT ON COLUMN users.org_tags IS '所属组织标签（逗号分隔）';

-- ========================================
-- 触发器：自动更新 updated_at
-- ========================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_org_tags_updated_at
    BEFORE UPDATE ON organization_tags
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ========================================
-- 默认数据
-- ========================================

-- 默认组织标签
INSERT INTO organization_tags (tag_id, name, description, created_by) VALUES
    ('default', '默认组织', '系统默认组织', 1),
    ('engineering', '研发部', '工程研发部门', 1),
    ('product', '产品部', '产品设计部门', 1);

-- 默认管理员 (密码: admin123)
INSERT INTO users (username, email, password_hash, full_name, role, primary_org_tag)
VALUES (
    'admin',
    'admin@think-platform.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    '系统管理员',
    'ADMIN',
    'default'
);
