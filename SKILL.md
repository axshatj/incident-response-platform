---
name: incident-response-platform
description: >-
  Build and extend the Autonomous Incident Response Platform, an event-driven
  Java/Spring Boot system that detects incidents, investigates with bounded AI
  agents, and executes policy-controlled Kubernetes remediation. Use when
  working in this repository on incident services, agent orchestration, MCP
  tools, RAG, Kafka events, OpenTelemetry, remediation safety, or evaluation.
---

# Autonomous Incident Response Platform

Build a production-style platform that detects operational incidents, correlates telemetry, investigates with AI agents, performs evidence-based root-cause analysis, proposes remediation, executes approved actions, and verifies recovery.

Follow the directives below exactly. When a decision is not covered here, prefer the option that keeps the MVP small, deterministic, and auditable.

## The One Rule That Governs Everything

**AI reasons and investigates. Deterministic software controls execution.**

```text
Telemetry → Detection → Investigation → Evidence → RCA → Remediation Plan
                                                              ↓
                                                        Policy Engine
                                                         /         \
                                                      Safe        Risky
                                                        ↓            ↓
                                                 Auto-execute    Approval
                                                         \        /
                                                          Execute
                                                             ↓
                                                        Verification
                                                             ↓
                                                    Resolved / Retry
```

**NEVER let an LLM execute shell, `kubectl`, SQL, or cloud API calls directly.** The model emits structured proposals only; a deterministic executor validates and runs them.

## Non-Negotiable Guardrails

Apply these to every change. Treat a violation as a bug.

1. Retrieve and validate deterministically **before** calling the LLM.
2. **NEVER** send unbounded logs, traces, or query results to the model. Structure, filter, and aggregate first.
3. Represent every model-generated action as structured data and validate it against a schema before use.
4. Authorize every remediation through the deterministic policy engine, not the model.
5. Require human approval for HIGH-risk actions; **prohibit** CRITICAL actions entirely.
6. Make every action idempotent (use idempotency keys).
7. Bound every agent loop with explicit iteration, token, latency, and tool-call limits.
8. Persist durable incident state in PostgreSQL. Redis is cache/coordination only, never source of truth.
9. Use Kafka for asynchronous workflows; do not make the pipeline fully synchronous.
10. Keep agent reasoning separate from infrastructure execution, in different services.
11. Prefer bounded, specialized agents over one unrestricted general-purpose agent.
12. Write an immutable audit record for every agent run, tool call, approval, and remediation.
13. Expose evidence and concise reasoning summaries. **NEVER** persist or display raw chain-of-thought.
14. Write evaluation cases before claiming any AI behavior works.
15. Give the AI agent least-privilege credentials. It **NEVER** inherits admin.

## Tech Stack (use these unless told otherwise)

Java 21 · Spring Boot 3 · Spring AI · PostgreSQL + pgvector · Kafka · Redis · OpenSearch · OpenTelemetry · Kubernetes (kind/Minikube locally) · Prometheus · Grafana · Jaeger/Tempo · React/Next.js · Docker Compose · GitHub Actions · Testcontainers.

## Build Order (do not skip ahead)

Build strictly in this order. Each phase must run locally and be demoable before starting the next.

| Phase | Add | Deliverable |
|-------|-----|-------------|
| 1 Foundation | Spring Boot services, PostgreSQL, incident state machine, basic UI, Docker | Manually create and resolve an incident |
| 2 Observability | OpenTelemetry, Prometheus, Grafana, logs, traces | Visualize a real incident in the demo system |
| 3 Event-driven | Kafka topics, async workers, retries, DLQ | Auto-create incidents from alert events |
| 4 AI investigation | Spring AI, Triage Agent, Investigation Agent, tool calling | Agent investigates a real incident |
| 5 RAG | pgvector, runbooks, incident history, retrieval | Investigation cites operational knowledge |
| 6 MCP | Incident Operations MCP server + client | Tools decoupled from orchestration |
| 7 Remediation | Policy engine, approval workflow, K8s executor, idempotency, audit | Safely perform a rollback |
| 8 Verification | Verification Agent, recovery checks, bounded retry/escalation | Platform closes incidents automatically |
| 9 Production polish | AuthN/Z, AI observability, eval suite, CI/CD, dashboards, threat model | Portfolio-ready |

## MVP: the only scenario that matters first

Ship this single end-to-end path before adding any other failure class or agent:

**A bad `payment-service` deployment exhausts PostgreSQL connections.**

```text
Injected DB connection exhaustion → OpenTelemetry → Alert/Incident → Kafka
  → Investigation Agent (metrics + logs + deployment history) → RCA
  → Rollback recommendation → Human approval → Kubernetes rollback
  → Verification → Resolved
```

**Do not** add extra services, agents, or failure modes until this path is reliable.

## Incident State Machine

