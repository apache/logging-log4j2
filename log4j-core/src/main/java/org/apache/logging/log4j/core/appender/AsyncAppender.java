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
package org.apache.logging.log4j.core.appender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.async.ArrayBlockingQueueFactory;
import org.apache.logging.log4j.core.async.AsyncQueueFullMessageUtil;
import org.apache.logging.log4j.core.async.AsyncQueueFullPolicy;
import org.apache.logging.log4j.core.async.BlockingQueueFactory;
import org.apache.logging.log4j.core.async.DiscardingAsyncQueueFullPolicy;
import org.apache.logging.log4j.core.async.EventRoute;
import org.apache.logging.log4j.core.async.InternalAsyncUtil;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.filter.AbstractFilterable;
import org.apache.logging.log4j.kit.logger.AbstractLogger;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAliases;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.validation.constraints.Required;
import org.apache.logging.log4j.util.InternalApi;

/**
 * Appends to one or more Appenders asynchronously. You can configure an AsyncAppender with one or more Appenders and an
 * Appender to append to if the queue is full. The AsyncAppender does not allow a filter to be specified on the Appender
 * references.
 */
@Configurable(elementType = Appender.ELEMENT_TYPE, printObject = true)
@Plugin("Async")
public final class AsyncAppender extends AbstractAppender {

    private static final int DEFAULT_QUEUE_SIZE = 1024;

    private final BlockingQueue<LogEvent> queue;
    private final int queueSize;
    private final boolean blocking;
    private final long shutdownTimeout;
    private final Configuration config;
    private final AppenderRef[] appenderRefs;
    private final String errorRef;
    private final boolean includeLocation;
    private AppenderControl errorAppender;
    private AsyncAppenderEventDispatcher dispatcher;
    private AsyncQueueFullPolicy asyncQueueFullPolicy;

    private AsyncAppender(
            final String name,
            final Filter filter,
            final AppenderRef[] appenderRefs,
            final String errorRef,
            final int queueSize,
            final boolean blocking,
            final boolean ignoreExceptions,
            final long shutdownTimeout,
            final Configuration config,
            final boolean includeLocation,
            final BlockingQueueFactory blockingQueueFactory,
            final Property[] properties) {
        super(name, filter, null, ignoreExceptions, properties);
        this.queue = blockingQueueFactory.create(queueSize);
        this.queueSize = queueSize;
        this.blocking = blocking;
        this.shutdownTimeout = shutdownTimeout;
        this.config = config;
        this.appenderRefs = appenderRefs;
        this.errorRef = errorRef;
        this.includeLocation = includeLocation;
    }

    @Override
    public void start() {
        final Map<String, Appender> map = config.getAppenders();
        final List<AppenderControl> appenders = new ArrayList<>();
        for (final AppenderRef appenderRef : appenderRefs) {
            final Appender appender = map.get(appenderRef.getRef());
            if (appender != null) {
                appenders.add(new AppenderControl(appender, appenderRef.getLevel(), appenderRef.getFilter()));
            } else {
                LOGGER.error("No appender named {} was configured", appenderRef);
            }
        }
        if (errorRef != null) {
            final Appender appender = map.get(errorRef);
            if (appender != null) {
                errorAppender = new AppenderControl(appender, null, null);
            } else {
                LOGGER.error("Unable to set up error Appender. No appender named {} was configured", errorRef);
            }
        }
        if (appenders.size() > 0) {
            dispatcher = new AsyncAppenderEventDispatcher(getName(), errorAppender, appenders, queue);
        } else if (errorRef == null) {
            throw new ConfigurationException("No appenders are available for AsyncAppender " + getName());
        }
        asyncQueueFullPolicy = config.getComponent(Key.forClass(AsyncQueueFullPolicy.class));

        dispatcher.start();
        super.start();
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        super.stop(timeout, timeUnit, false);
        LOGGER.trace("AsyncAppender stopping. Queue still has {} events.", queue.size());
        try {
            dispatcher.stop(shutdownTimeout);
        } catch (final InterruptedException ignored) {
            // Restore the interrupted flag cleared when the exception is caught.
            Thread.currentThread().interrupt();
            LOGGER.warn("Interrupted while stopping AsyncAppender {}", getName());
        }
        LOGGER.trace("AsyncAppender stopped. Queue has {} events.", queue.size());

        if (DiscardingAsyncQueueFullPolicy.getDiscardCount(asyncQueueFullPolicy) > 0) {
            LOGGER.trace(
                    "AsyncAppender: {} discarded {} events.",
                    asyncQueueFullPolicy,
                    DiscardingAsyncQueueFullPolicy.getDiscardCount(asyncQueueFullPolicy));
        }
        setStopped();
        return true;
    }

