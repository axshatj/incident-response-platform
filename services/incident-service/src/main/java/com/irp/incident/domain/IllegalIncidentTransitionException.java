package com.irp.incident.domain;

public class IllegalIncidentTransitionException extends RuntimeException {

    private final IncidentStatus from;
    private final IncidentStatus to;

    public IllegalIncidentTransitionException(IncidentStatus from, IncidentStatus to) {
        super("Illegal incident transition: " + from + " -> " + to);
        this.from = from;
        this.to = to;
    }

    public IncidentStatus getFrom() { return from; }
    public IncidentStatus getTo() { return to; }
}
