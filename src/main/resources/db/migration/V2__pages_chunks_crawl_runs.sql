-- Replace bootstrap placeholder with real ingest schema.

DROP TABLE IF EXISTS schema_bootstrap;

CREATE TABLE IF NOT EXISTS pages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    url TEXT NOT NULL UNIQUE,
    title TEXT,
    source_type VARCHAR(16) NOT NULL,
    language VARCHAR(8),
    content_hash TEXT NOT NULL,
    last_modified TIMESTAMPTZ,
    last_crawled_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    http_status INTEGER,
    error_message TEXT,
    consecutive_miss_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pages_status ON pages (status);
CREATE INDEX IF NOT EXISTS idx_pages_source_type ON pages (source_type);
CREATE INDEX IF NOT EXISTS idx_pages_content_hash ON pages (content_hash);

CREATE TABLE IF NOT EXISTS chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    page_id UUID NOT NULL REFERENCES pages (id) ON DELETE CASCADE,
    document_id TEXT NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    content_hash TEXT NOT NULL,
    section TEXT,
    token_count INTEGER NOT NULL,
    language VARCHAR(8),
    url TEXT NOT NULL,
    title TEXT,
    source_type VARCHAR(16) NOT NULL,
    last_modified TIMESTAMPTZ,
    embedding vector(1536) NOT NULL,
    embedding_model TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chunks_page_id ON chunks (page_id);
CREATE INDEX IF NOT EXISTS idx_chunks_page_hash ON chunks (page_id, content_hash);
CREATE INDEX IF NOT EXISTS idx_chunks_source_type ON chunks (source_type);

CREATE TABLE IF NOT EXISTS crawl_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_type VARCHAR(16) NOT NULL DEFAULT 'website',
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    status VARCHAR(16) NOT NULL,
    pages_discovered INTEGER NOT NULL DEFAULT 0,
    pages_updated INTEGER NOT NULL DEFAULT 0,
    pages_failed INTEGER NOT NULL DEFAULT 0,
    pages_removed INTEGER NOT NULL DEFAULT 0,
    error_summary TEXT
);

CREATE INDEX IF NOT EXISTS idx_crawl_runs_started_at ON crawl_runs (started_at DESC);
