// Thin fetch wrapper. Vite proxies /api to http://localhost:8080 in dev.

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
    headers: { "Content-Type": "application/json", ...(init?.headers ?? {}) },
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
