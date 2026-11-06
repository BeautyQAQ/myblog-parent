package com.myblog.config;

import com.myblog.common.messaging.KafkaConnectionProperties;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class KafkaProducerConfiguration {
    @Bean
    public ProducerFactory<String, String> kafkaProducerFactory(
            @Value("${kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${kafka.security-protocol}") String securityProtocol,
            @Value("${kafka.sasl-mechanism}") String saslMechanism,
            @Value("${kafka.sasl-jaas-config}") String saslJaasConfig) {
        Map<String, Object> properties = KafkaConnectionProperties.create(
                bootstrapServers, securityProtocol, saslMechanism, saslJaasConfig);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 10_000);
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10_000);
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30_000);
        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> kafkaProducerFactory) {
        return new KafkaTemplate<>(kafkaProducerFactory);
    }
}
