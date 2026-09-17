package com.irp.remediation.k8s;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-cluster stand-in for the Kubernetes Apps API. Validates allowlists and
 * revision history before mutating demo state. No kubectl, no shell.
 */
@Component
public class SimulatedKubernetesClient {

    public static final Set<String> NAMESPACE_ALLOWLIST = Set.of("prod", "production", "staging");
    public static final Set<String> DEPLOYMENT_ALLOWLIST = Set.of("payment-service");

    private final Map<String, Integer> currentRevision = new ConcurrentHashMap<>();

    public SimulatedKubernetesClient() {
        currentRevision.put(key("prod", "payment-service"), 42);
        currentRevision.put(key("production", "payment-service"), 42);
        currentRevision.put(key("staging", "payment-service"), 42);
    }

    public String rollback(String namespace, String deployment, int targetRevision) {
        assertAllowlisted(namespace, deployment);
        String k = key(namespace, deployment);
        Integer current = currentRevision.get(k);
        if (current == null) {
            throw new IllegalArgumentException("Unknown deployment " + k);
        }
        if (targetRevision < 1 || targetRevision >= current) {
            throw new IllegalArgumentException(
                    "Invalid target revision " + targetRevision + " (current=" + current + ")");
        }
        currentRevision.put(k, targetRevision);
        return "rolled back " + k + " " + current + " -> " + targetRevision;
    }

    public String restartPod(String namespace, String deployment) {
        assertAllowlisted(namespace, deployment);
        return "restarted one replica of " + key(namespace, deployment);
    }

    public int currentRevision(String namespace, String deployment) {
        Integer revision = currentRevision.get(key(namespace, deployment));
        return revision == null ? -1 : revision;
    }

    private static void assertAllowlisted(String namespace, String deployment) {
        if (!NAMESPACE_ALLOWLIST.contains(namespace)) {
            throw new IllegalArgumentException("Namespace not allowlisted: " + namespace);
        }
        if (!DEPLOYMENT_ALLOWLIST.contains(deployment)) {
            throw new IllegalArgumentException("Deployment not allowlisted: " + deployment);
        }
    }

    private static String key(String namespace, String deployment) {
        return namespace + "/" + deployment;
    }
}
