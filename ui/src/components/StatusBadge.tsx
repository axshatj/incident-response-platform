import type { IncidentStatus } from "../api/client";

const STATUS_STYLES: Record<IncidentStatus, string> = {
  DETECTED: "bg-red-500/15 text-red-300 ring-1 ring-inset ring-red-500/30",
  TRIAGING: "bg-orange-500/15 text-orange-300 ring-1 ring-inset ring-orange-500/30",
  INVESTIGATING: "bg-amber-500/15 text-amber-300 ring-1 ring-inset ring-amber-500/30",
  ROOT_CAUSE_IDENTIFIED: "bg-yellow-500/15 text-yellow-300 ring-1 ring-inset ring-yellow-500/30",
  REMEDIATION_PROPOSED: "bg-sky-500/15 text-sky-300 ring-1 ring-inset ring-sky-500/30",
  AWAITING_APPROVAL: "bg-purple-500/15 text-purple-300 ring-1 ring-inset ring-purple-500/30",
  REMEDIATING: "bg-blue-500/15 text-blue-300 ring-1 ring-inset ring-blue-500/30",
  VERIFYING: "bg-teal-500/15 text-teal-300 ring-1 ring-inset ring-teal-500/30",
  RESOLVED: "bg-emerald-500/15 text-emerald-300 ring-1 ring-inset ring-emerald-500/30",
};

export default function StatusBadge({ status }: { status: IncidentStatus }) {
  return <span className={`badge ${STATUS_STYLES[status]}`}>{status.replace(/_/g, " ")}</span>;
}
