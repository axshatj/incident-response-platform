package com.irp.incident.api.dto;

import com.irp.incident.domain.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIncidentRequest(
        @Size(max = 128) String externalId,
        @NotBlank @Size(max = 128) String service,
        @NotBlank @Size(max = 256) String title,
        @Size(max = 4096) String description,
        @NotNull Severity severity,
        @NotBlank @Size(max = 64) String environment
) {}
