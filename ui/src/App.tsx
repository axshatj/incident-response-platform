import { useState } from "react";
import { Link, Outlet } from "react-router-dom";
import { getActor, getRole, listRoles, setActor, setRole, type IrpRole } from "./auth";

export default function App() {
  const [role, setRoleState] = useState<IrpRole>(getRole);
  const [actor, setActorState] = useState(getActor);

  return (
    <div className="min-h-screen">
      <header className="border-b border-border bg-panel/60 backdrop-blur">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-6 py-4">
          <Link to="/" className="flex items-center gap-2 text-lg font-semibold tracking-tight">
            <span className="inline-block h-2 w-2 rounded-full bg-emerald-400" />
            IRP <span className="text-slate-500">— Incident Response Platform</span>
          </Link>
          <div className="flex items-center gap-2 text-xs">
            <label className="text-slate-500">
              Actor
              <input
                className="input ml-2 w-28 py-1"
                value={actor}
                onChange={(e) => {
                  setActor(e.target.value);
                  setActorState(e.target.value);
                }}
              />
            </label>
            <label className="text-slate-500">
              Role
              <select
                className="input ml-2 w-32 py-1"
                value={role}
                onChange={(e) => {
                  const next = e.target.value as IrpRole;
                  setRole(next);
                  setRoleState(next);
                }}
              >
                {listRoles().map((r) => (
                  <option key={r} value={r}>
                    {r}
                  </option>
                ))}
              </select>
            </label>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-6 py-8">
        <Outlet />
      </main>
    </div>
  );
}
