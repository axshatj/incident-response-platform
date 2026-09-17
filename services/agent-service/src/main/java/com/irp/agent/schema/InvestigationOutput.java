package com.irp.agent.schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InvestigationOutput(
        @NotBlank @Size(max = 2048) String summary,
        @jakarta.validation.constraints.NotNull Boolean sufficientEvidence
) {}
