package com.irp.agent.llm;

import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Deterministic ChatModel used when no external LLM is configured. It returns
 * schema-shaped JSON for the payment-service DB-exhaustion MVP so the platform
 * can demo locally without an API key. It never executes tools itself.
 */
@Component
@ConditionalOnProperty(name = "irp.agent.llm.provider", havingValue = "stub", matchIfMissing = true)
public class ScriptedChatModel implements ChatModel {

    @Override
    public ChatResponse call(Prompt prompt) {
        String text = prompt.getContents();
        String json;
        if (contains(text, "Triage Agent") || contains(text, "triage")) {
            json = """
                    {
                      "severity": "SEV2",
                      "suspectedServices": ["payment-service"],
                      "investigationPlan": [
                        "query_metrics",
                        "query_logs",
                        "query_traces",
                        "get_deployment_history",
                        "get_database_metrics"
                      ]
                    }
                    """;
        } else if (contains(text, "Root Cause") || contains(text, "root cause")) {
            boolean cited = contains(text, "RAG") || contains(text, "knowledge/");
            json = """
                    {
                      "rootCause": "payment-service v42 increased the DB connection pool and exhausted PostgreSQL connections",
                      "confidence": 0.91,
                      "evidence": [
                        "DB connection acquisition latency increased after deployment v42",
                        "Deployment v42 changed hikari maximum-pool-size from 10 to 50",
                        "PostgreSQL connection count reached the configured limit of 100"%s
                      ],
                      "counterEvidence": [],
                      "affectedComponents": ["payment-service", "postgres"]
                    }
                    """.formatted(cited
                    ? ",\n                        \"Cited [runbook:knowledge/runbooks/payment-db-pool.md] Payment service database connection exhaustion\""
                    : "");
        } else {
            json = """
                    {
                      "summary": "Investigation collected metrics, logs, traces and deployment history pointing at a connection-pool change.",
                      "sufficientEvidence": true
                    }
                    """;
        }
        return new ChatResponse(List.of(new Generation(new AssistantMessage(json))));
    }

    private static boolean contains(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase().contains(needle.toLowerCase());
    }
}
