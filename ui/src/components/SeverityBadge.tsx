import type { Severity } from "../api/client";

const SEVERITY_STYLES: Record<Severity, string> = {
  SEV1: "bg-red-600 text-white",
  SEV2: "bg-orange-500 text-white",
  SEV3: "bg-amber-500 text-slate-900",
  SEV4: "bg-slate-500 text-white",
};

export default function SeverityBadge({ severity }: { severity: Severity }) {
  return <span className={`badge ${SEVERITY_STYLES[severity]}`}>{severity}</span>;
}
