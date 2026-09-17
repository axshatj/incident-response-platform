package com.irp.agent.tools;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Deterministic ops tools used BEFORE the LLM is asked to reason. Results are
 * compact evidence strings — never raw log dumps. MCP comes in a later phase;
 * this in-process catalog is the Phase 4 stand-in.
 *
 * There is no shell, kubectl, or SQL execution tool.
 */
@Component
public class OpsToolCatalog {

    public static final Set<String> ALLOWLIST = Set.of(
            "query_metrics",
            "query_logs",
            "query_traces",
            "get_deployment_history",
            "get_database_metrics",
            "get_service_health"
    );

    public String invoke(String toolName, String service) {
        if (!ALLOWLIST.contains(toolName)) {
            throw new IllegalArgumentException("Tool not allowlisted: " + toolName);
        }
        return switch (toolName) {
            case "query_metrics" -> metrics(service);
            case "query_logs" -> logs(service);
            case "query_traces" -> traces(service);
            case "get_deployment_history" -> deployments(service);
            case "get_database_metrics" -> database(service);
            case "get_service_health" -> health(service);
            default -> throw new IllegalArgumentException("Tool not allowlisted: " + toolName);
        };
    }

    private String metrics(String service) {
        return compact(service, "metrics", Map.of(
                "http.server.request.duration.p99", "2.4s (baseline 180ms)",
                "http.server.error.rate", "18%",
                "db.pool.active", "50/50",
                "db.pool.pending", "73"
        ));
    }

    private String logs(String service) {
        return compact(service, "logs", Map.of(
                "sample_count", "12 aggregated ERROR lines (bounded)",
                "dominant_error", "DB_CONNECTION_TIMEOUT",
                "first_seen", "30s after payment-service v42 rollout"
        ));
    }

    private String traces(String service) {
        return compact(service, "traces", Map.of(
                "critical_path", "api-gateway -> order-service -> payment-service -> postgres",
                "slowest_span", "payment-service GET /charge db.acquire 1.9s"
        ));
    }

    private String deployments(String service) {
        return compact(service, "deployments", Map.of(
                "current_revision", "42",
                "previous_revision", "41",
                "changed", "hikari.maximum-pool-size 10 -> 50",
                "rolled_out_at", "T-12m"
        ));
    }

    private String database(String service) {
        return compact(service, "database", Map.of(
                "postgres.max_connections", "100",
                "postgres.numbackends", "100",
                "wait_event", "Client:ClientRead / connection slots exhausted"
        ));
    }

    private String health(String service) {
        return compact(service, "health", Map.of(
                "status", "DEGRADED",
                "ready_replicas", "3/3",
                "restart_count_15m", "0"
        ));
    }

    private static String compact(String service, String source, Map<String, String> fields) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", service);
        body.put("source", source);
        body.put("fields", fields);
        StringBuilder sb = new StringBuilder();
        sb.append(source).append(" for ").append(service).append(':');
        fields.forEach((k, v) -> sb.append(' ').append(k).append('=').append(v).append(';'));
        return sb.toString();
    }
}
