# ATA RAG Chat — Backend

Spring Boot API for the AkademiaTA (`akademiata.pl`) RAG chatbot.

## Stack

- Java 21 + Spring Boot 3.5
- PostgreSQL + pgvector (Flyway)
- Actuator + Prometheus metrics
- Coolify-ready Docker image

## Quick start

```bash
cp .env.example .env
# Start pgvector Postgres (optional local compose)
docker compose up -d postgres
./mvnw spring-boot:run
```

Health:

```bash
curl -s http://localhost:8080/health
```

## Feature branches (planned)

| Branch | Scope |
|--------|--------|
| `be/ingestion-pipeline` | Website crawl + Apps Script pricing ingest (**this branch**) |
| `be/rag-chat-api` | SSE RAG chat + citations |
| `be/admin-observability` | Admin auth + metrics hardening |

### Ingest endpoints (this branch)

```bash
# limited crawl via CRAWL_MAX_PAGES (see .env.example)
curl -X POST http://localhost:8080/api/admin/sync
curl -X POST http://localhost:8080/api/admin/prices/sync
curl http://localhost:8080/api/admin/summary
```

Pricing data comes from `PRICING_API_URL` (Google Apps Script used by
https://akademiata.pl/kalkulator-czesnego/). Without `OPENAI_API_KEY`, ingest uses
deterministic hash embeddings so the pipeline still runs locally.

See [jira_tasks.csv](./jira_tasks.csv) for Jira import and [docs/openapi.yaml](./docs/openapi.yaml) for the API contract.

## Docker / Coolify

```bash
./scripts/docker-up.sh
```

Production notes: [docs/COOLIFY_DEPLOY.md](./docs/COOLIFY_DEPLOY.md)

## Tests

```bash
./mvnw test
```
