-- ========================================
-- 企业知识库 MVP - V4: Token管理
-- JWT刷新令牌 + 密码重置
-- ========================================

-- ========================================
-- 刷新令牌表
-- ========================================
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMPTZ,
    device_info VARCHAR(255),
    ip_address INET,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at) WHERE revoked = FALSE;

COMMENT ON TABLE refresh_tokens IS 'JWT刷新令牌表';

-- ========================================
-- 密码重置令牌表
-- ========================================
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    used_at TIMESTAMPTZ,
    ip_address INET,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_expires ON password_reset_tokens(expires_at) WHERE used = FALSE;

COMMENT ON TABLE password_reset_tokens IS '密码重置令牌表';

-- ========================================
-- 清理过期Token的函数
-- ========================================
CREATE OR REPLACE FUNCTION cleanup_expired_tokens()
RETURNS INTEGER AS $$
DECLARE
    v_cleaned_count INTEGER := 0;
BEGIN
    -- 清理过期刷新令牌（保留7天用于审计）
    DELETE FROM refresh_tokens
    WHERE (revoked = TRUE OR expires_at < NOW() - INTERVAL '7 days');

    GET DIAGNOSTICS v_cleaned_count = ROW_COUNT;

    -- 清理过期密码重置令牌
    DELETE FROM password_reset_tokens
    WHERE (used = TRUE OR expires_at < NOW() - INTERVAL '30 days');

    RETURN v_cleaned_count;
END;
$$ LANGUAGE plpgsql;
