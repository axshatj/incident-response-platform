package com.irp.mcp.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

public final class McpProtocol {

    public static final String VERSION = "2024-11-05";

    private McpProtocol() {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record JsonRpcRequest(
            String jsonrpc,
            Object id,
            String method,
            JsonNode params
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record JsonRpcResponse(
            String jsonrpc,
            Object id,
            Object result,
            JsonRpcError error
    ) {
        public static JsonRpcResponse ok(Object id, Object result) {
            return new JsonRpcResponse("2.0", id, result, null);
        }

        public static JsonRpcResponse error(Object id, int code, String message) {
            return new JsonRpcResponse("2.0", id, null, new JsonRpcError(code, message));
        }
    }

    public record JsonRpcError(int code, String message) {}

    public record InitializeResult(
            String protocolVersion,
            Map<String, Object> capabilities,
            Map<String, String> serverInfo
    ) {}

    public record ToolsListResult(List<ToolDescriptor> tools) {}

    public record ToolDescriptor(
            String name,
            String description,
            Map<String, Object> inputSchema
    ) {}

    public record ToolCallResult(List<Content> content, boolean isError) {
        public static ToolCallResult text(String text) {
            return new ToolCallResult(List.of(new Content("text", text)), false);
        }

        public static ToolCallResult error(String text) {
            return new ToolCallResult(List.of(new Content("text", text)), true);
        }
    }

    public record Content(String type, String text) {}
}