    /**
     * Actual writing occurs here.
     *
     * @param logEvent The LogEvent.
     */
    @Override
    public void append(final LogEvent logEvent) {
        if (!isStarted()) {
            throw new IllegalStateException("AsyncAppender " + getName() + " is not active");
        }
        // Modifications to the original `logEvent`:
        // 1. Format message, if it is not thread-safe.
        InternalAsyncUtil.makeMessageImmutable(logEvent.getMessage());
        // 2. Compute location, unless disabled.
        InternalAsyncUtil.makeLocationImmutable(this, logEvent);
        final LogEvent memento = logEvent.toMemento();
        if (!transfer(memento)) {
            if (blocking) {
                if (AbstractLogger.getRecursionDepth() > 1) { // LOG4J2-1518, LOG4J2-2031
                    // If queue is full AND we are in a recursive call, call appender directly to prevent deadlock
                    AsyncQueueFullMessageUtil.logWarningToStatusLogger();
                    logMessageInCurrentThread(logEvent);
                } else {
                    // delegate to the event router (which may discard, enqueue and block, or log in current thread)
                    final EventRoute route = asyncQueueFullPolicy.getRoute(dispatcher.getId(), memento.getLevel());
                    switch (route) {
                        case DISCARD:
                            break;
                        case ENQUEUE:
                            logMessageInBackgroundThread(memento);
                            break;
                        case SYNCHRONOUS:
                            logMessageInCurrentThread(memento);
                            break;
                        default:
                    }
                }
            } else {
                error("Appender " + getName() + " is unable to write primary appenders. queue is full");
                logToErrorAppenderIfNecessary(false, memento);
            }
        }
    }

    private boolean transfer(final LogEvent memento) {
        return queue instanceof TransferQueue
                ? ((TransferQueue<LogEvent>) queue).tryTransfer(memento)
                : queue.offer(memento);
    }

    /**
     * FOR INTERNAL USE ONLY.
     *
     * @param logEvent the event to log
     */
    @InternalApi
    public void logMessageInCurrentThread(final LogEvent logEvent) {
        logEvent.setEndOfBatch(queue.isEmpty());
        dispatcher.dispatch(logEvent);
    }

    /**
     * FOR INTERNAL USE ONLY.
     *
     * @param logEvent the event to log
     */
    @InternalApi
    public void logMessageInBackgroundThread(final LogEvent logEvent) {
        try {
            // wait for free slots in the queue
            queue.put(logEvent);
        } catch (final InterruptedException ignored) {
            final boolean appendSuccessful = handleInterruptedException(logEvent);
            logToErrorAppenderIfNecessary(appendSuccessful, logEvent);
        }
    }

    // LOG4J2-1049: Some applications use Thread.interrupt() to send
    // messages between application threads. This does not necessarily
    // mean that the queue is full. To prevent dropping a log message,
    // quickly try to offer the event to the queue again.
    // (Yes, this means there is a possibility the same event is logged twice.)
    //
    // Finally, catching the InterruptedException means the
    // interrupted flag has been cleared on the current thread.
    // This may interfere with the application's expectation of
    // being interrupted, so when we are done, we set the interrupted
    // flag again.
    private boolean handleInterruptedException(final LogEvent memento) {
        final boolean appendSuccessful = queue.offer(memento);
        if (!appendSuccessful) {
            LOGGER.warn("Interrupted while waiting for a free slot in the AsyncAppender LogEvent-queue {}", getName());
        }
        // set the interrupted flag again.
        Thread.currentThread().interrupt();
        return appendSuccessful;
    }

    private void logToErrorAppenderIfNecessary(final boolean appendSuccessful, final LogEvent logEvent) {
        if (!appendSuccessful && errorAppender != null) {
            errorAppender.callAppender(logEvent);
        }
    }

