---
source: runbook
service: payment-service
team: payments
environment: prod
severity: SEV2
tags: postgres,hikari,connections,rollback
title: Payment service database connection exhaustion
---

Symptoms: p99 latency climbs, 5xx on /charge, Hikari `db.pool.active` sits at max,
`db.pool.pending` grows, Postgres `numbackends` equals `max_connections`.

Likely cause: a recent payment-service deploy raising `hikari.maximum-pool-size`
so replicas together exceed Postgres `max_connections`.

Immediate actions (HIGH risk — require approval in production):
1. Roll back payment-service to the last healthy revision.
2. Do not raise Postgres max_connections as the first move.

Verify recovery: error rate and p99 back to baseline, pool pending = 0,
Postgres connection count below 80% of the limit.
