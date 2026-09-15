package com.irp.incident.api.dto;

import jakarta.validation.constraints.Size;

/**
 * Common request body for lifecycle actions (acknowledge / approve / reject /
 * resolve). Both fields are optional; the caller identity should come from the
 * auth context once Phase 9 lands.
 */
public record TransitionRequest(
        @Size(max = 128) String actor,
        @Size(max = 1024) String note
) {
    public String actorOr(String fallback) {
        return actor == null || actor.isBlank() ? fallback : actor;
    }
}
