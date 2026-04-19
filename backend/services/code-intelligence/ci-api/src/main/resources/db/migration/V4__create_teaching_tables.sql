-- ================================================================
-- Multi-Agent AI Code Review & Teaching System - Database Migration
-- Version: V4
-- Description: Create tables for teaching and learning features
-- ================================================================

-- ================================================================
-- learning_content: Lessons, tutorials, code examples, and quizzes
-- ================================================================
CREATE TABLE IF NOT EXISTS learning_content (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    content_type VARCHAR(20) NOT NULL CHECK (content_type IN ('LESSON', 'TUTORIAL', 'CODE_EXAMPLE', 'QUIZ')),
    difficulty_level VARCHAR(20) NOT NULL CHECK (difficulty_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    content TEXT NOT NULL,
    language VARCHAR(50),
    category VARCHAR(100),
    tags VARCHAR(500)[],
    code_examples TEXT[],
    prerequisites BIGINT[],
    estimated_minutes INTEGER,
    is_published BOOLEAN DEFAULT FALSE,
    creator_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_learning_content_type ON learning_content(content_type);
CREATE INDEX idx_learning_content_difficulty ON learning_content(difficulty_level);
CREATE INDEX idx_learning_content_language ON learning_content(language);
CREATE INDEX idx_learning_content_category ON learning_content(category);
CREATE INDEX idx_learning_content_creator ON learning_content(creator_id);
CREATE INDEX idx_learning_content_published ON learning_content(is_published);

-- ================================================================
-- user_progress: Track user learning progress
-- ================================================================
CREATE TABLE IF NOT EXISTS user_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content_id BIGINT NOT NULL REFERENCES learning_content(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED')),
    progress_percent INTEGER DEFAULT 0 CHECK (progress_percent >= 0 AND progress_percent <= 100),
    current_section INTEGER DEFAULT 1,
    score INTEGER,
    max_score INTEGER,
    time_spent_minutes INTEGER DEFAULT 0,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    last_accessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, content_id)
);

CREATE INDEX idx_user_progress_user ON user_progress(user_id);
CREATE INDEX idx_user_progress_content ON user_progress(content_id);
CREATE INDEX idx_user_progress_status ON user_progress(status);
CREATE INDEX idx_user_progress_completed ON user_progress(completed_at) WHERE completed_at IS NOT NULL;

-- ================================================================
-- skill_profiles: User skill levels by language and category
-- ================================================================
CREATE TABLE IF NOT EXISTS skill_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    language VARCHAR(50) NOT NULL,
    category VARCHAR(100) NOT NULL,
    skill_level INTEGER NOT NULL CHECK (skill_level >= 0 AND skill_level <= 100),
    exercises_completed INTEGER DEFAULT 0,
    reviews_completed INTEGER DEFAULT 0,
    lessons_completed INTEGER DEFAULT 0,
    total_xp INTEGER DEFAULT 0,
    last_assessed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, language, category)
);

CREATE INDEX idx_skill_profiles_user ON skill_profiles(user_id);
CREATE INDEX idx_skill_profiles_language ON skill_profiles(language);
CREATE INDEX idx_skill_profiles_category ON skill_profiles(category);
CREATE INDEX idx_skill_profiles_level ON skill_profiles(skill_level);

