package com.irp.incident.policy;

import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Deterministic remediation policy. The LLM never decides risk or execution.
 */
@Component
public class PolicyEngine {

    private static final Set<String> CRITICAL = Set.of(
            "SHELL",
            "KUBECTL",
            "DROP_DATABASE",
            "DELETE_DATABASE",
            "ARBITRARY_SQL",
            "IAM_CHANGE",
            "DESTROY_CLUSTER"
    );

    public PolicyVerdict evaluate(String action, String environment, String namespace) {
        String normalized = normalize(action);
        if (normalized.isBlank() || CRITICAL.contains(normalized) || normalized.contains("SHELL") || normalized.contains("KUBECTL")) {
            return new PolicyVerdict(
                    RiskClass.CRITICAL,
                    PolicyDecisionType.PROHIBITED,
                    "Destructive or unrestricted actions are never executed"
            );
        }
        boolean prod = isProd(environment, namespace);
        return switch (normalized) {
            case "RESTART_POD", "RESTART_CONSUMER" -> new PolicyVerdict(
                    RiskClass.LOW,
                    PolicyDecisionType.AUTO_EXECUTE,
                    "Restart of a single unhealthy replica is auto-executable"
            );
            case "SCALE_DEPLOYMENT" -> new PolicyVerdict(
                    RiskClass.MEDIUM,
                    prod ? PolicyDecisionType.REQUIRE_APPROVAL : PolicyDecisionType.AUTO_EXECUTE,
                    prod ? "Scaling production requires approval" : "Non-prod scale is auto-executable within bounds"
            );
            case "ROLLBACK_DEPLOYMENT", "MODIFY_CONFIG" -> new PolicyVerdict(
                    RiskClass.HIGH,
                    PolicyDecisionType.REQUIRE_APPROVAL,
                    "Production rollback / config change requires human approval"
            );
            default -> new PolicyVerdict(
                    RiskClass.CRITICAL,
                    PolicyDecisionType.PROHIBITED,
                    "Unknown action is treated as prohibited: " + normalized
            );
        };
    }

    public static String normalize(String action) {
        if (action == null) {
            return "";
        }
        return action.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static boolean isProd(String environment, String namespace) {
        String env = environment == null ? "" : environment.toLowerCase(Locale.ROOT);
        String ns = namespace == null ? "" : namespace.toLowerCase(Locale.ROOT);
        return env.equals("prod") || env.equals("production") || ns.equals("prod") || ns.equals("production");
    }
}
