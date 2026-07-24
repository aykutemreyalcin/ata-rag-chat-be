CREATE TABLE IF NOT EXISTS chat_queries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question TEXT NOT NULL,
    answer TEXT,
    answered BOOLEAN NOT NULL DEFAULT FALSE,
    confidence DOUBLE PRECISION,
    retrieval_score DOUBLE PRECISION,
    source_count INTEGER NOT NULL DEFAULT 0,
    latency_ms BIGINT,
    model TEXT,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_queries_created_at
    ON chat_queries (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_queries_answered
    ON chat_queries (answered);

CREATE INDEX IF NOT EXISTS idx_chunks_embedding_cosine
    ON chunks USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 20);
