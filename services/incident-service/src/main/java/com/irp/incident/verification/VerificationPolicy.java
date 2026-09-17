package com.irp.incident.verification;

/**
 * Deterministic gate: the LLM may summarize, but cannot declare recovery
 * without a successful remediation execution.
 */
public final class VerificationPolicy {

    public static final int DEFAULT_MAX_ATTEMPTS = 2;

    private VerificationPolicy() {}

    public static String effectiveOutcome(String llmOutcome, boolean executionSucceeded) {
        String outcome = llmOutcome == null ? "NOT_RESOLVED" : llmOutcome.trim().toUpperCase();
        if ("RESOLVED".equals(outcome) && !executionSucceeded) {
            return "NOT_RESOLVED";
        }
        if ("PARTIALLY_RESOLVED".equals(outcome)) {
            return "NOT_RESOLVED";
        }
        if (!"RESOLVED".equals(outcome) && !"NOT_RESOLVED".equals(outcome)) {
            return "NOT_RESOLVED";
        }
        return outcome;
    }

    public static boolean shouldRetry(int attemptsIncludingThis, int maxAttempts) {
        return attemptsIncludingThis < maxAttempts;
    }
}
