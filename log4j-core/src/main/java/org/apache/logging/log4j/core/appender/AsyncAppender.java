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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;

/**
 * Appends to one or more Appenders asynchronously.  You can configure an
 * AsyncAppender with one or more Appenders and an Appender to append to if the
 * queue is full. The AsyncAppender does not allow a filter to be specified on
 * the Appender references.
 */
@Plugin(name = "Async", category = "Core", elementType = "appender", printObject = true)
public final class AsyncAppender extends AbstractAppender {

    private static final int DEFAULT_QUEUE_SIZE = 128;
    private static final String SHUTDOWN = "Shutdown";

    private final BlockingQueue<Serializable> queue;
    private final int queueSize;
    private final boolean blocking;
    private final Configuration config;
    private final AppenderRef[] appenderRefs;
    private final String errorRef;
    private final boolean includeLocation;
    private AppenderControl errorAppender;
    private AsyncThread thread;
    private static final AtomicLong threadSequence = new AtomicLong(1);
    private static ThreadLocal<Boolean> isAppenderThread = new ThreadLocal<Boolean>();


    private AsyncAppender(final String name, final Filter filter, final AppenderRef[] appenderRefs,
                           final String errorRef, final int queueSize, final boolean blocking,
                           final boolean ignoreExceptions, final Configuration config,
                           final boolean includeLocation) {
        super(name, filter, null, ignoreExceptions);
        this.queue = new ArrayBlockingQueue<Serializable>(queueSize);
        this.queueSize = queueSize;
        this.blocking = blocking;
        this.config = config;
        this.appenderRefs = appenderRefs;
        this.errorRef = errorRef;
        this.includeLocation = includeLocation;
    }

