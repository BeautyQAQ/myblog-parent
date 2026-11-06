package com.myblog.portal.message;

import com.myblog.common.jedis.JedisClient;
import com.myblog.common.messaging.ArticleCacheEvents;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class ArticleCacheInvalidationListenerTest {
    @Test
    void advancesTheCacheNamespaceBeforeAcknowledging() {
        AtomicLong version = new AtomicLong();
        JedisClient redis = redis((key) -> {
            assertEquals("test:articles:version", key);
            return version.incrementAndGet();
        });
        var listener = new ArticleCacheInvalidationListener(redis, "test:articles");
        AtomicBoolean acknowledged = new AtomicBoolean();

        listener.onMessage(ArticleCacheEvents.INVALIDATE, () -> {
            assertEquals(1, version.get());
            acknowledged.set(true);
        });

        assertTrue(acknowledged.get());
    }

    @Test
    void redisFailureDoesNotAcknowledgeTheRecord() {
        JedisClient redis = redis(key -> { throw new IllegalStateException("Redis unavailable"); });
        var listener = new ArticleCacheInvalidationListener(redis, "test:articles");
        AtomicBoolean acknowledged = new AtomicBoolean();

        assertThrows(IllegalStateException.class,
                () -> listener.onMessage(ArticleCacheEvents.INVALIDATE, () -> acknowledged.set(true)));

        assertFalse(acknowledged.get());
    }

    private static JedisClient redis(java.util.function.Function<String, Long> increment) {
        return (JedisClient) Proxy.newProxyInstance(JedisClient.class.getClassLoader(),
                new Class<?>[]{JedisClient.class}, (proxy, method, args) -> {
                    if ("incr".equals(method.getName())) {
                        return increment.apply((String) args[0]);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
