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

import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.spi.AbstractLogger;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Sends log events to a Redis Queue. All logs are appended to Redis lists via the RPUSH command.
 */
@Plugin(name = "Redis", category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class RedisAppender extends AbstractAppender {

    private final RedisManager manager;
    private final LinkedBlockingQueue<String> logQueue;
    private final boolean immediateFlush;
    private final int queueCapacity;

    private RedisAppender(final String name, final Layout<? extends Serializable> layout, final Filter filter,
                          final boolean ignoreExceptions, boolean immediateFlush, final int queueCapacity, final RedisManager manager) {
        super(name, filter, layout, ignoreExceptions);
        this.manager = Objects.requireNonNull(manager, "Redis Manager");
        this.immediateFlush = immediateFlush;
        this.queueCapacity = queueCapacity;
        this.logQueue = new LinkedBlockingQueue<>(queueCapacity);
    }

    /**
     * Builds RedisAppender instances.
     * @param <B> The type to build
     */
    public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<RedisAppender> {

        @PluginAttribute("keys")
        private String[] keys;

        @PluginAttribute(value = "host")
        @Required(message = "No Redis hostname provided")
        private String host;

        @PluginAttribute(value = "port")
        private int port = 6379;

        @PluginAttribute(value = "immediateFlush")
        private boolean immediateFlush = true;

        @PluginAttribute(value = "queueCapacity")
        private int queueCapacity = 20;

        @PluginAttribute(value = "maxRetries")
        private int maxRetries = 3;

        @PluginAttribute(value = "msBetweenRetries")
        private long msBetweenRetries = 5000L;

        @PluginElement("SslConfiguration")
        private SslConfiguration sslConfiguration;

        @PluginElement("PoolConfiguration")
        private LoggingJedisPoolConfiguration poolConfiguration = LoggingJedisPoolConfiguration.defaultConfiguration();

        @SuppressWarnings("resource")
        @Override
        public RedisAppender build() {
            return new RedisAppender(
                    getName(),
                    getLayout(),
                    getFilter(),
                    isIgnoreExceptions(),
                    isImmediateFlush(),
                    getQueueCapacity(),
                    getRedisManager()
            );
        }

        String[] getKeys() {
            return keys;
        }

        String getHost() {
            return host;
        }

        int getQueueCapacity() {
            return queueCapacity;
        }

        boolean isImmediateFlush() {
            return immediateFlush;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public long getMsBetweenRetries() {
            return msBetweenRetries;
        }

        SslConfiguration getSslConfiguration() {
            return sslConfiguration;
        }

        LoggingJedisPoolConfiguration getPoolConfiguration() {
            return poolConfiguration;
        }

        int getPort() {
            return port;
        }

        public B setKeys(final String key) {
            this.keys = new String[]{key};
            return asBuilder();
        }

        public B setKeys(final String[] keys) {
            this.keys = keys;
            return asBuilder();
        }

        public B setHost(final String host) {
            this.host = host;
            return asBuilder();
        }

        public B setPort(final int port) {
            this.port = port;
            return asBuilder();
        }

        public B setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return asBuilder();
        }

        public void setMsBetweenRetries(long msBetweenRetries) {
            this.msBetweenRetries = msBetweenRetries;
        }

        public B setQueueCapacity(final int queueCapacity) {
            this.queueCapacity = queueCapacity;
            return asBuilder();
        }

        public B setPoolConfiguration(final LoggingJedisPoolConfiguration poolConfiguration) {
            this.poolConfiguration = poolConfiguration;
            return asBuilder();
        }

        public B setSslConfiguration(final SslConfiguration ssl) {
            this.sslConfiguration = ssl;
            return asBuilder();
        }

        public B setImmediateFlush(final boolean immediateFlush) {
            this.immediateFlush = immediateFlush;
            return asBuilder();
        }

        RedisManager getRedisManager() {
            return new RedisManager(
                    getConfiguration().getLoggerContext(),
                    getName(),
                    getKeys(),
                    getHost(),
                    getPort(),
                    getMaxRetries(),
                    getMsBetweenRetries(),
                    getSslConfiguration(),
                    getPoolConfiguration()
            );
        }
    }

    /**
     * Creates a builder for a RedisAppender.
     * @return a builder for a RedisAppender.
     */
    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    @Override
    public void append(final LogEvent event) {
        final Layout<? extends Serializable> layout = getLayout();
        if (event.getLoggerName() != null && AbstractLogger.getRecursionDepth() > 1) {
            LOGGER.warn("Recursive logging from [{}] for appender [{}].", event.getLoggerName(), getName());
        } else if (layout instanceof StringLayout) {
            logQueue.add(((StringLayout)layout).toSerializable(event));
            if (shouldFlushLogQueue(event.isEndOfBatch())) {
                try {
                    tryFlushQueue();
                } catch (final Exception e) {
                    error("Unable to write to Redis in appender [" + getName() + "]", event, e);
                }
            }
        } else {
            throw new AppenderLoggingException("The Redis appender only supports StringLayouts.");
        }
    }

    private boolean shouldFlushLogQueue(boolean endOfBatch) {
        return immediateFlush || endOfBatch || logQueue.size() >= queueCapacity;
    }

    private void tryFlushQueue() {
        manager.sendBulk(logQueue);
    }

    @Override
    public void start() {
        setStarting();
        manager.startup();
        setStarted();
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        if (logQueue.size() > 0) {
            tryFlushQueue();
        }
        boolean stopped = super.stop(timeout, timeUnit, false);
        stopped &= manager.stop(timeout, timeUnit);
        setStopped();
        return stopped;
    }

    @Override
    public String toString() {
        return "RedisAppender{" +
            "name=" + getName() +
            ", host=" + manager.getHost() +
            ", port=" + manager.getPort() +
            ", keys=" + manager.getKeysAsString() +
            ", immediateFlush=" + this.immediateFlush +
            '}';
    }
}
