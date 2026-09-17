---
source: runbook
service: notification-service
team: messaging
environment: prod
severity: SEV3
tags: kafka,lag
title: Kafka consumer lag on notification-service
---

If notification-service Kafka lag grows while payment-service is healthy,
restart the lagged consumer or scale the consumer group. This is unrelated
to PostgreSQL connection exhaustion.
