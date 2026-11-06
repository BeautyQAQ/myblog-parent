package com.myblog.portal.message;

import com.myblog.common.jedis.JedisClient;
import com.myblog.common.messaging.ArticleCacheEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

public class ArticleCacheInvalidationListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArticleCacheInvalidationListener.class);

    private final JedisClient jedisClient;
    private final String cacheKey;

    public ArticleCacheInvalidationListener(JedisClient jedisClient, String cacheKey) {
        this.jedisClient = jedisClient;
        this.cacheKey = cacheKey;
    }

    @KafkaListener(topics = "${kafka.topic}", groupId = "${kafka.consumer-group}")
    public void onMessage(String event, Acknowledgment acknowledgment) {
        if (ArticleCacheEvents.INVALIDATE.equals(event)) {
            // A new namespace invalidates every page/order without scanning a shared Redis.
            // Redelivery may increment again, which is safe; failures must not commit offsets.
            Long version = jedisClient.incr(cacheKey + ":version");
            LOGGER.info("Article cache invalidated: {}, version {}", cacheKey, version);
        } else {
            LOGGER.warn("Ignoring unrecognized article cache event: {}", event);
        }
        acknowledgment.acknowledge();
    }
}
