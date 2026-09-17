// Thin fetch wrapper. Vite proxies /api to http://localhost:8080 in dev.

import { authHeaders } from "../auth";

export type Severity = "SEV1" | "SEV2" | "SEV3" | "SEV4";

export type IncidentStatus =
  | "DETECTED"
  | "TRIAGING"
  | "INVESTIGATING"
  | "ROOT_CAUSE_IDENTIFIED"
  | "REMEDIATION_PROPOSED"
  | "AWAITING_APPROVAL"
  | "REMEDIATING"
  | "VERIFYING"
  | "RESOLVED";

export interface Incident {
  id: string;
  externalId: string | null;
  service: string;
  title: string;
  description: string | null;
  severity: Severity;
  status: IncidentStatus;
  environment: string;
  detectedAt: string;
  resolvedAt: string | null;
  rootCauseId: string | null;
  verificationAttempts: number;
}

export interface IncidentEvent {
  id: string;
  incidentId: string;
  eventType: string;
  fromStatus: IncidentStatus | null;
  toStatus: IncidentStatus | null;
  actor: string | null;
  note: string | null;
  occurredAt: string;
}

export interface InvestigationView {
  incidentId: string;
  observations: ObservationView[];
  agentRuns: AgentRunView[];
  rootCause: RootCauseView | null;
}

export interface ObservationView {
  id: string;
  type: string;
  source: string;
  observedAt: string;
  content: string;
  confidence: number | null;
  traceId: string | null;
}

export interface AgentRunView {
  id: string;
  agentType: string;
  model: string | null;
  status: string;
  latencyMs: number | null;
  errorMessage: string | null;
  startedAt: string;
  completedAt: string | null;
  toolCalls: ToolCallView[];
}

export interface ToolCallView {
  id: string;
  toolName: string;
  status: string;
  latencyMs: number | null;
  resultPreview: string | null;
  startedAt: string;
  completedAt: string | null;
}

export interface RootCauseView {
  id: string;
  statement: string;
  confidence: number;
  evidenceJson: string;
  counterEvidenceJson: string;
  affectedJson: string;
  createdAt: string;
}

export interface RemediationView {
  incidentId: string;
  plan: RemediationPlanView | null;
  executions: RemediationExecutionView[];
}

export interface RemediationPlanView {
  id: string;
  action: string;
  namespace: string;
  deployment: string;
  targetRevision: number;
  risk: string;
  decision: string;
  status: string;
  idempotencyKey: string;
  expectedImpact: string | null;
  blastRadius: string | null;
  confidence: number | null;
  rationale: string | null;
  createdAt: string;
}

export interface RemediationExecutionView {
  id: string;
  planId: string | null;
  idempotencyKey: string;
  status: string;
  resultPreview: string | null;
  errorMessage: string | null;
  startedAt: string;
  completedAt: string | null;
}

export interface VerificationView {
  incidentId: string;
  attempts: number;
  maxAttempts: number;
  latest: VerificationRunView | null;
}

export interface VerificationRunView {
  id: string;
  attempt: number;
  outcome: string;
  summary: string | null;
  signalsJson: string;
  createdAt: string;
}

export interface CreateIncidentRequest {
  externalId?: string | null;
  service: string;
  title: string;
  description?: string | null;
  severity: Severity;
  environment: string;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`/api${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...authHeaders(),
      ...(init?.headers ?? {}),
    },
    ...init,
  });
  if (!res.ok) {
    let body: unknown = null;
    try {
      body = await res.json();
    } catch {
      // ignore body parse errors
    }
    const err = new Error(`API ${res.status} ${res.statusText}`) as Error & { body?: unknown };
    err.body = body;
    throw err;
  }
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

export const api = {
  listIncidents: () => request<Incident[]>("/incidents"),
  createIncident: (body: CreateIncidentRequest) =>
    request<Incident>("/incidents", { method: "POST", body: JSON.stringify(body) }),
  getIncident: (id: string) => request<Incident>(`/incidents/${id}`),
  getTimeline: (id: string) => request<IncidentEvent[]>(`/incidents/${id}/events`),
  getInvestigation: (id: string) => request<InvestigationView>(`/incidents/${id}/investigation`),
  getRemediation: (id: string) => request<RemediationView>(`/incidents/${id}/remediation`),
  getVerification: (id: string) => request<VerificationView>(`/incidents/${id}/verification`),
  acknowledge: (id: string, actor?: string, note?: string) =>
    request<Incident>(`/incidents/${id}/acknowledge`, {
      method: "POST",
      body: JSON.stringify({ actor, note }),
    }),
  approve: (id: string, actor?: string, note?: string) =>
    request<Incident>(`/incidents/${id}/approve`, {
      method: "POST",
      body: JSON.stringify({ actor, note }),
    }),
  reject: (id: string, actor?: string, note?: string) =>
    request<Incident>(`/incidents/${id}/reject`, {
      method: "POST",
      body: JSON.stringify({ actor, note }),
    }),
  resolve: (id: string, actor?: string, note?: string) =>
    request<Incident>(`/incidents/${id}/resolve`, {
      method: "POST",
      body: JSON.stringify({ actor, note }),
    }),
};
