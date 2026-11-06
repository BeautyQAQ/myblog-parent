package com.myblog.common.messaging;

import java.util.HashMap;
import java.util.Map;

/** Connection settings shared by producers and consumers, without framework dependencies. */
public final class KafkaConnectionProperties {
    private KafkaConnectionProperties() {
    }

    public static Map<String, Object> create(String bootstrapServers, String securityProtocol,
                                             String saslMechanism, String saslJaasConfig) {
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            throw new IllegalArgumentException("kafka.bootstrap-servers must be configured");
        }
        if (securityProtocol == null || securityProtocol.isBlank()) {
            throw new IllegalArgumentException("kafka.security-protocol must be configured");
        }
        String protocol = securityProtocol.trim();
        Map<String, Object> properties = new HashMap<>();
        properties.put("bootstrap.servers", bootstrapServers.trim());
        properties.put("security.protocol", protocol);
        if (protocol.startsWith("SASL_")) {
            if (saslMechanism != null && !saslMechanism.isBlank()) {
                properties.put("sasl.mechanism", saslMechanism.trim());
            }
            if (saslJaasConfig != null && !saslJaasConfig.isBlank()) {
                properties.put("sasl.jaas.config", saslJaasConfig);
            }
        }
        return properties;
    }
}
