package com.myblog.portal.config;

import com.myblog.common.jedis.JedisClient;
import com.myblog.common.messaging.KafkaConnectionProperties;
import com.myblog.portal.message.ArticleCacheInvalidationListener;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableKafka
public class KafkaConsumerConfiguration {
    @Bean
    public ConsumerFactory<String, String> kafkaConsumerFactory(
            @Value("${kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${kafka.security-protocol}") String securityProtocol,
            @Value("${kafka.sasl-mechanism}") String saslMechanism,
            @Value("${kafka.sasl-jaas-config}") String saslJaasConfig,
            @Value("${kafka.consumer-group}") String consumerGroup) {
        Map<String, Object> properties = KafkaConnectionProperties.create(
                bootstrapServers, securityProtocol, saslMechanism, saslJaasConfig);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(properties);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> kafkaConsumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(kafkaConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.getContainerProperties().setSyncCommits(true);
        // Redis outages must not turn into skipped (and committed) invalidation records.
        var errorHandler = new DefaultErrorHandler(new FixedBackOff(1_000, FixedBackOff.UNLIMITED_ATTEMPTS));
        errorHandler.setAckAfterHandle(false);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    @Bean
    public ArticleCacheInvalidationListener articleCacheInvalidationListener(
            JedisClient jedisClient, @Value("${CONTENT_LIST}") String cacheKey) {
        return new ArticleCacheInvalidationListener(jedisClient, cacheKey);
    }
}