Persist state in PostgreSQL. Every transition emits an event and an audit record.

```text
DETECTED → TRIAGING → INVESTIGATING → ROOT_CAUSE_IDENTIFIED
  → REMEDIATION_PROPOSED → AWAITING_APPROVAL → REMEDIATING
  → VERIFYING → RESOLVED

Failure path: VERIFYING → INVESTIGATING   (bounded retry count)
```

## Services (one responsibility each)

- **API Gateway / BFF** — authN/Z, UI APIs, incident queries, approval requests, streaming updates, rate limiting.
- **Incident Service** — owns incident lifecycle and durable state.
- **Agent Service** — hosts the AI workflow and agent runtime.
- **Remediation Service** — validates plans, enforces policy/allowlists, executes, reports status.
- **Approval Service** — approval requests, approvers, expiration, audit trail.
- **Knowledge Service** — ingest/chunk/embed/retrieve runbooks and past incidents.

Core API surface:

```text
GET  /api/incidents
POST /api/incidents
GET  /api/incidents/{id}
POST /api/incidents/{id}/acknowledge
POST /api/incidents/{id}/approve
POST /api/incidents/{id}/reject
POST /api/incidents/{id}/resolve
```

## AI Agents (bounded and specialized)

Build five narrow agents. Each returns schema-validated structured output, never free text used as control flow.

- **Triage Agent** — classify severity, suspected services, and an investigation plan.
- **Investigation Agent** — the tool-using agent; forms hypotheses, gathers evidence, stops on sufficient evidence or budget.
- **Root Cause Agent** — produces a hypothesis with confidence, evidence, and counter-evidence. **NEVER** presents unsupported guesses as facts.
- **Remediation Planning Agent** — for each candidate action, returns impact, risk, reversibility, blast radius, confidence, prerequisites.
- **Verification Agent** — returns `RESOLVED | PARTIALLY_RESOLVED | NOT_RESOLVED`; on failure routes back to investigation within the retry budget.

Details and required JSON schemas: see [reference/ai-agents.md](reference/ai-agents.md).

## Remediation Safety (the highest-stakes code path)

Classify every action into `LOW | MEDIUM | HIGH | CRITICAL` and gate it:

```text
LOW      → auto-execute if policy allows   (restart unhealthy pod, restart failed consumer)
MEDIUM   → configurable                     (scale deployment within bounds)
HIGH     → mandatory human approval         (rollback prod, modify prod config)
CRITICAL → PROHIBITED                       (destructive DB ops, shell, IAM/credential changes)
```

The executor is deterministic and validates namespace allowlist, resource allowlist, target revision, blast radius, replica limits, authorization, approval status, and idempotency key **before** touching the Kubernetes API.

Full policy model, executor contract, and security roles: see [reference/safety-and-security.md](reference/safety-and-security.md).

## Demo Environment & Failure Injection

Build a small app (`api-gateway`, `order-service`, `payment-service`, `inventory-service`, `notification-service`) with deterministic chaos endpoints for demos and integration tests:

```text
POST /chaos/payment/latency
POST /chaos/payment/db-connections
POST /chaos/kafka/consumer-lag
POST /chaos/service/error-rate
```

Supported failure classes to grow into (start with `database-connection-exhaustion`): high-latency, http-5xx-spike, kafka-consumer-lag, memory-pressure, pod-crash, bad-deployment.

## Reference Material (read when working in that area)

- [reference/architecture.md](reference/architecture.md) — full architecture, telemetry spec, Kafka event model + envelope, PostgreSQL data model, Redis usage, log search pattern, infrastructure, CI/CD.
- [reference/ai-agents.md](reference/ai-agents.md) — agent I/O schemas, tool catalog, MCP server design, RAG pipeline, AI observability, evaluation harness.
- [reference/safety-and-security.md](reference/safety-and-security.md) — remediation policy classes, deterministic executor contract, security model and roles.

## Definition of Done

The project is portfolio-ready when all hold:

- The full MVP flow runs locally end-to-end.
- ≥3 demo microservices emit real telemetry; incidents are deterministically reproducible.
- Kafka drives asynchronous incident processing.
- An AI agent uses tools (not just text) and RAG retrieves relevant runbooks/history.
- MCP provides operational capabilities; remediation is policy-controlled.
- Dangerous actions require approval or are blocked; Kubernetes performs ≥1 safe remediation.
- Verification determines recovery; every agent run and remediation is auditable.
- Integration tests cover the main flow; an evaluation suite measures agent quality.
- The UI shows the incident timeline and remediation workflow.
- The README has architecture diagrams and a reproducible local setup, plus a demo video from injection to resolution.

## Positioning

Title: **Autonomous Incident Response Platform**. Describe it as a production-style, event-driven Java + AI reliability platform with RAG, MCP, and policy-controlled agents. **Do not** call it an "AI chatbot" or "LLM project."
