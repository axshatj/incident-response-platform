package com.irp.incident.knowledge;

import com.irp.incident.observability.IncidentMetrics;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeService {

    private static final int DEFAULT_TOP_K = 3;
    private static final int MAX_TOP_K = 8;
    private static final int SNIPPET_CHARS = 400;

    private final JdbcTemplate jdbc;
    private final HashingEmbedder embedder;
    private final IncidentMetrics metrics;
    private final Clock clock;

    public KnowledgeService(JdbcTemplate jdbc,
                            HashingEmbedder embedder,
                            IncidentMetrics metrics,
                            Clock clock) {
        this.jdbc = jdbc;
        this.embedder = embedder;
        this.metrics = metrics;
        this.clock = clock;
    }

    public long count() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_documents", Long.class);
        return n == null ? 0 : n;
    }

    @Transactional
    public void upsert(String source,
                       String path,
                       String title,
                       String service,
                       String team,
                       String environment,
                       String severity,
                       String tags,
                       String content) {
        Instant now = clock.instant();
        String vector = HashingEmbedder.toPgVector(embedder.embed(title + " " + content + " " + tags));
        jdbc.update("""
                        INSERT INTO knowledge_documents
                          (id, source, path, title, service, team, environment, severity, tags,
                           content, embedding, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS vector), ?, ?)
                        ON CONFLICT (path) DO UPDATE SET
                          source = EXCLUDED.source,
                          title = EXCLUDED.title,
                          service = EXCLUDED.service,
                          team = EXCLUDED.team,
                          environment = EXCLUDED.environment,
                          severity = EXCLUDED.severity,
                          tags = EXCLUDED.tags,
                          content = EXCLUDED.content,
                          embedding = EXCLUDED.embedding,
                          updated_at = EXCLUDED.updated_at
                        """,
                UUID.randomUUID(),
                source,
                path,
                title,
                service,
                team,
                environment,
                severity,
                tags,
                content,
                vector,
                Timestamp.from(now),
                Timestamp.from(now)
        );
    }

    /**
     * Hybrid retrieve: metadata filter on service when provided, then pgvector
     * cosine distance, top-K compact snippets only.
     */
    public List<KnowledgeHit> search(String query, String service, String environment, Integer topK) {
        int k = topK == null ? DEFAULT_TOP_K : Math.min(Math.max(topK, 1), MAX_TOP_K);
        String vector = HashingEmbedder.toPgVector(embedder.embed(query));
        String serviceFilter = blankToNull(service);
        String environmentFilter = blankToNull(environment);
        List<KnowledgeHit> hits = jdbc.query("""
                        SELECT id, source, path, title, service, content,
                               1 - (embedding <=> CAST(? AS vector)) AS score
                        FROM knowledge_documents
                        WHERE (? IS NULL OR service = ? OR service IS NULL)
                          AND (? IS NULL OR environment = ? OR environment IS NULL)
                        ORDER BY embedding <=> CAST(? AS vector)
                        LIMIT ?
                        """,
                (rs, rowNum) -> new KnowledgeHit(
                        rs.getObject("id", UUID.class),
                        rs.getString("source"),
                        rs.getString("path"),
                        rs.getString("title"),
                        rs.getString("service"),
                        snippet(rs.getString("content")),
                        rs.getDouble("score")
                ),
                vector,
                serviceFilter, serviceFilter,
                environmentFilter, environmentFilter,
                vector,
                k
        );
        metrics.recordRagRetrieved(hits.size());
        return hits;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String snippet(String content) {
        if (content == null) {
            return "";
        }
        String collapsed = content.replaceAll("\\s+", " ").trim();
        return collapsed.length() <= SNIPPET_CHARS ? collapsed : collapsed.substring(0, SNIPPET_CHARS) + "…";
    }
}
