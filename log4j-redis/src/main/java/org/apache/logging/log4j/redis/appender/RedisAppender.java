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

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Sends log events to a Redis Queue. All logs are appended Redis lists via the RPUSH command.
 */
@Plugin(name = "Redis", category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class RedisAppender extends AbstractAppender {

    private RedisAppender(final String name, final Layout<? extends Serializable> layout, final Filter filter,
                          final boolean ignoreExceptions, final RedisManager manager) {
        super(name, filter, layout, ignoreExceptions);
        this.manager = Objects.requireNonNull(manager, "Redis Manager");
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
        private int port;

        @PluginAttribute(value = "ssl")
        private boolean ssl = false;

        @SuppressWarnings("resource")
        @Override
        public RedisAppender build() {
            return new RedisAppender(getName(), getLayout(), getFilter(), isIgnoreExceptions(), getRedisManager());
        }

        public Charset getCharset() {
            if (getLayout() instanceof AbstractStringLayout) {
                return ((AbstractStringLayout) getLayout()).getCharset();
            } else {
                return Charset.defaultCharset();
            }
        }

        String[] getKeys() {
            return keys;
        }

        String getHost() {
            return host;
        }

        boolean getSsl() {
            return ssl;
        }

        int getPort() {
            return port;
        }

        public B withKeys(final String key) {
            this.keys = new String[]{key};
            return asBuilder();
        }

        public B withKeys(final String[] keys) {
            this.keys = keys;
            return asBuilder();
        }

        public B withHost(final String host) {
            this.host = host;
            return asBuilder();
        }

        public B withPort(final int port) {
            this.port = port;
            return asBuilder();
        }

        public B withSsl(final boolean ssl) {
            this.ssl = ssl;
            return asBuilder();
        }

        RedisManager getRedisManager() {
            return new RedisManager(
                    getConfiguration().getLoggerContext(),
                    getName(),
                    getKeys(),
                    getHost(),
                    getPort(),
                    getSsl(),
                    getCharset()
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

    private final RedisManager manager;

    @Override
    public void append(final LogEvent event) {
        if (event.getLoggerName() != null && event.getLoggerName().startsWith("org.apache.redis")) {
            LOGGER.warn("Recursive logging from [{}] for appender [{}].", event.getLoggerName(), getName());
        } else {
            try {
                tryAppend(event);
            } catch (final Exception e) {
                error("Unable to write to Redis in appender [" + getName() + "]", event, e);
            }
        }
    }

    private void tryAppend(final LogEvent event) {
        final Layout<? extends Serializable> layout = getLayout();
        final byte[] header = layout.getHeader();
        final byte[] body = layout.toByteArray(event);

        int len = (header != null ? header.length : 0) + body.length;
        byte[] data = new byte[len];
        if (header != null) {
            System.arraycopy(header, 0, data, 0, header.length);
            System.arraycopy(body, 0, data, header.length, body.length);
        } else {
            System.arraycopy(body, 0, data, 0, body.length);
        }
        manager.send(data);
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
            '}';
    }
}
