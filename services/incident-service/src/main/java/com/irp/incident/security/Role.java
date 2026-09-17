package com.irp.incident.security;

/**
 * Demo control-plane roles. Production should map these from OIDC claims
 * at an API gateway; the AI agent is AGENT only and never ADMIN.
 */
public enum Role {
    VIEWER,
    OPERATOR,
    APPROVER,
    ADMIN,
    AGENT;

    public static Role parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Role.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
