package com.irp.agent.tools;

import java.util.Set;

/** In-process stand-in so orchestrator tests do not need the MCP server. */
public class StubOpsTools implements OpsTools {

    static final Set<String> ALLOWLIST = Set.of(
            "query_metrics",
            "query_logs",
            "query_traces",
            "get_deployment_history",
            "get_database_metrics",
            "get_service_health"
    );

    @Override
    public Set<String> allowlist() {
        return ALLOWLIST;
    }

    @Override
    public String invoke(String toolName, String service) {
        if (!ALLOWLIST.contains(toolName)) {
            throw new IllegalArgumentException("Tool not allowlisted: " + toolName);
        }
        return toolName + " for " + service + ": compact";
    }
}
