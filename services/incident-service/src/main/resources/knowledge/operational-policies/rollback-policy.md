---
source: operational-policy
service: payment-service
team: sre
environment: prod
severity: SEV1
tags: rollback,approval
title: Production rollback policy
---

Rolling back a production deployment is HIGH risk and requires human approval.
Restarting a single unhealthy pod is LOW and may auto-execute.
Destructive database operations are CRITICAL and prohibited.
