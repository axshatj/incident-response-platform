package com.irp.incident.domain;

public class PolicyRejectedException extends RuntimeException {

    private final String action;
    private final String risk;

    public PolicyRejectedException(String action, String risk, String reason) {
        super(reason);
        this.action = action;
        this.risk = risk;
    }

    public String getAction() {
        return action;
    }

    public String getRisk() {
        return risk;
    }
}
