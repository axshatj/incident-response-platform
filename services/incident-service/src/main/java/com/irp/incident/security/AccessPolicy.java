package com.irp.incident.security;

/**
 * Deterministic authorization for the public incident API. The model never
 * participates; role + method + path decide.
 */
public final class AccessPolicy {

    private AccessPolicy() {}

    public static boolean allows(Role role, String method, String path) {
        if (role == null || method == null || path == null) {
            return false;
        }
        if (role == Role.ADMIN) {
            return true;
        }
        String normalized = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        String verb = method.toUpperCase();
        if ("GET".equals(verb) || "HEAD".equals(verb) || "OPTIONS".equals(verb)) {
            return true;
        }
        if (!"POST".equals(verb)) {
            return false;
        }
        if (normalized.equals("/api/knowledge/search")) {
            return true;
        }
        if (isAgentWrite(normalized)) {
            return role == Role.AGENT;
        }
        if (normalized.endsWith("/approve") || normalized.endsWith("/reject")) {
            return role == Role.APPROVER;
        }
        if (normalized.endsWith("/acknowledge") || normalized.endsWith("/resolve")
                || normalized.equals("/api/incidents")) {
            return role == Role.OPERATOR || role == Role.APPROVER;
        }
        return false;
    }

    private static boolean isAgentWrite(String path) {
        return path.endsWith("/advance")
                || path.endsWith("/investigations")
                || path.endsWith("/investigation-report")
                || path.endsWith("/remediation-plan")
                || path.endsWith("/remediation-executions")
                || path.endsWith("/verification");
    }
}
