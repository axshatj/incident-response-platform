package com.irp.agent.tools;

import java.util.Set;

/**
 * Read-only ops capabilities. Production implementation is the MCP client;
 * tests use an in-process stub. Never exposes shell or kubectl.
 */
public interface OpsTools {

    Set<String> allowlist();

    String invoke(String toolName, String service);
}
