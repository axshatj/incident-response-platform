# Architecture Reference

Deep specs for the platform. The directives in `SKILL.md` take precedence; this file expands the detail.

## System Architecture

```text
                          ┌────────────────────┐
                          │ React / Next.js UI │
                          └─────────┬──────────┘
                                    │
                          ┌─────────▼──────────┐
                          │ API Gateway / BFF  │
                          │ Spring Boot        │
                          └─────────┬──────────┘
                                    │
        ┌───────────────────────────┼────────────────────────────┐
        │                           │                            │
 ┌──────▼───────┐        ┌──────────▼────────┐        ┌──────────▼──────┐
 │ Incident Svc │        │ Agent Orchestrator│        │ Approval Service│
 └──────┬───────┘        └──────────┬────────┘        └─────────────────┘
        │                           │
        │                   ┌───────┴───────────────────┐
        │                   │ AI Agent Runtime          │
        │                   │ Triage → Investigation →  │
        │                   │ RCA → Remediation → Verify│
        │                   └───────┬───────────────────┘
        │                           │
        │               ┌───────────┼─────────────┐
        │              MCP         RAG           LLM
        │               │           │             │
        │               ▼           ▼             ▼
        │        Ops MCP Server  pgvector     Model API
        │               │
        │     ┌─────────┼──────────┬───────────┐
        │     ▼         ▼          ▼           ▼
        │   Metrics    Logs      Traces       K8s
        ▼
   PostgreSQL  ── incident state / agents / approvals (authoritative)
```

## Telemetry (OpenTelemetry is the standard)

### Metrics

```text
http.server.request.duration
http.server.request.count
http.server.active_requests
jvm.memory.used
jvm.gc.duration
db.pool.active
db.pool.pending
kafka.consumer.lag
pod.restart.count
```

### Logs (structured JSON, correlated to traces)

```json
{
  "timestamp": "...",
  "level": "ERROR",
  "service": "payment-service",
  "traceId": "...",
  "spanId": "...",
  "message": "Database connection timeout",
  "errorCode": "DB_CONNECTION_TIMEOUT"
}
```

### Traces

Propagate context across every service boundary so an investigation can traverse
`API Gateway → Order Service → Payment Service → PostgreSQL`. Correlate trace IDs with logs and incident records.

## Kafka Event Model

Topics:

```text
telemetry.events
incident.detected
incident.updated
investigation.requested
investigation.completed
rca.generated
remediation.requested
remediation.approved
remediation.executed
remediation.failed
verification.completed
```

Requirements: consumer groups, retries, dead-letter topics, schema validation, idempotent processing, partition-aware design, correlation IDs, event versioning.

Event envelope (every message):

```json
{
  "eventId": "uuid",
  "eventType": "incident.detected",
  "schemaVersion": 1,
  "timestamp": "...",
  "correlationId": "incident-123",
  "source": "incident-service",
  "payload": {}
}
```

## PostgreSQL Data Model

Tables:

```text
services
service_dependencies
deployments
incidents
incident_events
investigations
observations
root_causes
remediation_plans
remediation_actions
approvals
agent_runs
tool_calls
runbooks
knowledge_documents
```

Key fields:

```text
incidents:    id, external_id, service_id, severity, status,
              environment, detected_at, resolved_at, root_cause_id
observations: id, incident_id, type, source, timestamp,
              content, confidence, trace_id
agent_runs:   id, incident_id, agent_type, model, status,
              input_tokens, output_tokens, latency_ms,
              started_at, completed_at
tool_calls:   id, agent_run_id, tool_name, arguments_hash,
              status, latency_ms, started_at, completed_at
```

## Redis Usage

Use Redis for short-lived / high-frequency state only: active incident cache, agent session state, idempotency keys, distributed locks, rate limiting, short-lived telemetry cache. **PostgreSQL remains authoritative** for durable state.

## Log Search Pattern (OpenSearch / Elasticsearch)

Never feed the LLM a raw log dump. Always:

```text
LLM query intent → structured query generation → validated search query
  → OpenSearch → filtered/aggregated results → compact evidence → LLM
```

This bounds tokens and makes investigations reproducible.

## Infrastructure

Local: Docker Compose + local Kubernetes (kind or Minikube).

```text
Docker · Kubernetes · Helm · Kafka · PostgreSQL · Redis · OpenSearch
OpenTelemetry Collector · Prometheus · Grafana · Jaeger/Tempo · Loki/OpenSearch Logs
```

Telemetry path: `Demo Services → OTel SDK/Agent → OTel Collector → {Metrics, Logs, Traces}`.
Event path: `Services/Collectors → Kafka → Incident Processing / Agent Workers`.

## CI/CD (GitHub Actions)

```text
Build → Unit tests → Integration tests → Agent evaluation
  → Container build → Security checks
```

Run the agent evaluation suite in CI so regressions are caught automatically.

## Frontend Screens (React / Next.js)

- **Incident Dashboard** — active incidents, severity, service, status, duration, current agent stage.
- **Incident Detail** — timeline, correlated telemetry, observations, evidence, RCA, confidence, related incidents, recommended remediation.
- **Approval Panel** — proposed action, risk level, expected impact, affected resources, evidence, approve/reject controls.
- **Agent Operations** — structured agent events, tool calls, evidence, final decisions. **NEVER** expose private chain-of-thought; show concise reasoning summaries only.

## Testing Strategy

- **Unit** — state transitions, risk classification, policy rules, remediation validation, event serialization, repository logic.
- **Integration** — Testcontainers for PostgreSQL, Kafka, Redis, OpenSearch.
- **Agent** — mock LLM outputs; verify schema validation, tool selection, loop limits, refusal of unsafe actions, correct escalation.
- **End-to-end** — inject DB failure → telemetry → incident → investigation → RCA → rollback → approval → execute → recover → resolved.

## First Repository Milestone

Build only: 3 Spring Boot demo services, 1 Incident Service, 1 Agent Service, PostgreSQL, Kafka, OpenTelemetry, Prometheus, Grafana, 1 Incident MCP server, 1 Spring AI Investigation Agent, 1 RAG knowledge base, 1 Kubernetes remediation, React dashboard. One failure mode: **database connection exhaustion caused by a bad payment-service deployment.** Stabilize this before expanding.
