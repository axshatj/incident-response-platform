# Autonomous Incident Response Platform

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

## Status — Phase 1

Delivered:

- Incident Service (Spring Boot 3, Java 21)
- PostgreSQL 16 via Docker Compose
- Incident lifecycle state machine + immutable audit trail
- REST API (`/api/incidents/*`)
- React dashboard (Vite + TS + Tailwind) with list, create, detail, timeline, lifecycle actions

Next: Phase 2 — OpenTelemetry, Prometheus, Grafana.

## Quickstart (Phase 1)

Prerequisites: Docker Desktop, Node 20+, Java 21+ (only if building without Docker).

```bash
# 1. Start Postgres + incident-service
docker compose -f infra/docker-compose.yml up --build

# 2. In another terminal, start the UI
cd ui
npm install
npm run dev
```

Open http://localhost:5173. The Vite dev server proxies `/api/*` to the incident-service on port 8080.

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
# Start only Postgres
docker compose -f infra/docker-compose.yml up postgres -d

# Run the service directly
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
    └── docker-compose.yml          # Local Postgres + incident-service
```

## Tech Stack

Java 21 · Spring Boot 3 · Spring AI · PostgreSQL + pgvector · Kafka · Redis · OpenSearch · OpenTelemetry · Kubernetes · Prometheus · Grafana · React (Vite + TS + Tailwind)

See the [build order](SKILL.md#build-order-do-not-skip-ahead) in `SKILL.md` for what each phase delivers.
