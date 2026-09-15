package com.irp.incident.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Canonical incident lifecycle states. The allowed transition graph mirrors the
 * state machine defined in SKILL.md. Any change here MUST update
 * {@link #ALLOWED_TRANSITIONS} and the corresponding tests.
 */
public enum IncidentStatus {
    DETECTED,
    TRIAGING,
    INVESTIGATING,
    ROOT_CAUSE_IDENTIFIED,
    REMEDIATION_PROPOSED,
    AWAITING_APPROVAL,
    REMEDIATING,
    VERIFYING,
    RESOLVED;

    /**
     * Directed graph of legal transitions. The verification failure path
     * (VERIFYING -> INVESTIGATING) is included so recovery retries can loop back.
     */
    public static final Map<IncidentStatus, Set<IncidentStatus>> ALLOWED_TRANSITIONS = Map.of(
            DETECTED, EnumSet.of(TRIAGING),
            TRIAGING, EnumSet.of(INVESTIGATING),
            INVESTIGATING, EnumSet.of(ROOT_CAUSE_IDENTIFIED),
            ROOT_CAUSE_IDENTIFIED, EnumSet.of(REMEDIATION_PROPOSED),
            REMEDIATION_PROPOSED, EnumSet.of(AWAITING_APPROVAL),
            AWAITING_APPROVAL, EnumSet.of(REMEDIATING, INVESTIGATING),
            REMEDIATING, EnumSet.of(VERIFYING),
            VERIFYING, EnumSet.of(RESOLVED, INVESTIGATING),
            RESOLVED, EnumSet.noneOf(IncidentStatus.class)
    );

    public boolean canTransitionTo(IncidentStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isTerminal() {
        return this == RESOLVED;
    }
}
