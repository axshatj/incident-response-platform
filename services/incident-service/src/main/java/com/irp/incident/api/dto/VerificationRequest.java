package com.irp.incident.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record VerificationRequest(
        @NotBlank @Size(max = 32) String outcome,
        @Size(max = 1024) String summary,
        List<@Size(max = 512) String> signals
) {}