-- ================================================================
-- exercises: Practice problems and coding exercises
-- ================================================================
CREATE TABLE IF NOT EXISTS exercises (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    exercise_type VARCHAR(20) NOT NULL CHECK (exercise_type IN ('CODE_REVIEW', 'FIX_BUG', 'WRITE_CODE')),
    difficulty_level VARCHAR(20) NOT NULL CHECK (difficulty_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    language VARCHAR(50) NOT NULL,
    category VARCHAR(100),
    starter_code TEXT,
    solution_code TEXT,
    test_cases JSONB,
    hints TEXT[],
    requirements JSONB,
    estimated_minutes INTEGER,
    max_score INTEGER DEFAULT 100,
    is_published BOOLEAN DEFAULT FALSE,
    creator_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_exercises_type ON exercises(exercise_type);
CREATE INDEX idx_exercises_difficulty ON exercises(difficulty_level);
CREATE INDEX idx_exercises_language ON exercises(language);
CREATE INDEX idx_exercises_category ON exercises(category);
CREATE INDEX idx_exercises_creator ON exercises(creator_id);
CREATE INDEX idx_exercises_published ON exercises(is_published);

-- ================================================================
-- exercise_attempts: User exercise submissions
-- ================================================================
CREATE TABLE IF NOT EXISTS exercise_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    exercise_id BIGINT NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    submitted_code TEXT NOT NULL,
    score INTEGER,
    max_score INTEGER,
    passed BOOLEAN,
    test_results JSONB,
    feedback TEXT,
    time_spent_seconds INTEGER,
    hints_used INTEGER DEFAULT 0,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_exercise_attempts_user ON exercise_attempts(user_id);
CREATE INDEX idx_exercise_attempts_exercise ON exercise_attempts(exercise_id);
CREATE INDEX idx_exercise_attempts_submitted ON exercise_attempts(submitted_at);

-- ================================================================
-- achievements: Gamification achievements
-- ================================================================
CREATE TABLE IF NOT EXISTS achievements (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    icon_url VARCHAR(500),
    category VARCHAR(50),
    requirements JSONB,
    xp_reward INTEGER DEFAULT 0,
    badge_color VARCHAR(20) DEFAULT '#3B82F6',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_achievements_code ON achievements(code);
CREATE INDEX idx_achievements_category ON achievements(category);
CREATE INDEX idx_achievements_active ON achievements(is_active);

-- ================================================================
-- user_achievements: User earned achievements
-- ================================================================
CREATE TABLE IF NOT EXISTS user_achievements (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    achievement_id BIGINT NOT NULL REFERENCES achievements(id) ON DELETE CASCADE,
    earned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    progress_data JSONB,
    UNIQUE(user_id, achievement_id)
);

CREATE INDEX idx_user_achievements_user ON user_achievements(user_id);
CREATE INDEX idx_user_achievements_achievement ON user_achievements(achievement_id);
CREATE INDEX idx_user_achievements_earned ON user_achievements(earned_at);

-- ================================================================
-- Extend code_issues table with teaching fields
-- ================================================================
ALTER TABLE code_issues
ADD COLUMN IF NOT EXISTS teaching_explanation TEXT,
ADD COLUMN IF NOT EXISTS related_lesson_id BIGINT REFERENCES learning_content(id);

CREATE INDEX idx_code_issues_related_lesson ON code_issues(related_lesson_id) WHERE related_lesson_id IS NOT NULL;

-- ================================================================
-- Insert initial seed achievements
-- ================================================================
INSERT INTO achievements (code, title, description, category, requirements, xp_reward, badge_color) VALUES
('first_review', 'Code Reviewer', 'Complete your first code review', 'review', '{"type": "count", "target": 1, "field": "reviews_completed"}', 10, '#10B981'),
('review_novice', 'Review Novice', 'Complete 10 code reviews', 'review', '{"type": "count", "target": 10, "field": "reviews_completed"}', 50, '#3B82F6'),
('review_expert', 'Review Expert', 'Complete 100 code reviews', 'review', '{"type": "count", "target": 100, "field": "reviews_completed"}', 200, '#8B5CF6'),
('bug_hunter', 'Bug Hunter', 'Fix 10 bugs in exercises', 'exercise', '{"type": "count", "target": 10, "field": "bugs_fixed"}', 75, '#F59E0B'),
('lesson_learner', 'Lesson Learner', 'Complete your first lesson', 'learning', '{"type": "count", "target": 1, "field": "lessons_completed"}', 15, '#EC4899'),
('knowledge_seeker', 'Knowledge Seeker', 'Complete 10 lessons', 'learning', '{"type": "count", "target": 10, "field": "lessons_completed"}', 100, '#6366F1'),
('skill_builder', 'Skill Builder', 'Reach skill level 50 in any category', 'skill', '{"type": "skill_level", "target": 50}', 150, '#14B8A6'),
('master_coder', 'Master Coder', 'Reach skill level 90 in any category', 'skill', '{"type": "skill_level", "target": 90}', 500, '#F43F5E'),
('streak_week', 'Week Warrior', 'Maintain a 7-day learning streak', 'streak', '{"type": "streak", "target": 7}', 50, '#A78BFA'),
('streak_month', 'Month Master', 'Maintain a 30-day learning streak', 'streak', '{"type": "streak", "target": 30}', 200, '#F472B6')
ON CONFLICT (code) DO NOTHING;
