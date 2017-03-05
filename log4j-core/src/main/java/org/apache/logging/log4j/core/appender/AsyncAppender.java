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
package org.apache.logging.log4j.core.appender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.core.AbstractLogEvent;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.async.ArrayBlockingQueueFactory;
import org.apache.logging.log4j.core.async.AsyncQueueFullPolicy;
import org.apache.logging.log4j.core.async.AsyncQueueFullPolicyFactory;
import org.apache.logging.log4j.core.async.BlockingQueueFactory;
import org.apache.logging.log4j.core.async.DiscardingAsyncQueueFullPolicy;
import org.apache.logging.log4j.core.async.EventRoute;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.Log4jThread;
import org.apache.logging.log4j.message.AsynchronouslyFormattable;
import org.apache.logging.log4j.message.Message;

/**
 * Appends to one or more Appenders asynchronously. You can configure an AsyncAppender with one or more Appenders and an
 * Appender to append to if the queue is full. The AsyncAppender does not allow a filter to be specified on the Appender
 * references.
 */
@Plugin(name = "Async", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class AsyncAppender extends AbstractAppender {

    private static final int DEFAULT_QUEUE_SIZE = 128;
    private static final LogEvent SHUTDOWN_LOG_EVENT = new AbstractLogEvent() {
    };

    private static final AtomicLong THREAD_SEQUENCE = new AtomicLong(1);

    private final BlockingQueue<LogEvent> queue;
    private final int queueSize;
    private final boolean blocking;
    private final long shutdownTimeout;
    private final Configuration config;
    private final AppenderRef[] appenderRefs;
    private final String errorRef;
    private final boolean includeLocation;
    private AppenderControl errorAppender;
    private AsyncThread thread;
    private AsyncQueueFullPolicy asyncQueueFullPolicy;

    private AsyncAppender(final String name, final Filter filter, final AppenderRef[] appenderRefs,
                          final String errorRef, final int queueSize, final boolean blocking,
                          final boolean ignoreExceptions, final long shutdownTimeout, final Configuration config,
                          final boolean includeLocation, final BlockingQueueFactory<LogEvent> blockingQueueFactory) {
        super(name, filter, null, ignoreExceptions);
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
            thread = new AsyncThread(appenders, queue);
            thread.setName("AsyncAppender-" + getName());
        } else if (errorRef == null) {
            throw new ConfigurationException("No appenders are available for AsyncAppender " + getName());
        }
        asyncQueueFullPolicy = AsyncQueueFullPolicyFactory.create();

        thread.start();
        super.start();
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        super.stop(timeout, timeUnit, false);
        LOGGER.trace("AsyncAppender stopping. Queue still has {} events.", queue.size());
        thread.shutdown();
        try {
            thread.join(shutdownTimeout);
        } catch (final InterruptedException ex) {
            LOGGER.warn("Interrupted while stopping AsyncAppender {}", getName());
        }
        LOGGER.trace("AsyncAppender stopped. Queue has {} events.", queue.size());

        if (DiscardingAsyncQueueFullPolicy.getDiscardCount(asyncQueueFullPolicy) > 0) {
            LOGGER.trace("AsyncAppender: {} discarded {} events.", asyncQueueFullPolicy,
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
        if (!canFormatMessageInBackground(logEvent.getMessage())) {
            logEvent.getMessage().getFormattedMessage(); // LOG4J2-763: ask message to freeze parameters
        }
        final Log4jLogEvent memento = Log4jLogEvent.createMemento(logEvent, includeLocation);
        if (!transfer(memento)) {
            if (blocking) {
                // delegate to the event router (which may discard, enqueue and block, or log in current thread)
                final EventRoute route = asyncQueueFullPolicy.getRoute(thread.getId(), memento.getLevel());
                route.logMessage(this, memento);
            } else {
                error("Appender " + getName() + " is unable to write primary appenders. queue is full");
                logToErrorAppenderIfNecessary(false, memento);
            }
        }
    }

    private boolean canFormatMessageInBackground(final Message message) {
        return Constants.FORMAT_MESSAGES_IN_BACKGROUND // LOG4J2-898: user wants to format all msgs in background
                || message.getClass().isAnnotationPresent(AsynchronouslyFormattable.class); // LOG4J2-1718
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
    public void logMessageInCurrentThread(final LogEvent logEvent) {
        logEvent.setEndOfBatch(queue.isEmpty());
        final boolean appendSuccessful = thread.callAppenders(logEvent);
        logToErrorAppenderIfNecessary(appendSuccessful, logEvent);
    }

    /**
     * FOR INTERNAL USE ONLY.
     *
     * @param logEvent the event to log
     */
    public void logMessageInBackgroundThread(final LogEvent logEvent) {
        try {
            // wait for free slots in the queue
            queue.put(logEvent);
        } catch (final InterruptedException e) {
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
            LOGGER.warn("Interrupted while waiting for a free slot in the AsyncAppender LogEvent-queue {}",
                getName());
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

    /**
     * Create an AsyncAppender. This method is retained for backwards compatibility. New code should use the
     * {@link Builder} instead. This factory will use {@link ArrayBlockingQueueFactory} by default as was the behavior
     * pre-2.7.
     *
     * @param appenderRefs     The Appenders to reference.
     * @param errorRef         An optional Appender to write to if the queue is full or other errors occur.
     * @param blocking         True if the Appender should wait when the queue is full. The default is true.
     * @param shutdownTimeout  How many milliseconds the Appender should wait to flush outstanding log events
     *                         in the queue on shutdown. The default is zero which means to wait forever.
     * @param size             The size of the event queue. The default is 128.
     * @param name             The name of the Appender.
     * @param includeLocation  whether to include location information. The default is false.
     * @param filter           The Filter or null.
     * @param config           The Configuration.
     * @param ignoreExceptions If {@code "true"} (default) exceptions encountered when appending events are logged;
     *                         otherwise they are propagated to the caller.
     * @return The AsyncAppender.
     * @deprecated use {@link Builder} instead
     */
    @Deprecated
    public static AsyncAppender createAppender(final AppenderRef[] appenderRefs, final String errorRef,
                                               final boolean blocking, final long shutdownTimeout, final int size,
                                               final String name, final boolean includeLocation, final Filter filter,
                                               final Configuration config, final boolean ignoreExceptions) {
        if (name == null) {
            LOGGER.error("No name provided for AsyncAppender");
            return null;
        }
        if (appenderRefs == null) {
            LOGGER.error("No appender references provided to AsyncAppender {}", name);
        }

        return new AsyncAppender(name, filter, appenderRefs, errorRef, size, blocking, ignoreExceptions,
            shutdownTimeout, config, includeLocation, new ArrayBlockingQueueFactory<LogEvent>());
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<AsyncAppender> {

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

        @PluginBuilderAttribute
        @Required(message = "No name provided for AsyncAppender")
        private String name;

        @PluginBuilderAttribute
        private boolean includeLocation = false;

        @PluginElement("Filter")
        private Filter filter;

        @PluginConfiguration
        private Configuration configuration;

        @PluginBuilderAttribute
        private boolean ignoreExceptions = true;

        @PluginElement(BlockingQueueFactory.ELEMENT_TYPE)
        private BlockingQueueFactory<LogEvent> blockingQueueFactory = new ArrayBlockingQueueFactory<>();

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

        public Builder setFilter(final Filter filter) {
            this.filter = filter;
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

        public Builder setBlockingQueueFactory(final BlockingQueueFactory<LogEvent> blockingQueueFactory) {
            this.blockingQueueFactory = blockingQueueFactory;
            return this;
        }

        @Override
        public AsyncAppender build() {
            return new AsyncAppender(name, filter, appenderRefs, errorRef, bufferSize, blocking, ignoreExceptions,
                shutdownTimeout, configuration, includeLocation, blockingQueueFactory);
        }
    }

    /**
     * Thread that calls the Appenders.
     */
    private class AsyncThread extends Log4jThread {

        private volatile boolean shutdown = false;
        private final List<AppenderControl> appenders;
        private final BlockingQueue<LogEvent> queue;

        public AsyncThread(final List<AppenderControl> appenders, final BlockingQueue<LogEvent> queue) {
            super("AsyncAppender-" + THREAD_SEQUENCE.getAndIncrement());
            this.appenders = appenders;
            this.queue = queue;
            setDaemon(true);
        }

        @Override
        public void run() {
            while (!shutdown) {
                LogEvent event;
                try {
                    event = queue.take();
                    if (event == SHUTDOWN_LOG_EVENT) {
                        shutdown = true;
                        continue;
                    }
                } catch (final InterruptedException ex) {
                    break; // LOG4J2-830
                }
                event.setEndOfBatch(queue.isEmpty());
                final boolean success = callAppenders(event);
                if (!success && errorAppender != null) {
                    try {
                        errorAppender.callAppender(event);
                    } catch (final Exception ex) {
                        // Silently accept the error.
                    }
                }
            }
            // Process any remaining items in the queue.
            LOGGER.trace("AsyncAppender.AsyncThread shutting down. Processing remaining {} queue events.",
                queue.size());
            int count = 0;
            int ignored = 0;
            while (!queue.isEmpty()) {
                try {
                    final LogEvent event = queue.take();
                    if (event instanceof Log4jLogEvent) {
                        final Log4jLogEvent logEvent = (Log4jLogEvent) event;
                        logEvent.setEndOfBatch(queue.isEmpty());
                        callAppenders(logEvent);
                        count++;
                    } else {
                        ignored++;
                        LOGGER.trace("Ignoring event of class {}", event.getClass().getName());
                    }
                } catch (final InterruptedException ex) {
                    // May have been interrupted to shut down.
                    // Here we ignore interrupts and try to process all remaining events.
                }
            }
            LOGGER.trace("AsyncAppender.AsyncThread stopped. Queue has {} events remaining. "
                + "Processed {} and ignored {} events since shutdown started.", queue.size(), count, ignored);
        }

        /**
         * Calls {@link AppenderControl#callAppender(LogEvent) callAppender} on all registered {@code AppenderControl}
         * objects, and returns {@code true} if at least one appender call was successful, {@code false} otherwise. Any
         * exceptions are silently ignored.
         *
         * @param event the event to forward to the registered appenders
         * @return {@code true} if at least one appender call succeeded, {@code false} otherwise
         */
        boolean callAppenders(final LogEvent event) {
            boolean success = false;
            for (final AppenderControl control : appenders) {
                try {
                    control.callAppender(event);
                    success = true;
                } catch (final Exception ex) {
                    // If no appender is successful the error appender will get it.
                }
            }
            return success;
        }

        public void shutdown() {
            shutdown = true;
            if (queue.isEmpty()) {
                queue.offer(SHUTDOWN_LOG_EVENT);
            }
            if (getState() == State.TIMED_WAITING || getState() == State.WAITING) {
                this.interrupt(); // LOG4J2-1422: if underlying appender is stuck in wait/sleep/join/park call
            }
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
}
