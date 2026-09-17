package com.irp.incident.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PolicyEngineTest {

    private final PolicyEngine engine = new PolicyEngine();

    @Test
    void productionRollbackRequiresApproval() {
        PolicyVerdict verdict = engine.evaluate("ROLLBACK_DEPLOYMENT", "prod", "prod");
        assertThat(verdict.risk()).isEqualTo(RiskClass.HIGH);
        assertThat(verdict.decision()).isEqualTo(PolicyDecisionType.REQUIRE_APPROVAL);
    }

    @Test
    void restartPodIsAutoExecutable() {
        PolicyVerdict verdict = engine.evaluate("RESTART_POD", "prod", "prod");
        assertThat(verdict.risk()).isEqualTo(RiskClass.LOW);
        assertThat(verdict.decision()).isEqualTo(PolicyDecisionType.AUTO_EXECUTE);
    }

    @Test
    void shellAndDropDatabaseAreProhibited() {
        assertThat(engine.evaluate("SHELL", "prod", "prod").decision())
                .isEqualTo(PolicyDecisionType.PROHIBITED);
        assertThat(engine.evaluate("DROP_DATABASE", "prod", "prod").risk())
                .isEqualTo(RiskClass.CRITICAL);
        assertThat(engine.evaluate("kubectl", "prod", "prod").decision())
                .isEqualTo(PolicyDecisionType.PROHIBITED);
    }
}
