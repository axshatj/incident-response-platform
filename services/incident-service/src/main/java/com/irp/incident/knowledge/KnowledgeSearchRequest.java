package com.irp.incident.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KnowledgeSearchRequest(
        @NotBlank @Size(max = 1024) String query,
        @Size(max = 128) String service,
        @Size(max = 64) String environment,
        Integer topK
) {}
