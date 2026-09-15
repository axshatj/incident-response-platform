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

## MVP Scenario

A bad `payment-service` deployment exhausts PostgreSQL connections. The platform detects the incident, investigates with bounded AI agents, recommends a rollback, requires human approval, executes via Kubernetes, and verifies recovery.

## Tech Stack

Java 21 · Spring Boot 3 · Spring AI · PostgreSQL + pgvector · Kafka · Redis · OpenSearch · OpenTelemetry · Kubernetes · Prometheus · Grafana · React/Next.js

## Status

Documentation and engineering skill only — implementation pending.
