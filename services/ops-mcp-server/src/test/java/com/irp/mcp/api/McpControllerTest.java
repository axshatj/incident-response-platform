package com.irp.mcp.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class McpControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void toolsListDoesNotIncludeShell() throws Exception {
        mvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tools[?(@.name=='query_metrics')]").exists())
                .andExpect(jsonPath("$.tools[?(@.name=='shell')]").doesNotExist());
    }

    @Test
    void jsonRpcCallReturnsCompactEvidence() throws Exception {
        mvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":1,"method":"tools/call",
                                 "params":{"name":"query_metrics","arguments":{"service":"payment-service"}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isError").value(false))
                .andExpect(jsonPath("$.result.content[0].text").value(org.hamcrest.Matchers.containsString("db.pool.active")));
    }

    @Test
    void jsonRpcRejectsShell() throws Exception {
        mvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":2,"method":"tools/call",
                                 "params":{"name":"shell","arguments":{"service":"payment-service"}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32000))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("not allowlisted")));
    }
}
