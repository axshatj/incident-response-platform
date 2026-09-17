package com.irp.incident.knowledge;

import java.util.UUID;

public record KnowledgeHit(
        UUID id,
        String source,
        String path,
        String title,
        String service,
        String snippet,
        double score
) {
    public String citation() {
        return "[" + source + ":" + path + "] " + title;
    }
}