    @Factory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends AbstractFilterable.Builder<Builder>
            implements org.apache.logging.log4j.plugins.util.Builder<AsyncAppender> {

        @PluginElement("AppenderRef")
        @Required(message = "No appender references provided to AsyncAppender")
        private AppenderRef[] appenderRefs;

        @PluginBuilderAttribute
        @PluginAliases("error-ref")
        private String errorRef;

        @PluginBuilderAttribute
        private boolean blocking = true;

        @PluginBuilderAttribute
        private long shutdownTimeout = 0L;

        @PluginBuilderAttribute
        private int bufferSize = DEFAULT_QUEUE_SIZE;

        /**
         * Name of the appender to use in references
         */
        @PluginBuilderAttribute
        @Required(message = "No name provided for AsyncAppender")
        private String name;

        @PluginBuilderAttribute
        private boolean includeLocation = false;

        @PluginConfiguration
        private Configuration configuration;

        @PluginBuilderAttribute
        private boolean ignoreExceptions = true;

        @PluginElement(BlockingQueueFactory.ELEMENT_TYPE)
        private BlockingQueueFactory blockingQueueFactory = new ArrayBlockingQueueFactory();

        public Builder setAppenderRefs(final AppenderRef[] appenderRefs) {
            this.appenderRefs = appenderRefs;
            return this;
        }

        public Builder setErrorRef(final String errorRef) {
            this.errorRef = errorRef;
            return this;
        }

        public Builder setBlocking(final boolean blocking) {
            this.blocking = blocking;
            return this;
        }

        public Builder setShutdownTimeout(final long shutdownTimeout) {
            this.shutdownTimeout = shutdownTimeout;
            return this;
        }

        public Builder setBufferSize(final int bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        public Builder setName(final String name) {
            this.name = name;
            return this;
        }

        public Builder setIncludeLocation(final boolean includeLocation) {
            this.includeLocation = includeLocation;
            return this;
        }

        public Builder setConfiguration(final Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder setIgnoreExceptions(final boolean ignoreExceptions) {
            this.ignoreExceptions = ignoreExceptions;
            return this;
        }

        public Builder setBlockingQueueFactory(final BlockingQueueFactory blockingQueueFactory) {
            this.blockingQueueFactory = blockingQueueFactory;
            return this;
        }

        @Override
        public AsyncAppender build() {
            return new AsyncAppender(
                    name,
                    getFilter(),
                    appenderRefs,
                    errorRef,
                    bufferSize,
                    blocking,
                    ignoreExceptions,
                    shutdownTimeout,
                    configuration,
                    includeLocation,
                    blockingQueueFactory,
                    getPropertyArray());
        }
    }

    /**
     * Returns the names of the appenders that this asyncAppender delegates to as an array of Strings.
     *
     * @return the names of the sink appenders
     */
    public String[] getAppenderRefStrings() {
        final String[] result = new String[appenderRefs.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = appenderRefs[i].getRef();
        }
        return result;
    }

    /**
     * Returns {@code true} if this AsyncAppender will take a snapshot of the stack with every log event to determine
     * the class and method where the logging call was made.
     *
     * @return {@code true} if location is included with every event, {@code false} otherwise
     */
    public boolean isIncludeLocation() {
        return includeLocation;
    }

    /**
     * Returns {@code true} if this AsyncAppender will block when the queue is full, or {@code false} if events are
     * dropped when the queue is full.
     *
     * @return whether this AsyncAppender will block or drop events when the queue is full.
     */
    public boolean isBlocking() {
        return blocking;
    }

    /**
     * Returns the name of the appender that any errors are logged to or {@code null}.
     *
     * @return the name of the appender that any errors are logged to or {@code null}
     */
    public String getErrorRef() {
        return errorRef;
    }

    public int getQueueCapacity() {
        return queueSize;
    }

    public int getQueueRemainingCapacity() {
        return queue.remainingCapacity();
    }

    /**
     * Returns the number of elements in the queue.
     *
     * @return the number of elements in the queue.
     * @since 2.11.1
     */
    public int getQueueSize() {
        return queue.size();
    }

    @Override
    public boolean requiresLocation() {
        return includeLocation && dispatcher.requiresLocation();
    }
}
