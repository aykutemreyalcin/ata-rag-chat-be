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
| `be/admin-observability` | Admin APIs + metrics hardening |

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
