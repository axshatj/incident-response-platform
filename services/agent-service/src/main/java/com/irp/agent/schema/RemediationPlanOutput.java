package com.irp.agent.schema;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RemediationPlanOutput(
        @NotBlank @Size(max = 64) String action,
        @NotBlank @Size(max = 64) String namespace,
        @NotBlank @Size(max = 128) String deployment,
        @NotNull Integer targetRevision,
        @NotBlank @Size(max = 512) String expectedImpact,
        @NotBlank @Size(max = 256) String blastRadius,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double confidence,
        @NotBlank @Size(max = 1024) String rationale
) {}
