package com.irp.incident.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record RemediationExecutionRequest(
        UUID planId,
        @NotBlank @Size(max = 256) String idempotencyKey,
        @NotBlank @Size(max = 32) String status,
        @Size(max = 2048) String resultPreview,
        @Size(max = 1024) String errorMessage,
        Instant startedAt,
        Instant completedAt
) {}
