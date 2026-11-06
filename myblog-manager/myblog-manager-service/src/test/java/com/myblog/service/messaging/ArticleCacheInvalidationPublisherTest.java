package com.myblog.service.messaging;

import com.myblog.common.messaging.ArticleCacheEvents;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.*;

class ArticleCacheInvalidationPublisherTest {
    private final MockProducer<String, String> producer =
            new MockProducer<>(true, null, new StringSerializer(), new StringSerializer());
    private final ProducerFactory<String, String> factory = () -> producer;
    private final ArticleCacheInvalidationPublisher publisher =
            new ArticleCacheInvalidationPublisher(new KafkaTemplate<>(factory), "test.article-cache");

    @AfterEach
    void clearTransactionState() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.clear();
    }

    @Test
    void publishesOnlyAfterTheDatabaseCommit() {
        beginTransaction();
        publisher.publishAfterCommit();
        assertTrue(producer.history().isEmpty());

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        assertEquals(1, producer.history().size());
        assertEquals("test.article-cache", producer.history().getFirst().topic());
        assertEquals(ArticleCacheEvents.KEY, producer.history().getFirst().key());
        assertEquals(ArticleCacheEvents.INVALIDATE, producer.history().getFirst().value());
    }

    @Test
    void aRolledBackDatabaseChangeDoesNotInvalidateTheCache() {
        beginTransaction();
        publisher.publishAfterCommit();
        TransactionSynchronizationManager.getSynchronizations().forEach(
                synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        assertTrue(producer.history().isEmpty());
    }

    @Test
    void aSendFailureIsPropagatedWithTheCommittedDatabaseState() {
        producer.sendException = new KafkaException("broker unavailable");
        IllegalStateException failure = assertThrows(IllegalStateException.class, publisher::publishAfterCommit);
        assertTrue(failure.getMessage().contains("变更已提交"));
        assertNotNull(failure.getCause());
    }

    private static void beginTransaction() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }
}
