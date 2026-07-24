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
| `be/ingestion-pipeline` | Website crawl + Apps Script pricing ingest |
| `be/rag-chat-api` | SSE RAG chat + citations |
| `be/admin-observability` | Admin auth + metrics hardening (**this branch**) |

### Ingest endpoints

```bash
# limited crawl via CRAWL_MAX_PAGES (see .env.example)
curl -X POST http://localhost:8080/api/admin/sync
curl -X POST http://localhost:8080/api/admin/prices/sync
curl http://localhost:8080/api/admin/summary
```

Pricing data comes from `PRICING_API_URL` (Google Apps Script used by
https://akademiata.pl/kalkulator-czesnego/). Without `OPENAI_API_KEY`, ingest uses
deterministic hash embeddings so the pipeline still runs locally.

### RAG chat endpoint

The chat endpoint emits `sources`, one or more `token`, and `done` SSE events:

```bash
curl -N -H 'Content-Type: application/json' \
  -d '{"question":"What is the tuition for Computer Science in Warsaw?","top_k":5}' \
  http://localhost:8080/api/chat
```

Use `OPENAI_API_KEY` for OpenAI-compatible embeddings and chat generation, or
enable Vertex AI with `VERTEX_AI_ENABLED=true` and the documented GCP variables.
Vertex takes precedence for generation. Without either provider, retrieval still
works and the API returns an extractive grounded answer.

### Admin and observability

Set both `BASIC_AUTH_USER` and `BASIC_AUTH_PASSWORD`; otherwise every
`/api/admin/**` request remains locked. Admin sync endpoints return HTTP 202 and
run one ingestion job at a time:

```bash
curl -u admin:change-me http://localhost:8080/api/admin/summary
curl -u admin:change-me http://localhost:8080/api/admin/questions
curl -u admin:change-me -X POST http://localhost:8080/api/admin/prices/sync
curl http://localhost:8080/actuator/prometheus
```

Prometheus exports chat request, latency, confidence, LLM token, and ingestion
job metrics under the `rag_*` prefix.

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