    @Override
    public void start() {
        final Map<String, Appender> map = config.getAppenders();
        final List<AppenderControl> appenders = new ArrayList<AppenderControl>();
        for (final AppenderRef appenderRef : appenderRefs) {
            if (map.containsKey(appenderRef.getRef())) {
                appenders.add(new AppenderControl(map.get(appenderRef.getRef()), appenderRef.getLevel(),
                    appenderRef.getFilter()));
            } else {
                LOGGER.error("No appender named {} was configured", appenderRef);
            }
        }
        if (errorRef != null) {
            if (map.containsKey(errorRef)) {
                errorAppender = new AppenderControl(map.get(errorRef), null, null);
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

        thread.start();
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        LOGGER.trace("AsyncAppender stopping. Queue still has {} events.", queue.size());
        thread.shutdown();
        try {
            thread.join();
        } catch (final InterruptedException ex) {
            LOGGER.warn("Interrupted while stopping AsyncAppender {}", getName());
        }
        LOGGER.trace("AsyncAppender stopped. Queue has {} events.", queue.size());
    }

    /**
     * Actual writing occurs here.
     * <p/>
     * @param logEvent The LogEvent.
     */
    @Override
    public void append(final LogEvent logEvent) {
        if (!isStarted()) {
            throw new IllegalStateException("AsyncAppender " + getName() + " is not active");
        }
        if (!(logEvent instanceof Log4jLogEvent)) {
            return; // only know how to Serialize Log4jLogEvents
        }
        Log4jLogEvent coreEvent = (Log4jLogEvent) logEvent;
        boolean appendSuccessful = false;
        if (blocking) {
            if (isAppenderThread.get() == Boolean.TRUE && queue.remainingCapacity() == 0) {
                // LOG4J2-485: avoid deadlock that would result from trying
                // to add to a full queue from appender thread
                coreEvent.setEndOfBatch(false); // queue is definitely not empty!
                appendSuccessful = thread.callAppenders(coreEvent);
            } else {
                try {
                    // wait for free slots in the queue
                    queue.put(Log4jLogEvent.serialize(coreEvent, includeLocation));
                    appendSuccessful = true;
                } catch (final InterruptedException e) {
                    LOGGER.warn("Interrupted while waiting for a free slot in the AsyncAppender LogEvent-queue {}",
                            getName());
                }
            }
        } else {
            appendSuccessful = queue.offer(Log4jLogEvent.serialize(coreEvent, includeLocation));
            if (!appendSuccessful) {
                error("Appender " + getName() + " is unable to write primary appenders. queue is full");
            }
        }
        if (!appendSuccessful && errorAppender != null) {
            errorAppender.callAppender(coreEvent);
        }
    }

    /**
     * Create an AsyncAppender.
     * @param appenderRefs The Appenders to reference.
     * @param errorRef An optional Appender to write to if the queue is full or other errors occur.
     * @param blocking True if the Appender should wait when the queue is full. The default is true.
     * @param size The size of the event queue. The default is 128.
     * @param name The name of the Appender.
     * @param includeLocation whether to include location information. The default is false.
     * @param filter The Filter or null.
     * @param config The Configuration.
     * @param ignoreExceptions If {@code "true"} (default) exceptions encountered when appending events are logged;
     *                         otherwise they are propagated to the caller.
     * @return The AsyncAppender.
     */
    @PluginFactory
    public static AsyncAppender createAppender(@PluginElement("AppenderRef") final AppenderRef[] appenderRefs,
            @PluginAttribute("errorRef") @PluginAliases("error-ref") final String errorRef,
            @PluginAttribute(value = "blocking", defaultBoolean = true) final boolean blocking,
            @PluginAttribute(value = "bufferSize", defaultInt = DEFAULT_QUEUE_SIZE) final int size,
            @PluginAttribute("name") final String name,
            @PluginAttribute(value = "includeLocation", defaultBoolean = false) final boolean includeLocation,
            @PluginElement("Filter") final Filter filter,
            @PluginConfiguration final Configuration config,
            @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) final boolean ignoreExceptions) {
        if (name == null) {
            LOGGER.error("No name provided for AsyncAppender");
            return null;
        }
        if (appenderRefs == null) {
            LOGGER.error("No appender references provided to AsyncAppender {}", name);
        }

        return new AsyncAppender(name, filter, appenderRefs, errorRef,
                size, blocking, ignoreExceptions, config, includeLocation);
    }

    /**
     * Thread that calls the Appenders.
     */
    private class AsyncThread extends Thread {

        private volatile boolean shutdown = false;
        private final List<AppenderControl> appenders;
        private final BlockingQueue<Serializable> queue;

        public AsyncThread(final List<AppenderControl> appenders, final BlockingQueue<Serializable> queue) {
            this.appenders = appenders;
            this.queue = queue;
            setDaemon(true);
            setName("AsyncAppenderThread" + threadSequence.getAndIncrement());
        }

        @Override
        public void run() {
            isAppenderThread.set(Boolean.TRUE); // LOG4J2-485
            while (!shutdown) {
                Serializable s;
                try {
                    s = queue.take();
                    if (s != null && s instanceof String && SHUTDOWN.equals(s.toString())) {
                        shutdown = true;
                        continue;
                    }
                } catch (final InterruptedException ex) {
                    // No good reason for this.
                    continue;
                }
                final Log4jLogEvent event = Log4jLogEvent.deserialize(s);
                event.setEndOfBatch(queue.isEmpty());
                boolean success = callAppenders(event);
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
            int count= 0;
            int ignored = 0;
            while (!queue.isEmpty()) {
                try {
                    final Serializable s = queue.take();
                    if (Log4jLogEvent.canDeserialize(s)) {
                        final Log4jLogEvent event = Log4jLogEvent.deserialize(s);
                        event.setEndOfBatch(queue.isEmpty());
                        callAppenders(event);
                        count++;
                    } else {
                        ignored++;
                        LOGGER.trace("Ignoring event of class {}", s.getClass().getName());
                    }
                } catch (final InterruptedException ex) {
                    // May have been interrupted to shut down.
                }
            }
            LOGGER.trace("AsyncAppender.AsyncThread stopped. Queue has {} events remaining. " +
            		"Processed {} and ignored {} events since shutdown started.",
            		queue.size(), count, ignored);
        }

        /**
         * Calls {@link AppenderControl#callAppender(LogEvent) callAppender} on
         * all registered {@code AppenderControl} objects, and returns {@code true}
         * if at least one appender call was successful, {@code false} otherwise.
         * Any exceptions are silently ignored.
         *
         * @param event the event to forward to the registered appenders
         * @return {@code true} if at least one appender call succeeded, {@code false} otherwise
         */
        boolean callAppenders(final Log4jLogEvent event) {
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
                queue.offer(SHUTDOWN);
            }
        }
    }

    /**
     * Returns the names of the appenders that this asyncAppender delegates to
     * as an array of Strings.
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
     * Returns {@code true} if this AsyncAppender will take a snapshot of the stack with
     * every log event to determine the class and method where the logging call
     * was made.
     * @return {@code true} if location is included with every event, {@code false} otherwise
     */
    public boolean isIncludeLocation() {
        return includeLocation;
    }

    /**
     * Returns {@code true} if this AsyncAppender will block when the queue is full,
     * or {@code false} if events are dropped when the queue is full.
     * @return whether this AsyncAppender will block or drop events when the queue is full.
     */
    public boolean isBlocking() {
        return blocking;
    }

    /**
     * Returns the name of the appender that any errors are logged to or {@code null}.
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
