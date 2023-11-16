/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.appender.mom.kafka;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.layout.SerializedLayout;
import org.apache.logging.log4j.core.util.Integers;

/**
 * Sends log events to an Apache Kafka topic.
 */
@Plugin(name = "Kafka", category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class KafkaAppender extends AbstractAppender {

    /**
     * Builds KafkaAppender instances.
     *
     * @param <B>
     *            The type to build
     */
    public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<KafkaAppender> {

        @PluginAttribute("retryCount")
        private int retryCount;

        @PluginAttribute("topic")
        private String topic;

        @PluginAttribute("key")
        private String key;

        @PluginAttribute(value = "syncSend", defaultBoolean = true)
        private boolean syncSend;

        @PluginAttribute(value = "sendEventTimestamp", defaultBoolean = false)
        private boolean sendEventTimestamp;

        @SuppressWarnings("resource")
        @Override
        public KafkaAppender build() {
            final Layout<? extends Serializable> layout = getLayout();
            if (layout == null) {
                AbstractLifeCycle.LOGGER.error("No layout provided for KafkaAppender");
                return null;
            }
            final KafkaManager kafkaManager = KafkaManager.getManager(
                    getConfiguration().getLoggerContext(),
                    getName(),
                    topic,
                    syncSend,
                    sendEventTimestamp,
                    getPropertyArray(),
                    key);
            return new KafkaAppender(
                    getName(),
                    layout,
                    getFilter(),
                    isIgnoreExceptions(),
                    kafkaManager,
                    getPropertyArray(),
                    getRetryCount());
        }

        public Integer getRetryCount() {
            Integer intRetryCount = null;
            try {
                intRetryCount = Integer.valueOf(retryCount);
            } catch (NumberFormatException e) {

            }
            return intRetryCount;
        }

        public String getTopic() {
            return topic;
        }

        public boolean isSendEventTimestamp() {
            return sendEventTimestamp;
        }

        public boolean isSyncSend() {
            return syncSend;
        }

        public B setKey(final String key) {
            this.key = key;
            return asBuilder();
        }

        @Deprecated
        public B setRetryCount(final String retryCount) {
            this.retryCount = Integers.parseInt(retryCount, 0);
            return asBuilder();
        }

        public B setRetryCount(final int retryCount) {
            this.retryCount = retryCount;
            return asBuilder();
        }

        public B setSendEventTimestamp(final boolean sendEventTimestamp) {
            this.sendEventTimestamp = sendEventTimestamp;
            return asBuilder();
        }

        public B setSyncSend(final boolean syncSend) {
            this.syncSend = syncSend;
            return asBuilder();
        }

        public B setTopic(final String topic) {
            this.topic = topic;
            return asBuilder();
        }
    }

    private static final String[] KAFKA_CLIENT_PACKAGES =
            new String[] {"org.apache.kafka.common", "org.apache.kafka.clients"};

    @Deprecated
    public static KafkaAppender createAppender(
            final Layout<? extends Serializable> layout,
            final Filter filter,
            final String name,
            final boolean ignoreExceptions,
            final String topic,
            final Property[] properties,
            final Configuration configuration,
            final String key) {

        if (layout == null) {
            AbstractLifeCycle.LOGGER.error("No layout provided for KafkaAppender");
            return null;
        }
        final KafkaManager kafkaManager =
                KafkaManager.getManager(configuration.getLoggerContext(), name, topic, true, properties, key);
        return new KafkaAppender(name, layout, filter, ignoreExceptions, kafkaManager, null, 0);
    }

    /**
     * Tests if the given log event is from a Kafka Producer implementation.
     *
     * @param event The event to test.
     * @return true to avoid recursion and skip logging, false to log.
     */
    private static boolean isRecursive(final LogEvent event) {
        return Stream.of(KAFKA_CLIENT_PACKAGES)
                .anyMatch(prefix -> event.getLoggerName().startsWith(prefix));
    }

    /**
     * Creates a builder for a KafkaAppender.
     *
     * @return a builder for a KafkaAppender.
     */
    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    private final Integer retryCount;

    private final KafkaManager manager;

    private KafkaAppender(
            final String name,
            final Layout<? extends Serializable> layout,
            final Filter filter,
            final boolean ignoreExceptions,
            final KafkaManager manager,
            final Property[] properties,
            final int retryCount) {
        super(name, filter, layout, ignoreExceptions, properties);
        this.manager = Objects.requireNonNull(manager, "manager");
        this.retryCount = retryCount;
    }

    @Override
    public void append(final LogEvent event) {
        if (event.getLoggerName() != null && isRecursive(event)) {
            LOGGER.warn("Recursive logging from [{}] for appender [{}].", event.getLoggerName(), getName());
        } else {
            try {
                tryAppend(event);
            } catch (final Exception e) {

                if (this.retryCount != null) {
                    int currentRetryAttempt = 0;
                    while (currentRetryAttempt < this.retryCount) {
                        currentRetryAttempt++;
                        try {
                            tryAppend(event);
                            break;
                        } catch (Exception e1) {

                        }
                    }
                }
                error("Unable to write to Kafka in appender [" + getName() + "]", event, e);
            }
        }
    }

    @Override
    public void start() {
        super.start();
        manager.startup();
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
        return "KafkaAppender{" + "name=" + getName() + ", state=" + getState() + ", topic=" + manager.getTopic() + '}';
    }

    private void tryAppend(final LogEvent event) throws ExecutionException, InterruptedException, TimeoutException {
        final Layout<? extends Serializable> layout = getLayout();
        byte[] data;
        if (layout instanceof SerializedLayout) {
            final byte[] header = layout.getHeader();
            final byte[] body = layout.toByteArray(event);
            data = new byte[header.length + body.length];
            System.arraycopy(header, 0, data, 0, header.length);
            System.arraycopy(body, 0, data, header.length, body.length);
        } else {
            data = layout.toByteArray(event);
        }
        manager.send(data, event.getTimeMillis());
    }
}
