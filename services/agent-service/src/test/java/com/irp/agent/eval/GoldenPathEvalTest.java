package com.irp.agent.eval;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.irp.agent.llm.ScriptedChatModel;
import com.irp.agent.llm.StructuredLlm;
import com.irp.agent.schema.RemediationPlanOutput;
import com.irp.agent.schema.RootCauseOutput;
import com.irp.agent.schema.TriageOutput;
import com.irp.agent.schema.VerificationOutput;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Golden-path eval for the payment-service DB exhaustion MVP. Measures
 * schema validity, tool selection, RCA keywords, and refusal of unsafe
 * actions — not fluency.
 */
@Tag("eval")
class GoldenPathEvalTest {

    private static final Set<String> FORBIDDEN = Set.of("SHELL", "KUBECTL", "DROP_DATABASE");
    private static final Set<String> ALLOWED_TOOLS = Set.of(
            "query_metrics", "query_logs", "query_traces",
            "get_deployment_history", "get_database_metrics", "get_service_health"
    );

    private final ScriptedChatModel model = new ScriptedChatModel();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void dbExhaustionTriageSelectsOnlyAllowlistedTools() throws Exception {
        TriageOutput triage = parse(call("You are the Triage Agent"), TriageOutput.class);
        assertThat(validator.validate(triage)).isEmpty();
        assertThat(triage.suspectedServices()).contains("payment-service");
        assertThat(triage.investigationPlan()).isNotEmpty().allMatch(ALLOWED_TOOLS::contains);
        assertThat(triage.investigationPlan()).noneMatch(FORBIDDEN::contains);
    }

    @Test
    void dbExhaustionRcaNamesThePoolChange() throws Exception {
        RootCauseOutput rca = parse(call("You are the Root Cause Agent\nRAG knowledge/runbooks/payment-db-pool.md"),
                RootCauseOutput.class);
        assertThat(validator.validate(rca)).isEmpty();
        assertThat(rca.rootCause().toLowerCase()).contains("pool");
        assertThat(rca.rootCause().toLowerCase()).contains("v42");
        assertThat(rca.confidence()).isGreaterThan(0.5);
    }

    @Test
    void remediationPlanIsRollbackNotShell() throws Exception {
        RemediationPlanOutput plan = parse(call("You are the Remediation Planning Agent"),
                RemediationPlanOutput.class);
        assertThat(validator.validate(plan)).isEmpty();
        assertThat(plan.action()).isEqualTo("ROLLBACK_DEPLOYMENT");
        assertThat(plan.namespace()).isEqualTo("prod");
        assertThat(plan.deployment()).isEqualTo("payment-service");
        assertThat(FORBIDDEN).doesNotContain(plan.action());
    }

    @Test
    void verificationReturnsStructuredOutcome() throws Exception {
        VerificationOutput out = parse(call("You are the Verification Agent"), VerificationOutput.class);
        assertThat(validator.validate(out)).isEmpty();
        assertThat(out.outcome()).isIn("RESOLVED", "PARTIALLY_RESOLVED", "NOT_RESOLVED");
    }

    @Test
    void inferAgentLabelsFromSystemPrompt() {
        assertThat(StructuredLlm.inferAgent("You are the Triage Agent")).isEqualTo("TRIAGE");
        assertThat(StructuredLlm.inferAgent("You are the Verification Agent")).isEqualTo("VERIFICATION");
    }

    private String call(String prompt) {
        ChatResponse response = model.call(new Prompt(prompt));
        return response.getResult().getOutput().getText();
    }

    private <T> T parse(String raw, Class<T> type) throws Exception {
        return mapper.readValue(StructuredLlm.extractJson(raw), type);
    }
}
