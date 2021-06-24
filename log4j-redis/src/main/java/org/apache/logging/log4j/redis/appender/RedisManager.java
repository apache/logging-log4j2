/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

package org.apache.logging.log4j.redis.appender;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * AutoCloseable wrapper class around a Redis client connection.
 * Enables the transport of data to Redis lists via RPUSH to preconfigured keys.
 */
class RedisManager extends AbstractManager {

    private final String[] keys;
    private final String host;
    private final int port;
    private final SslConfiguration sslConfiguration;
    private final JedisPoolConfig poolConfiguration;
    private JedisPool jedisPool;

    RedisManager(final LoggerContext loggerContext, final String name, final String[] keys, final String host, final int port,
                 final SslConfiguration sslConfiguration, final LoggingRedisPoolConfiguration poolConfiguration) {
        super(loggerContext, name);
        this.keys = keys;
        this.host = host;
        this.port = port;
        this.sslConfiguration = sslConfiguration;
        this.poolConfiguration = Objects.requireNonNullElseGet(poolConfiguration, LoggingRedisPoolConfiguration::defaultConfiguration);
    }

    JedisPool createPool(final String host, final int port, final SslConfiguration sslConfiguration) {
        if (sslConfiguration != null) {
            return new JedisPool(
                    poolConfiguration,
                    URI.create(host + ":" + port),
                    sslConfiguration.getSslSocketFactory(),
                    sslConfiguration.getSslContext().getSupportedSSLParameters(),
                    null
            );
        } else {
            return new JedisPool(poolConfiguration, host, port, false);
        }

    }

    public void startup() {
        jedisPool = createPool(host, port, sslConfiguration);
    }

    public void sendBulk(final List<String> logEvents) {
        try (final Jedis jedis = jedisPool.getResource()) {
            if (!logEvents.isEmpty()) {
                send(jedis, logEvents.toArray(new String[0]));
            }
        } catch (final JedisConnectionException e) {
            LOGGER.error("Unable to connect to redis. Please ensure that it's running on {}:{}", host, port, e);
        }
    }

    public void send(final String value) {
        try (final Jedis jedis = jedisPool.getResource()) {
            try {
                send(jedis, value);
            } catch (final JedisConnectionException e) {
                LOGGER.error("Unable to connect to redis. Please ensure that it's running on {}:{}", host, port, e);
            }
        }
    }

    private void send(final Jedis jedis, final String... value) {
        for (final String key: keys) {
            jedis.rpush(key, value);
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
        return String.join(",", keys);
    }
}
