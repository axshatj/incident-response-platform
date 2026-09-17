export type IrpRole = "VIEWER" | "OPERATOR" | "APPROVER" | "ADMIN";

const ROLE_KEY = "irp.role";
const ACTOR_KEY = "irp.actor";

const ROLES: IrpRole[] = ["VIEWER", "OPERATOR", "APPROVER", "ADMIN"];

export function listRoles(): IrpRole[] {
  return ROLES;
}

export function getRole(): IrpRole {
  const stored = localStorage.getItem(ROLE_KEY);
  return ROLES.includes(stored as IrpRole) ? (stored as IrpRole) : "APPROVER";
}

export function setRole(role: IrpRole): void {
  localStorage.setItem(ROLE_KEY, role);
}

export function getActor(): string {
  return localStorage.getItem(ACTOR_KEY) || "dev-user";
}

export function setActor(actor: string): void {
  localStorage.setItem(ACTOR_KEY, actor.trim() || "dev-user");
}

export function authHeaders(): Record<string, string> {
  return {
    "X-IRP-Role": getRole(),
    "X-IRP-Actor": getActor(),
  };
}
