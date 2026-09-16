# Autonomous Incident Response Platform

[![CI](https://github.com/axshatj/incident-response-platform/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/axshatj/incident-response-platform/actions/workflows/ci.yml)

Production-style AI platform that detects, investigates, diagnoses, and safely remediates distributed-system incidents using event-driven Java services, observability data, RAG, MCP, and policy-controlled AI agents.

## Documentation

| Document | Purpose |
|----------|---------|
| [SKILL.md](SKILL.md) | Cursor Agent Skill — directives, guardrails, build order, MVP |
| [reference/architecture.md](reference/architecture.md) | System architecture, telemetry, Kafka, data model, infra |
| [reference/ai-agents.md](reference/ai-agents.md) | Agent schemas, MCP tools, RAG, evaluation |
| [reference/safety-and-security.md](reference/safety-and-security.md) | Remediation policy, executor contract, security model |

## Core Principle

**AI reasons and investigates. Deterministic software controls execution.**

The LLM never executes shell, `kubectl`, SQL, or cloud API calls directly.

## Status — Phase 2

Delivered:

- Incident Service (Spring Boot 3, Java 21) with immutable state-machine + audit trail
- PostgreSQL 16 via Docker Compose
- REST API (`/api/incidents/*`) and React dashboard (Vite + TS + Tailwind)
- **OpenTelemetry instrumentation** (Micrometer Tracing → OTLP) with trace/span propagation
- **JSON structured logs** with `traceId` / `spanId` correlated to Jaeger
- **Custom domain metrics**: `irp_incident_opened_total`, `irp_incident_transitions_total`, `irp_incident_illegal_transitions_total`
- **Observability stack**: OTel Collector, Prometheus, Jaeger, Grafana with pre-provisioned dashboard

Next: Phase 3 — Kafka event-driven processing.

## Quickstart

Prerequisites: Docker Desktop, Node 20+, Java 21+ (only if building without Docker).

```bash
# 1. Start the whole stack (Postgres, incident-service, OTel Collector, Prometheus, Jaeger, Grafana)
docker compose -f infra/docker-compose.yml up --build

# 2. In another terminal, start the UI
cd ui
npm install
npm run dev
```

Open http://localhost:5173. The Vite dev server proxies `/api/*` to the incident-service on port 8080.

### Observability URLs

| Tool | URL | What to look at |
|------|-----|-----------------|
| Grafana | http://localhost:3000 | IRP → **Incident Service — Overview** dashboard (anonymous Viewer, or `admin`/`admin`) |
| Prometheus | http://localhost:9090 | Query `irp_incident_transitions_total` or `http_server_requests_seconds_count` |
| Jaeger | http://localhost:16686 | Service = `incident-service`, look for `incident.create` and `incident.transition` spans |
| Actuator | http://localhost:8080/actuator/prometheus | Raw Micrometer output |

### Generate traffic to light up the dashboard

```bash
# Loop: create an incident, then walk it partway to trigger both a legal and an illegal transition
for i in {1..10}; do
  ID=$(curl -sS -X POST http://localhost:8080/api/incidents \
    -H "Content-Type: application/json" \
    -d '{"service":"payment-service","title":"DB pool saturated","severity":"SEV2","environment":"prod"}' \
    | jq -r .id)
  curl -sS -X POST "http://localhost:8080/api/incidents/$ID/acknowledge" \
    -H "Content-Type: application/json" -d '{"actor":"loadgen"}' >/dev/null
  # Illegal jump (TRIAGING -> RESOLVED) -> increments irp_incident_illegal_transitions_total
  curl -sS -X POST "http://localhost:8080/api/incidents/$ID/resolve" \
    -H "Content-Type: application/json" -d '{"actor":"loadgen"}' >/dev/null
done
```

### Verify the backend directly

```bash
# Create an incident
curl -X POST http://localhost:8080/api/incidents \
  -H "Content-Type: application/json" \
  -d '{
        "service": "payment-service",
        "title": "DB connection exhaustion",
        "description": "Pool saturated after v42 deploy",
        "severity": "SEV2",
        "environment": "prod"
      }'

# List incidents
curl http://localhost:8080/api/incidents

# Acknowledge (DETECTED -> TRIAGING)
curl -X POST http://localhost:8080/api/incidents/<id>/acknowledge \
  -H "Content-Type: application/json" -d '{"actor":"you"}'
```

### Local dev without Docker

```bash
# Start only Postgres (traces will silently fail to export - that's fine)
docker compose -f infra/docker-compose.yml up postgres -d

# Run the service directly (uses the default profile: plain console logs)
export JAVA_HOME=/path/to/jdk-21   # Windows: $env:JAVA_HOME = "..."
./mvnw -pl services/incident-service spring-boot:run
```

## Repository Layout

```
incident-response-platform/
├── SKILL.md, reference/            # Engineering skill and design references
├── pom.xml                         # Parent Maven multi-module
├── mvnw, mvnw.cmd, .mvn/           # Maven wrapper
├── services/
│   └── incident-service/           # Spring Boot service (Phase 1)
├── ui/                             # React dashboard (Vite + TS + Tailwind)
└── infra/
    ├── docker-compose.yml          # Full local stack
    ├── otel/                       # OTel Collector config
    ├── prometheus/                 # Prometheus scrape config
    └── grafana/                    # Datasource + dashboard provisioning
```

## Tech Stack

Java 21 · Spring Boot 3 · Spring AI · PostgreSQL + pgvector · Kafka · Redis · OpenSearch · OpenTelemetry · Kubernetes · Prometheus · Grafana · React (Vite + TS + Tailwind)

See the [build order](SKILL.md#build-order-do-not-skip-ahead) in `SKILL.md` for what each phase delivers.
