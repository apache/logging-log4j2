package org.apache.logging.log4j.redis.appender;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

class RedisManager extends AbstractManager {

    private final byte[][] byteKeys;
    private final String host;
    private final int port;
    private final Charset charset;
    private final boolean ssl;
    private JedisPool jedisPool;

    RedisManager(LoggerContext loggerContext, String name, String[] keys, String host, int port, boolean ssl, Charset charset) {
        super(loggerContext, name);
        this.byteKeys = new byte[keys.length][];
        for (int i = 0; i < keys.length; i++) {
            this.byteKeys[i] = keys[i].getBytes(charset);
        }
        this.charset = charset;
        this.host = host;
        this.port = port;
        this.ssl = ssl;
    }

    JedisPool createPool(String host, int port, boolean ssl) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setNumTestsPerEvictionRun(10);
        poolConfig.setTimeBetweenEvictionRunsMillis(60000);
        return new JedisPool(poolConfig, host, port, ssl);
    }

    public void startup() {
        jedisPool = createPool(host, port, ssl);
    }

    public void send(byte[] value) {
        try (Jedis jedis = jedisPool.getResource()){
            for (byte[] key: byteKeys) {
                jedis.rpush(key, value);
            }
        } catch (JedisConnectionException e) {
            LOGGER.error("Unable to connect to redis. Please ensure that it's running on {}:{}", host, port, e);
        }
    }

    @Override
    protected boolean releaseSub(final long timeout, final TimeUnit timeUnit) {
        if (jedisPool != null) {
            jedisPool.destroy();
        }
        return true;
    }

    String getHost() {
        return host;
    }

    int getPort() {
        return port;
    }

    String getKeysAsString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < byteKeys.length; i++) {
            sb.append(new String(byteKeys[i], charset));
            if (i != byteKeys.length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}
