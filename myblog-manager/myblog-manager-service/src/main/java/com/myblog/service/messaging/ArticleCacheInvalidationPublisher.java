package com.myblog.service.messaging;

import com.myblog.common.messaging.ArticleCacheEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class ArticleCacheInvalidationPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArticleCacheInvalidationPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public ArticleCacheInvalidationPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                            @Value("${kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publishAfterCommit() {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendInvalidation();
                }
            });
        } else {
            sendInvalidation();
        }
    }

    private void sendInvalidation() {
        try {
            kafkaTemplate.send(topic, ArticleCacheEvents.KEY, ArticleCacheEvents.INVALIDATE)
                    .get(35, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw publishFailure(e);
        } catch (ExecutionException | TimeoutException | RuntimeException e) {
            throw publishFailure(e);
        }
    }

    private IllegalStateException publishFailure(Exception cause) {
        // The DB transaction is already committed: never report this as a DB rollback.
        LOGGER.error("Article change committed, but Kafka cache invalidation failed for topic {}. "
                + "The cache may remain stale until expiry; retry invalidation.", topic, cause);
        return new IllegalStateException("文章变更已提交，但缓存失效消息发送失败，请重试清理缓存", cause);
    }
}
