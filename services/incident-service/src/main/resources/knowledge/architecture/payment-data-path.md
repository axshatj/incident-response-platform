---
source: architecture
service: payment-service
team: payments
environment: prod
severity: SEV3
tags: topology,postgres
title: Payment service data path
---

Request path: API Gateway → order-service → payment-service → PostgreSQL.

payment-service owns the Hikari pool. Each replica opens up to
`hikari.maximum-pool-size` connections. Cluster-wide connections ≈ replicas × pool size.
The shared Postgres instance is configured with max_connections=100 in the demo env.
