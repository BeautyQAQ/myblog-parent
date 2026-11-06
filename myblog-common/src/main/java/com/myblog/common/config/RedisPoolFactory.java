package com.myblog.common.config;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPool;

/** Constructs a pool without relying on third-party constructor parameter names. */
public final class RedisPoolFactory {
    private RedisPoolFactory() {}

    public static JedisPool create(String host, int port, String username, String password,
                                   int database, boolean ssl, int timeoutMillis) {
        var config = DefaultJedisClientConfig.builder()
                .database(database).ssl(ssl)
                .connectionTimeoutMillis(timeoutMillis).socketTimeoutMillis(timeoutMillis);
        if (username != null && !username.isBlank()) config.user(username);
        if (password != null && !password.isBlank()) config.password(password);
        return new JedisPool(new HostAndPort(host, port), config.build());
    }
}
