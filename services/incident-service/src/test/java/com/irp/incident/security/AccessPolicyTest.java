package com.irp.incident.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("eval")
class AccessPolicyTest {

    @Test
    void viewerIsReadOnly() {
        assertThat(AccessPolicy.allows(Role.VIEWER, "GET", "/api/incidents")).isTrue();
        assertThat(AccessPolicy.allows(Role.VIEWER, "POST", "/api/incidents")).isFalse();
        assertThat(AccessPolicy.allows(Role.VIEWER, "POST", "/api/incidents/x/approve")).isFalse();
    }

    @Test
    void operatorCannotApproveProductionRollback() {
        assertThat(AccessPolicy.allows(Role.OPERATOR, "POST", "/api/incidents")).isTrue();
        assertThat(AccessPolicy.allows(Role.OPERATOR, "POST", "/api/incidents/x/acknowledge")).isTrue();
        assertThat(AccessPolicy.allows(Role.OPERATOR, "POST", "/api/incidents/x/approve")).isFalse();
        assertThat(AccessPolicy.allows(Role.APPROVER, "POST", "/api/incidents/x/approve")).isTrue();
    }

    @Test
    void agentNeverInheritsAdminAndCannotApprove() {
        assertThat(AccessPolicy.allows(Role.AGENT, "POST", "/api/incidents/x/verification")).isTrue();
        assertThat(AccessPolicy.allows(Role.AGENT, "POST", "/api/incidents/x/remediation-plan")).isTrue();
        assertThat(AccessPolicy.allows(Role.AGENT, "POST", "/api/incidents/x/approve")).isFalse();
        assertThat(AccessPolicy.allows(Role.AGENT, "POST", "/api/incidents")).isFalse();
        assertThat(AccessPolicy.allows(Role.AGENT, "POST", "/api/knowledge/search")).isTrue();
        assertThat(AccessPolicy.allows(Role.ADMIN, "POST", "/api/incidents/x/approve")).isTrue();
    }

    @Test
    void missingRoleIsDenied() {
        assertThat(AccessPolicy.allows(null, "GET", "/api/incidents")).isFalse();
        assertThat(Role.parse("not-a-role")).isNull();
        assertThat(Role.parse("approver")).isEqualTo(Role.APPROVER);
    }
}
