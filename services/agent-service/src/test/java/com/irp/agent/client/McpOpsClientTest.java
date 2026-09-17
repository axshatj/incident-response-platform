package com.irp.agent.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class McpOpsClientTest {

    private MockRestServiceServer server;
    private McpOpsClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new McpOpsClient(builder.baseUrl("http://mcp").build());
    }

    @Test
    void invokeUsesListedToolAndReturnsCompactText() {
        server.expect(requestTo("http://mcp/mcp/tools"))
                .andRespond(withSuccess("{\"tools\":[{\"name\":\"query_metrics\"}]}", APPLICATION_JSON));
        server.expect(requestTo("http://mcp/mcp"))
                .andExpect(method(POST))
                .andRespond(withSuccess("""
                        {"jsonrpc":"2.0","id":1,"result":{
                          "content":[{"type":"text","text":"metrics for payment-service: db.pool.active=50/50;"}],
                          "isError":false}}
                        """, APPLICATION_JSON));

        assertThat(client.invoke("query_metrics", "payment-service")).contains("db.pool.active");
        server.verify();
    }

    @Test
    void shellIsRejectedFromAllowlist() {
        server.expect(requestTo("http://mcp/mcp/tools"))
                .andRespond(withSuccess("{\"tools\":[{\"name\":\"query_metrics\"}]}", APPLICATION_JSON));

        assertThatThrownBy(() -> client.invoke("shell", "payment-service"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowlisted");
        server.verify();
    }
}
