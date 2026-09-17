package com.irp.incident.api.dto;

import com.irp.incident.domain.VerificationRun;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VerificationView(
        UUID incidentId,
        int attempts,
        int maxAttempts,
        RunView latest
) {
    public record RunView(
            UUID id,
            int attempt,
            String outcome,
            String summary,
            String signalsJson,
            Instant createdAt
    ) {
        public static RunView from(VerificationRun run) {
            return new RunView(
                    run.getId(),
                    run.getAttempt(),
                    run.getOutcome(),
                    run.getSummary(),
                    run.getSignalsJson(),
                    run.getCreatedAt()
            );
        }
    }
}
