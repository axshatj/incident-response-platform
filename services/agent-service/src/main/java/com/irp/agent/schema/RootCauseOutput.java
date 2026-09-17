package com.irp.agent.schema;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RootCauseOutput(
        @NotBlank @Size(max = 2048) String rootCause,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double confidence,
        @NotEmpty List<@NotBlank @Size(max = 512) String> evidence,
        List<@Size(max = 512) String> counterEvidence,
        @NotEmpty List<@NotBlank @Size(max = 128) String> affectedComponents
) {}
