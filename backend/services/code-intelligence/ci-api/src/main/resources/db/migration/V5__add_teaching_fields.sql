-- Add teaching fields to code_issues table
ALTER TABLE code_issues
ADD COLUMN IF NOT EXISTS teaching_explanation TEXT,
ADD COLUMN IF NOT EXISTS related_lesson_id BIGINT;

-- Add index for related_lesson_id
CREATE INDEX IF NOT EXISTS idx_code_issues_related_lesson_id ON code_issues(related_lesson_id);

-- Add comment
COMMENT ON COLUMN code_issues.teaching_explanation IS 'Educational explanation for the code issue';
COMMENT ON COLUMN code_issues.related_lesson_id IS 'Reference to related learning content lesson';
