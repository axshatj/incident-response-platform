package com.irp.incident.policy;

public record PolicyVerdict(
        RiskClass risk,
        PolicyDecisionType decision,
        String reason
) {}
