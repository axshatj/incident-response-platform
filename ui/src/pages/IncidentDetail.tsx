import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api, type Incident, type IncidentEvent, type InvestigationView, type RemediationView, type VerificationView } from "../api/client";
import { getActor } from "../auth";
import SeverityBadge from "../components/SeverityBadge";
import StatusBadge from "../components/StatusBadge";

type Action = "acknowledge" | "approve" | "reject" | "resolve";

function parseJsonList(raw: string | null | undefined): string[] {
  if (!raw) {
    return [];
  }
  try {
    const parsed = JSON.parse(raw) as unknown;
    return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === "string") : [];
  } catch {
    return [];
  }
}

const ACTIONS: { key: Action; label: string; hint: string }[] = [
  { key: "acknowledge", label: "Acknowledge", hint: "DETECTED → TRIAGING" },
  { key: "approve", label: "Approve", hint: "AWAITING_APPROVAL → REMEDIATING" },
  { key: "reject", label: "Reject", hint: "AWAITING_APPROVAL → INVESTIGATING" },
  { key: "resolve", label: "Resolve", hint: "VERIFYING → RESOLVED" },
];

export default function IncidentDetail() {
  const { id = "" } = useParams<{ id: string }>();
  const [incident, setIncident] = useState<Incident | null>(null);
  const [timeline, setTimeline] = useState<IncidentEvent[]>([]);
  const [investigation, setInvestigation] = useState<InvestigationView | null>(null);
  const [remediation, setRemediation] = useState<RemediationView | null>(null);
  const [verification, setVerification] = useState<VerificationView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<Action | null>(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      const [i, t, inv, rem, ver] = await Promise.all([
        api.getIncident(id),
        api.getTimeline(id),
        api.getInvestigation(id).catch(() => null),
        api.getRemediation(id).catch(() => null),
        api.getVerification(id).catch(() => null),
      ]);
      setIncident(i);
      setTimeline(t);
      setInvestigation(inv);
      setRemediation(rem);
      setVerification(ver);
    } catch (e) {
      setError((e as Error).message);
    }
  }, [id]);

  useEffect(() => {
    void load();
  }, [load]);

  const runAction = async (action: Action) => {
    setBusy(action);
    setError(null);
    try {
      await api[action](id, getActor(), `${action} via dashboard`);
      await load();
    } catch (e) {
      const err = e as Error & { body?: { message?: string } };
      setError(err.body?.message ?? err.message);
    } finally {
      setBusy(null);
    }
  };

  if (!incident) {
    return (
      <div>
        <Link to="/" className="text-sm text-slate-400 hover:text-slate-200">
          ← Back
        </Link>
        {error ? (
          <p className="mt-4 rounded border border-red-500/40 bg-red-500/10 p-3 text-sm text-red-300">
            {error}
          </p>
        ) : (
          <p className="mt-4 text-sm text-slate-500">Loading…</p>
        )}
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <Link to="/" className="text-sm text-slate-400 hover:text-slate-200">
        ← Back to incidents
      </Link>

      <section className="card">
        <div className="flex flex-wrap items-center gap-2">
          <SeverityBadge severity={incident.severity} />
          <StatusBadge status={incident.status} />
          <span className="text-xs text-slate-500">
            {incident.environment} · {incident.service}
          </span>
        </div>
        <h1 className="mt-2 text-xl font-semibold">{incident.title}</h1>
        {incident.description && (
          <p className="mt-2 text-sm text-slate-300">{incident.description}</p>
        )}
        <dl className="mt-4 grid grid-cols-2 gap-4 text-sm sm:grid-cols-4">
          <div>
            <dt className="text-xs uppercase text-slate-500">Detected</dt>
            <dd>{new Date(incident.detectedAt).toLocaleString()}</dd>
          </div>
          <div>
            <dt className="text-xs uppercase text-slate-500">Resolved</dt>
            <dd>{incident.resolvedAt ? new Date(incident.resolvedAt).toLocaleString() : "—"}</dd>
          </div>
          <div>
            <dt className="text-xs uppercase text-slate-500">External ID</dt>
            <dd>{incident.externalId ?? "—"}</dd>
          </div>
          <div>
            <dt className="text-xs uppercase text-slate-500">Verification</dt>
            <dd>
              {incident.verificationAttempts ?? 0}
              {verification ? ` / ${verification.maxAttempts}` : ""}
            </dd>
          </div>
          <div>
            <dt className="text-xs uppercase text-slate-500">ID</dt>
            <dd className="truncate font-mono text-xs">{incident.id}</dd>
          </div>
        </dl>
      </section>

      <section className="card">
        <h2 className="mb-3 text-lg font-semibold">Lifecycle actions</h2>
        {error && (
          <div className="mb-3 rounded border border-red-500/40 bg-red-500/10 p-2 text-sm text-red-300">
            {error}
          </div>
        )}
        <div className="flex flex-wrap gap-2">
          {ACTIONS.map((a) => (
            <button
              key={a.key}
              onClick={() => void runAction(a.key)}
              disabled={busy !== null}
              className="btn flex flex-col items-start"
              title={a.hint}
            >
              <span className="font-medium">{busy === a.key ? "…" : a.label}</span>
              <span className="text-[10px] text-slate-500">{a.hint}</span>
            </button>
          ))}
        </div>
      </section>

      {investigation?.rootCause && (
        <section className="card">
          <h2 className="mb-3 text-lg font-semibold">Root cause</h2>
          <p className="text-sm">{investigation.rootCause.statement}</p>
          <p className="mt-2 text-xs text-slate-500">
            confidence {(investigation.rootCause.confidence * 100).toFixed(0)}%
          </p>
          {parseJsonList(investigation.rootCause.evidenceJson).length > 0 && (
            <ul className="mt-3 list-disc space-y-1 pl-5 text-sm text-slate-300">
              {parseJsonList(investigation.rootCause.evidenceJson).map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          )}
        </section>
      )}

      {remediation?.plan && (
        <section className="card">
          <h2 className="mb-3 text-lg font-semibold">Proposed remediation</h2>
          <p className="text-sm font-medium">
            {remediation.plan.action} · {remediation.plan.namespace}/{remediation.plan.deployment} @ r
            {remediation.plan.targetRevision}
          </p>
          <p className="mt-2 text-xs uppercase text-slate-500">
            risk {remediation.plan.risk} · {remediation.plan.decision} · {remediation.plan.status}
          </p>
          {remediation.plan.rationale && (
            <p className="mt-2 text-sm text-slate-300">{remediation.plan.rationale}</p>
          )}
          {remediation.plan.expectedImpact && (
            <p className="mt-1 text-xs text-slate-500">Impact: {remediation.plan.expectedImpact}</p>
          )}
          {remediation.executions.length > 0 && (
            <ul className="mt-3 space-y-1 text-sm text-slate-300">
              {remediation.executions.map((ex) => (
                <li key={ex.id}>
                  {ex.status}
                  {ex.resultPreview ? ` — ${ex.resultPreview}` : ""}
                  {ex.errorMessage ? ` — ${ex.errorMessage}` : ""}
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      {verification?.latest && (
        <section className="card">
          <h2 className="mb-3 text-lg font-semibold">Verification</h2>
          <p className="text-sm font-medium">{verification.latest.outcome}</p>
          {verification.latest.summary && (
            <p className="mt-2 text-sm text-slate-300">{verification.latest.summary}</p>
          )}
          <p className="mt-2 text-xs uppercase text-slate-500">
            attempt {verification.latest.attempt} of {verification.maxAttempts}
          </p>
          {parseJsonList(verification.latest.signalsJson).length > 0 && (
            <ul className="mt-3 list-disc space-y-1 pl-5 text-sm text-slate-300">
              {parseJsonList(verification.latest.signalsJson).map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          )}
        </section>
      )}

      {investigation && investigation.observations.length > 0 && (
        <section className="card">
          <h2 className="mb-3 text-lg font-semibold">Observations</h2>
          <ul className="space-y-2 text-sm">
            {investigation.observations.map((o) => (
              <li key={o.id} className="rounded border border-border p-2">
                <div className="text-xs uppercase text-slate-500">
                  {o.type === "KNOWLEDGE" ? "Cited knowledge" : o.type} · {o.source}
                </div>
                <p className="mt-1 text-slate-200">{o.content}</p>
              </li>
            ))}
          </ul>
        </section>
      )}

      {investigation && investigation.agentRuns.length > 0 && (
        <section className="card">
          <h2 className="mb-3 text-lg font-semibold">Agent runs</h2>
          <ul className="space-y-3 text-sm">
            {investigation.agentRuns.map((run) => (
              <li key={run.id}>
                <div className="flex items-center gap-2">
                  <span className="font-medium">{run.agentType}</span>
                  <span className="text-xs text-slate-500">
                    {run.status} {run.latencyMs != null ? `· ${run.latencyMs}ms` : ""}
                  </span>
                </div>
                {run.toolCalls.length > 0 && (
                  <ul className="mt-1 space-y-1 text-xs text-slate-400">
                    {run.toolCalls.map((tc) => (
                      <li key={tc.id}>
                        {tc.toolName} → {tc.status}
                      </li>
                    ))}
                  </ul>
                )}
              </li>
            ))}
          </ul>
        </section>
      )}

      <section className="card">
        <h2 className="mb-3 text-lg font-semibold">Timeline</h2>
        {timeline.length === 0 ? (
          <p className="text-sm text-slate-500">No events yet.</p>
        ) : (
          <ol className="space-y-3">
            {timeline.map((event) => (
              <li key={event.id} className="flex gap-3">
                <span className="mt-1 h-2 w-2 flex-shrink-0 rounded-full bg-indigo-400" />
                <div className="flex-1">
                  <div className="flex items-center gap-2 text-sm">
                    <span className="font-medium">{event.eventType}</span>
                    {event.fromStatus && event.toStatus && (
                      <span className="text-xs text-slate-500">
                        {event.fromStatus} → {event.toStatus}
                      </span>
                    )}
                  </div>
                  <div className="mt-0.5 text-xs text-slate-500">
                    {new Date(event.occurredAt).toLocaleString()}
                    {event.actor ? ` · ${event.actor}` : ""}
                  </div>
                  {event.note && <p className="mt-1 text-sm text-slate-300">{event.note}</p>}
                </div>
              </li>
            ))}
          </ol>
        )}
      </section>
    </div>
  );
}
