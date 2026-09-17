# Threat Model (MVP)

Scope: the local Compose demo of the payment-service DB-exhaustion path. This is a
control-plane threat model, not a pentest report. Production must put OIDC in
front of the API and replace header RBAC.

## Assets

| Asset | Why it matters |
|-------|----------------|
| Incident state + audit trail | Source of truth for who approved what |
| Remediation executor | Can change production deployments |
| MCP read tools | Telemetry and deployment history |
| LLM prompts / outputs | Schema-validated proposals only |
| Knowledge base | Runbooks that steer investigation |

## Trust boundaries

```text
Browser / curl
    │  X-IRP-Role + X-IRP-Actor (demo) / OIDC (prod)
    ▼
incident-service  (:8080)     ← only public HTTP API
    │
    ├─ PostgreSQL             ← durable state
    ├─ Kafka                  ← async workflows
    ▼
agent-service (:8081)  ──MCP──► ops-mcp-server (:8082)
    │  role=AGENT (least privilege, cannot approve)
    ▼
remediation-service (:8083)   ← separate credentials, policy + allowlists
```

The LLM never holds Kubernetes or database credentials. Agent-service is AGENT,
not ADMIN.

## STRIDE (abridged)

| Threat | Mitigation in this repo |
|--------|-------------------------|
| Spoofed operator (S) | Role header required when `irp.auth.enabled`. Prod: OIDC at a gateway. |
| Tampered remediation plan (T) | Schema validation; policy engine; executor re-validates allowlists + idempotency. |
| Repudiated approval (R) | Immutable `incident_events` + remediation executions. |
| Info disclosure via LLM (I) | Compact tool previews; no raw log dumps; no chain-of-thought persistence. |
| Denial of service on API (D) | In-memory rate limit (`irp.ratelimit.requests-per-minute`). Prod: gateway/Redis. |
| Elevation via agent (E) | AGENT cannot `approve` / `reject` / create incidents. CRITICAL actions prohibited. MCP has no shell/kubectl. |

## Residual risk

- Header RBAC is **not** authentication. Anyone who can reach :8080 can pick a role.
- MCP evidence is stubbed; verification therefore requires a SUCCEEDED execution.
- Simulated Kubernetes is in-process; a real cluster needs a least-privilege SA.
- No tenant isolation yet. One environment = one demo.

## Roles

```text
VIEWER     GET incidents and evidence
OPERATOR   create / acknowledge / resolve
APPROVER   approve or reject HIGH-risk remediation
ADMIN      all of the above (humans only)
AGENT      investigation writes only (plans, reports, verification)
```
