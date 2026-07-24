-- Bootstrap schema for ATA RAG.
-- Full pages/chunks/chat_queries/crawl_runs modeling lands in branch be/ingestion-pipeline.

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS schema_bootstrap (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    note TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO schema_bootstrap (note)
SELECT 'ata-rag bootstrap ready'
WHERE NOT EXISTS (SELECT 1 FROM schema_bootstrap);
