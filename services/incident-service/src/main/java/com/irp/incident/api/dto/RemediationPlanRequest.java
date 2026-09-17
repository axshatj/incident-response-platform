package com.irp.incident.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RemediationPlanRequest(
        @NotBlank @Size(max = 64) String action,
        @NotBlank @Size(max = 64) String namespace,
        @NotBlank @Size(max = 128) String deployment,
        @NotNull Integer targetRevision,
        @Size(max = 512) String expectedImpact,
        @Size(max = 256) String blastRadius,
        Double confidence,
        @Size(max = 1024) String rationale
) {}
