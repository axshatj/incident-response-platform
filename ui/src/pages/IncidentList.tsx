import { FormEvent, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, type CreateIncidentRequest, type Incident, type Severity } from "../api/client";
import SeverityBadge from "../components/SeverityBadge";
import StatusBadge from "../components/StatusBadge";

const SEVERITIES: Severity[] = ["SEV1", "SEV2", "SEV3", "SEV4"];

export default function IncidentList() {
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      setIncidents(await api.listIncidents());
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const onCreate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const body: CreateIncidentRequest = {
      externalId: (form.get("externalId") as string) || null,
      service: form.get("service") as string,
      title: form.get("title") as string,
      description: (form.get("description") as string) || null,
      severity: form.get("severity") as Severity,
      environment: form.get("environment") as string,
    };
    setCreating(true);
    setError(null);
    try {
      await api.createIncident(body);
      (event.target as HTMLFormElement).reset();
      await load();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
      <section className="card lg:col-span-2">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-lg font-semibold">Active incidents</h2>
          <button className="btn" onClick={() => void load()} disabled={loading}>
            {loading ? "Refreshing…" : "Refresh"}
          </button>
        </div>
        {error && (
          <div className="mb-3 rounded border border-red-500/40 bg-red-500/10 p-2 text-sm text-red-300">
            {error}
          </div>
        )}
        {!loading && incidents.length === 0 && (
          <p className="py-8 text-center text-sm text-slate-500">
            No incidents yet. Create one from the panel on the right.
          </p>
        )}
        <ul className="divide-y divide-border">
          {incidents.map((i) => (
            <li key={i.id} className="py-3">
              <Link to={`/incidents/${i.id}`} className="block hover:bg-slate-800/40 -mx-2 px-2 py-2 rounded">
                <div className="flex items-center justify-between gap-2">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <SeverityBadge severity={i.severity} />
                      <StatusBadge status={i.status} />
                      <span className="text-xs text-slate-500">
                        {i.environment} · {i.service}
                      </span>
                    </div>
                    <p className="mt-1 truncate font-medium">{i.title}</p>
                  </div>
                  <span className="whitespace-nowrap text-xs text-slate-500">
                    {new Date(i.detectedAt).toLocaleString()}
                  </span>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      </section>

      <section className="card">
        <h2 className="mb-3 text-lg font-semibold">Create incident</h2>
        <form onSubmit={onCreate} className="space-y-3">
          <div>
            <label className="mb-1 block text-xs uppercase text-slate-500">Service</label>
            <input name="service" required placeholder="payment-service" className="input" />
          </div>
          <div>
            <label className="mb-1 block text-xs uppercase text-slate-500">Title</label>
            <input name="title" required placeholder="DB connection exhaustion" className="input" />
          </div>
          <div>
            <label className="mb-1 block text-xs uppercase text-slate-500">Description</label>
            <textarea
              name="description"
              rows={2}
              placeholder="Pool saturated after v42 deploy"
              className="input"
            />
          </div>
          <div className="grid grid-cols-2 gap-2">
            <div>
              <label className="mb-1 block text-xs uppercase text-slate-500">Severity</label>
              <select name="severity" defaultValue="SEV2" className="input" required>
                {SEVERITIES.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-xs uppercase text-slate-500">Environment</label>
              <input name="environment" required defaultValue="dev" className="input" />
            </div>
          </div>
          <div>
            <label className="mb-1 block text-xs uppercase text-slate-500">External ID (optional)</label>
            <input name="externalId" placeholder="pagerduty-XYZ" className="input" />
          </div>
          <button type="submit" className="btn-primary w-full" disabled={creating}>
            {creating ? "Creating…" : "Create incident"}
          </button>
        </form>
      </section>
    </div>
  );
}
