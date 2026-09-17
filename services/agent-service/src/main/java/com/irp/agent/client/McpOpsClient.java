package com.irp.agent.client;

import com.irp.agent.tools.OpsTools;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * MCP client for the Incident Operations server. Speaks JSON-RPC 2.0
 * {@code tools/list} and {@code tools/call} over HTTP. The LLM never sees
 * this client — the orchestrator invokes it deterministically.
 */
@Component
public class McpOpsClient implements OpsTools {

    private final RestClient http;
    private volatile Set<String> cachedAllowlist;

    public McpOpsClient(RestClient mcpRestClient) {
        this.http = mcpRestClient;
    }

    @Override
    public Set<String> allowlist() {
        Set<String> cached = cachedAllowlist;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (cachedAllowlist == null) {
                cachedAllowlist = Set.copyOf(listToolNames());
            }
            return cachedAllowlist;
        }
    }

    @Override
    public String invoke(String toolName, String service) {
        if (!allowlist().contains(toolName)) {
            throw new IllegalArgumentException("Tool not allowlisted: " + toolName);
        }
        JsonRpcResponse response = http.post()
                .uri("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "jsonrpc", "2.0",
                        "id", 1,
                        "method", "tools/call",
                        "params", Map.of(
                                "name", toolName,
                                "arguments", Map.of("service", service)
                        )
                ))
                .retrieve()
                .body(JsonRpcResponse.class);
        if (response == null) {
            throw new IllegalStateException("MCP server returned empty body");
        }
        if (response.error() != null) {
            throw new IllegalArgumentException(response.error().message());
        }
        if (response.result() == null || response.result().content() == null || response.result().content().isEmpty()) {
            throw new IllegalStateException("MCP tool returned no content");
        }
        return response.result().content().getFirst().text();
    }

    private List<String> listToolNames() {
        Map<String, Object> body = http.get()
                .uri("/mcp/tools")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        if (body == null || !(body.get("tools") instanceof List<?> tools)) {
            return List.of();
        }
        return tools.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<?, ?>) item)
                .map(item -> item.get("name"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toList());
    }

    public record JsonRpcResponse(String jsonrpc, Object id, ToolCallResult result, JsonRpcError error) {}

    public record JsonRpcError(int code, String message) {}

    public record ToolCallResult(List<Content> content, boolean isError) {}

    public record Content(String type, String text) {}
}
