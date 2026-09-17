package com.irp.agent.schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record TriageOutput(
        @NotBlank @Size(max = 8) String severity,
        @NotEmpty List<@NotBlank @Size(max = 128) String> suspectedServices,
        @NotEmpty List<@NotBlank @Size(max = 64) String> investigationPlan
) {}
