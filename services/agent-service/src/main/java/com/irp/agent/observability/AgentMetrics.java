package com.irp.agent.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class AgentMetrics {

    private static final String LLM = "irp.agent.llm";
    private static final String TOOLS = "irp.agent.tool_calls";
    private static final String RUNS = "irp.agent.runs";
    private static final String LLM_LATENCY = "irp.agent.llm.latency";

    private final MeterRegistry registry;
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();

    public AgentMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordLlm(String agentType, boolean ok, long latencyMs) {
        counter(LLM, "agent", agentType, "status", ok ? "ok" : "error").increment();
        Timer.builder(LLM_LATENCY)
                .tags("agent", agentType)
                .register(registry)
                .record(latencyMs, TimeUnit.MILLISECONDS);
    }

    public void recordTool(String tool, boolean ok) {
        counter(TOOLS, "tool", tool, "status", ok ? "ok" : "error").increment();
    }

    public void recordRun(String agentType, boolean ok) {
        counter(RUNS, "agent", agentType, "status", ok ? "ok" : "error").increment();
    }

    private Counter counter(String name, String... tags) {
        StringBuilder key = new StringBuilder(name);
        for (String tag : tags) {
            key.append('|').append(tag);
        }
        return counters.computeIfAbsent(key.toString(),
                k -> Counter.builder(name).tags(tags).register(registry));
    }
}
