package com.irp.mcp.api;

import com.irp.mcp.tools.IncidentOpsTools;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class McpController {

    private static final Map<String, Object> INPUT_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of("service", Map.of("type", "string", "description", "Affected service name")),
            "required", java.util.List.of("service")
    );

    private final IncidentOpsTools tools;
    private final MeterRegistry registry;

    public McpController(IncidentOpsTools tools, MeterRegistry registry) {
        this.tools = tools;
        this.registry = registry;
    }

    @GetMapping("/mcp/tools")
    public McpProtocol.ToolsListResult listGet() {
        return listTools();
    }

    @PostMapping("/mcp")
    public McpProtocol.JsonRpcResponse handle(@RequestBody McpProtocol.JsonRpcRequest request) {
        if (request.method() == null) {
            return McpProtocol.JsonRpcResponse.error(request.id(), -32600, "Missing method");
        }
        return switch (request.method()) {
            case "initialize" -> McpProtocol.JsonRpcResponse.ok(request.id(), initialize());
            case "tools/list" -> McpProtocol.JsonRpcResponse.ok(request.id(), listTools());
            case "tools/call" -> call(request);
            default -> McpProtocol.JsonRpcResponse.error(request.id(), -32601, "Method not found: " + request.method());
        };
    }

    private McpProtocol.InitializeResult initialize() {
        return new McpProtocol.InitializeResult(
                McpProtocol.VERSION,
                Map.of("tools", Map.of("listChanged", false)),
                Map.of("name", "irp-ops-mcp", "version", "0.1.0")
        );
    }

    private McpProtocol.ToolsListResult listTools() {
        return new McpProtocol.ToolsListResult(
                IncidentOpsTools.ALLOWLIST.stream()
                        .sorted()
                        .map(name -> new McpProtocol.ToolDescriptor(name, tools.description(name), INPUT_SCHEMA))
                        .toList()
        );
    }

    private McpProtocol.JsonRpcResponse call(McpProtocol.JsonRpcRequest request) {
        if (request.params() == null || request.params().path("name").isMissingNode()) {
            return McpProtocol.JsonRpcResponse.error(request.id(), -32602, "tools/call requires params.name");
        }
        String name = request.params().path("name").asText();
        String service = request.params().path("arguments").path("service").asText("unknown");
        try {
            String evidence = tools.invoke(name, service);
            recordCall(name, "ok");
            return McpProtocol.JsonRpcResponse.ok(request.id(), McpProtocol.ToolCallResult.text(evidence));
        } catch (IllegalArgumentException e) {
            recordCall(name, "rejected");
            return McpProtocol.JsonRpcResponse.error(request.id(), -32000, e.getMessage());
        }
    }

    private void recordCall(String tool, String status) {
        Counter.builder("irp.mcp.tool_calls")
                .tag("tool", tool)
                .tag("status", status)
                .register(registry)
                .increment();
    }
}
