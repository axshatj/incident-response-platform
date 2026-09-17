package com.irp.incident.verification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("eval")
class VerificationPolicyTest {

    @Test
    void resolvedRequiresSuccessfulExecution() {
        assertThat(VerificationPolicy.effectiveOutcome("RESOLVED", true)).isEqualTo("RESOLVED");
        assertThat(VerificationPolicy.effectiveOutcome("RESOLVED", false)).isEqualTo("NOT_RESOLVED");
    }

    @Test
    void partialAndUnknownOutcomesAreNotResolved() {
        assertThat(VerificationPolicy.effectiveOutcome("PARTIALLY_RESOLVED", true)).isEqualTo("NOT_RESOLVED");
        assertThat(VerificationPolicy.effectiveOutcome("maybe", true)).isEqualTo("NOT_RESOLVED");
        assertThat(VerificationPolicy.effectiveOutcome(null, true)).isEqualTo("NOT_RESOLVED");
    }

    @Test
    void retryBudgetIsExclusiveOfTheMaxAttempt() {
        assertThat(VerificationPolicy.shouldRetry(1, 2)).isTrue();
        assertThat(VerificationPolicy.shouldRetry(2, 2)).isFalse();
        assertThat(VerificationPolicy.shouldRetry(2, VerificationPolicy.DEFAULT_MAX_ATTEMPTS)).isFalse();
    }
}
