# Coolify deployment — ATA RAG Backend

## Overview

Deploy this API as a **Dockerfile** resource on Coolify. Use an **external PostgreSQL with pgvector** (Coolify Postgres, Neon, Supabase, or Mikrus). Do **not** rely on host port binds for production Traefik routing.

## 1. PostgreSQL (pgvector)

Create a Postgres instance that supports the `vector` extension (image `pgvector/pgvector:pg16` or managed equivalent).

Note the **internal hostname** on the Coolify network.

## 2. Backend service

| Setting | Value |
|---------|--------|
| **Build pack** | Dockerfile |
| **Ports Exposes** | `8080` |
| **Port Mappings** | leave empty (Traefik) |
| **Health check path** | `/health` |
| **Branch** | `main` |

Remove SPA-style `try_files` / Caddy SPA presets — this is an API.

## 3. Environment variables

| Variable | Example / notes |
|----------|-----------------|
| `SPRING_PROFILES_ACTIVE` | `docker` |
| `POSTGRES_HOST` | Coolify DB internal hostname |
| `POSTGRES_PORT` | `5432` |
| `POSTGRES_DB` | `ata_rag` |
| `POSTGRES_USER` | DB user |
| `POSTGRES_PASSWORD` | secret |
| `CORS_ALLOWED_ORIGINS` | FE URL, e.g. `https://ata-rag-fe.example.com` |
| `CRAWL_BASE_URL` | `https://akademiata.pl` |
| `PRICING_API_URL` | Google Apps Script JSON URL for tuition calculator |
| `PRICING_CITATION_URL` | `https://akademiata.pl/kalkulator-czesnego/` |
| `CRAWL_MAX_PAGES` | `0` = unlimited; use small values for smoke crawls |
| `INGEST_SCHEDULER_ENABLED` | `true`/`false` — nightly website+pricing sync at 03:00 |
| `OPENAI_API_KEY` | secret |
| `OPENAI_MODEL` | `gpt-4.1-mini` |
| `EMBEDDING_MODEL` | `text-embedding-3-small` |
| `CONFIDENCE_THRESHOLD` | `0.55` |
| `BASIC_AUTH_USER` | admin basic auth (when admin API lands) |
| `BASIC_AUTH_PASSWORD` | secret |

Mark secrets as **encrypted** in Coolify.

## 4. SSE / Traefik notes

When chat SSE is enabled (`be/rag-chat-api`):

- Disable response buffering on the reverse proxy for `/api/chat`
- Keep idle timeouts high enough for long streams

## 5. Verify

```bash
curl -sS "https://<your-be-host>/health"
curl -sS "https://<your-be-host>/actuator/health"
```

Expected: `{"status":"ok","service":"ata-rag-chat-be"}`.

Chat and admin endpoints return **501** until their feature branches are merged.
