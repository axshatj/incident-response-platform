import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api, type Incident, type IncidentEvent } from "../api/client";
import SeverityBadge from "../components/SeverityBadge";
import StatusBadge from "../components/StatusBadge";

type Action = "acknowledge" | "approve" | "reject" | "resolve";

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
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<Action | null>(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      const [i, t] = await Promise.all([api.getIncident(id), api.getTimeline(id)]);
      setIncident(i);
      setTimeline(t);
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
      await api[action](id, "dev-user", `${action} via dashboard`);
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
