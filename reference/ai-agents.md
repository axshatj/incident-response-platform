# AI Agents, MCP, RAG, Evaluation Reference

Every agent output MUST be structured and machine-validated against a schema before it is used. Bound every loop with iteration, token, latency, and tool-call limits.

## Triage Agent

Input: alert, affected service, initial metrics, severity hints.

Output (validate this shape):

```json
{
  "severity": "SEV-2",
  "suspectedServices": ["payment-service"],
  "investigationPlan": [
    "query_metrics",
    "inspect_recent_deployments",
    "query_logs",
    "query_traces"
  ]
}
```

## Investigation Agent

The primary tool-using agent. It forms hypotheses, gathers evidence, and stops when evidence is sufficient or the investigation budget is reached.

Tools:

```text
queryMetrics()          getPodStatus()           getGitChanges()
queryLogs()             getPodEvents()           getKafkaLag()
queryTraces()           getDeploymentHistory()   getDatabaseMetrics()
getServiceHealth()      getDeploymentManifest()  searchRunbooks()
getDependencyGraph()    searchPastIncidents()
```

## Root Cause Agent

Input: observations, evidence, recent changes, retrieved knowledge, dependency relationships.

Output:

```json
{
  "rootCause": "payment-service v42 increased the DB connection pool and exhausted PostgreSQL connections",
  "confidence": 0.91,
  "evidence": [
    "DB connection acquisition latency increased after deployment",
    "Deployment v42 changed connection pool configuration",
    "PostgreSQL connection count reached configured limit"
  ],
  "counterEvidence": [],
  "affectedComponents": ["payment-service", "postgres"]
}
```

**NEVER** present unsupported guesses as confirmed facts. Populate `counterEvidence` honestly.

## Remediation Planning Agent

Candidate actions: `restartPod()`, `scaleDeployment()`, `rollbackDeployment()`, `pauseDeployment()`, `restartConsumer()`.

For each candidate, return: expected impact, risk, reversibility, blast radius, confidence, prerequisites. The plan is a **proposal only** — the policy engine and executor decide what actually runs.

## Verification Agent

After remediation, check: error rate, latency, pod health, database metrics, Kafka lag, relevant logs.

Output: `RESOLVED | PARTIALLY_RESOLVED | NOT_RESOLVED`. If not resolved, route back to investigation with a bounded retry count.

## MCP: Incident Operations Server

Expose operational capabilities to the AI layer through an MCP server. Tools:

```text
get_metrics             get_deployment           search_runbooks
query_logs              get_deployment_history   search_past_incidents
get_trace               get_git_diff             get_dependency_graph
get_pod_status          get_kafka_consumer_lag
get_k8s_events
```

Rules:

- MCP tools provide **capabilities**; policy and authorization decide which may actually be invoked.
- **NEVER** expose an unrestricted shell-execution tool to the model.
- Keep MCP tools read-oriented for investigation; remediation goes through the deterministic executor (see `safety-and-security.md`).

## RAG / Knowledge Layer (PostgreSQL + pgvector)

Document sources: `runbooks/`, `architecture/`, `past-incidents/`, `operational-policies/`.

Per-document metadata: source, service, team, environment, severity, tags, createdAt, updatedAt.

Retrieval pipeline:

```text
Incident context → query construction → vector/hybrid retrieval
  → metadata filtering → top-K evidence → agent context
```

Cite or link retrieved evidence inside incident reports.

## AI Observability

Instrument the AI system itself. Track:

```text
LLM latency, token usage, error rate
agent run duration, agent retries
tool-call count, tool-call latency
RAG retrieval latency, retrieved-document count
remediation success rate, RCA confidence
MTTA, MTTR
```

Target trace shape:

```text
incident
 ├── triage-agent (llm-call, get-metrics)
 ├── investigation-agent (query-logs, get-trace, rag-retrieval, llm-call)
 ├── rca-agent (llm-call)
 └── remediation (policy-check, kubernetes-action, verification)
```

## Evaluation Harness

Do NOT evaluate the agent only by fluent text. Build a synthetic incident benchmark with known scenarios:

```text
DB_CONNECTION_EXHAUSTION   BAD_DEPLOYMENT
KAFKA_CONSUMER_LAG         DOWNSTREAM_TIMEOUT
MEMORY_LEAK                REDIS_UNAVAILABLE
```

For each scenario define: expected affected service, expected evidence, acceptable root causes, unsafe actions, recommended remediation, verification criteria.

Measure:

```text
incident detection accuracy   unsafe-action rate
RCA accuracy                  false remediation rate
tool-selection accuracy       recovery detection accuracy
average tokens per incident   average investigation latency
```

Run the suite repeatedly in CI to catch regressions.
