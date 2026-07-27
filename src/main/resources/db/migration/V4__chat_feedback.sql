ALTER TABLE chat_queries
    ADD COLUMN IF NOT EXISTS helpful BOOLEAN,
    ADD COLUMN IF NOT EXISTS feedback_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_chat_queries_helpful
    ON chat_queries (helpful)
    WHERE helpful IS NOT NULL;
