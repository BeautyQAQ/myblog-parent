package com.myblog.common.jedis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;

public class JedisClientPool implements JedisClient {
	
	private JedisPool jedisPool;

	public JedisPool getJedisPool() {
		return jedisPool;
	}

	public void setJedisPool(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
	}

    @Override
    public String setex(String key, int seconds, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.setex(key, seconds, value);
        }
    }

	@Override
	public String set(String key, String value) {
		try (Jedis jedis = jedisPool.getResource()) {
			String result = jedis.set(key, value);
			return result;
		}
	}

	@Override
	public String get(String key) {
		try (Jedis jedis = jedisPool.getResource()) {
			String result = jedis.get(key);
			return result;
		}
	}

	@Override
	public Boolean exists(String key) {
		try (Jedis jedis = jedisPool.getResource()) {
			Boolean result = jedis.exists(key);
			return result;
		}
	}

	@Override
	public Long expire(String key, int seconds) {
		try (Jedis jedis = jedisPool.getResource()) {
			Long result = jedis.expire(key, seconds);
			return result;
		}
	}

	@Override
	public Long ttl(String key) {
		try (Jedis jedis = jedisPool.getResource()) {
			Long result = jedis.ttl(key);
			return result;
		}
	}

	@Override
	public Long incr(String key) {
		try (Jedis jedis = jedisPool.getResource()) {
			Long result = jedis.incr(key);
			return result;
		}
	}

	@Override
	public Long hset(String key, String field, String value) {
		try (Jedis jedis = jedisPool.getResource()) {
			Long result = jedis.hset(key, field, value);
			return result;
		}
	}

	@Override
	public String hget(String key, String field) {
		try (Jedis jedis = jedisPool.getResource()) {
			String result = jedis.hget(key, field);
			return result;
		}
	}

	@Override
	public Long hdel(String key, String... field) {
		try (Jedis jedis = jedisPool.getResource()) {
			Long result = jedis.hdel(key, field);
			return result;
		}
	}

	@Override
	public Boolean hexists(String key, String field) {
		try (Jedis jedis = jedisPool.getResource()) {
			Boolean result = jedis.hexists(key, field);
			return result;
		}
	}

	@Override
	public List<String> hvals(String key) {
		try (Jedis jedis = jedisPool.getResource()) {
			List<String> result = jedis.hvals(key);
			return result;
		}
	}

	@Override
	public Long del(String key) {
		try (Jedis jedis = jedisPool.getResource()) {
			Long result = jedis.del(key);
			return result;
		}
	}

}
