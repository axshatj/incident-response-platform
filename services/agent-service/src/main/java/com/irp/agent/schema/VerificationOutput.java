package com.irp.agent.schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record VerificationOutput(
        @NotBlank
        @Pattern(regexp = "RESOLVED|PARTIALLY_RESOLVED|NOT_RESOLVED")
        String outcome,
        @NotBlank @Size(max = 1024) String summary,
        @NotEmpty List<@NotBlank @Size(max = 512) String> checks
) {}
