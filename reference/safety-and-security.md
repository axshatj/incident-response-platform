# Remediation Safety & Security Reference

This is the highest-stakes code path. The LLM proposes; deterministic software decides and executes.

## Risk Classification

Classify every remediation action into one class:

```text
LOW       restart unhealthy pod, restart failed consumer
MEDIUM    scale deployment within configured bounds
HIGH      rollback production deployment, modify production configuration
CRITICAL  destructive database operations, arbitrary shell execution,
          IAM / credential changes
```

## Approval Policy

```text
LOW      → auto-execute if policy allows
MEDIUM   → configurable (default: require approval in production)
HIGH     → mandatory human approval
CRITICAL → PROHIBITED (never executed)
```

Every action MUST generate an immutable audit record regardless of outcome.

## Deterministic Executor Contract

The executor never trusts model output directly. It accepts a structured action:

```json
{
  "action": "ROLLBACK_DEPLOYMENT",
  "namespace": "production",
  "deployment": "payment-service",
  "targetRevision": 41
}
```

Before calling the Kubernetes API, the executor MUST validate ALL of:

- namespace allowlist
- resource allowlist
- target revision exists and is valid
- blast radius within limits
- replica limits
- caller authorization
- approval status (for MEDIUM/HIGH)
- idempotency key (reject/short-circuit duplicates)

Only after every check passes does it invoke Kubernetes. On any failure, reject the action, record the reason, and do not partially apply.

## Security Model

Implement at least:

- OAuth2 / OIDC login
- role-based authorization
- least-privilege Kubernetes service account
- read-only telemetry credentials where possible
- separate credentials for remediation (distinct from investigation)
- secret management via environment / secret manager
- audit logging
- rate limiting
- request validation
- tool allowlists
- tenant / environment boundaries

Roles:

```text
VIEWER     read incidents and evidence
OPERATOR   acknowledge, trigger investigation
APPROVER   approve/reject remediation
ADMIN      manage policy and configuration
```

**The AI agent NEVER inherits unrestricted administrator permissions.** It uses least-privilege, read-oriented credentials for investigation; remediation runs under separate, policy-gated credentials in the deterministic executor.
