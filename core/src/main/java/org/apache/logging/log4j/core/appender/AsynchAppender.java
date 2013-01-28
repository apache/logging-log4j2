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

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Appends to one or more Appenders asynchronously.  You can configure an AsynchAppender with one
 * or more Appenders and an Appender to append to if the queue is full. The AsynchAppender does not allow
 * a filter to be specified on the Appender references.
 *
 * @param <T> The {@link Layout}'s {@link Serializable} type.
 */
@Plugin(name = "Asynch", type = "Core", elementType = "appender", printObject = true)
public final class AsynchAppender<T extends Serializable> extends AbstractAppender<T> {

    private static final int DEFAULT_QUEUE_SIZE = 128;
    private static final String SHUTDOWN = "Shutdown";

    private final BlockingQueue<Serializable> queue;
    private final boolean blocking;
    private final Configuration config;
    private final AppenderRef[] appenderRefs;
    private final String errorRef;
    private AppenderControl errorAppender;
    private AsynchThread thread;

    private AsynchAppender(final String name, final Filter filter, final AppenderRef[] appenderRefs,
                           final String errorRef, final int queueSize, final boolean blocking,
                           final boolean handleExceptions, final Configuration config) {
        super(name, filter, null, handleExceptions);
        this.queue = new ArrayBlockingQueue<Serializable>(queueSize);
        this.blocking = blocking;
        this.config = config;
        this.appenderRefs = appenderRefs;
        this.errorRef = errorRef;
    }

    @Override
    public void start() {
        final Map<String, Appender<?>> map = config.getAppenders();
        final List<AppenderControl> appenders = new ArrayList<AppenderControl>();
        for (final AppenderRef appenderRef : appenderRefs) {
            if (map.containsKey(appenderRef.getRef())) {
                appenders.add(new AppenderControl(map.get(appenderRef.getRef()), null, null));
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
            thread = new AsynchThread(appenders, queue);
        } else if (errorRef == null) {
            throw new ConfigurationException("No appenders are available for AsynchAppender " + getName());
        }

        thread.start();
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        thread.shutdown();
        try {
            thread.join();
        } catch (final InterruptedException ex) {
            LOGGER.warn("Interrupted while stopping AsynchAppender {}", getName());
        }
    }

    /**
     * Actual writing occurs here.
     * <p/>
     * @param event The LogEvent.
     */
    public void append(final LogEvent event) {
        if (!isStarted()) {
            throw new IllegalStateException("AsynchAppender " + getName() + " is not active");
        }
        if (event instanceof Log4jLogEvent) {
            if (blocking && queue.remainingCapacity() > 0) {
                try {
                    queue.add(Log4jLogEvent.serialize((Log4jLogEvent) event));
                    return;
                } catch (final IllegalStateException ex) {
                    error("Appender " + getName() + " is unable to write primary appenders. queue is full");
                }
            }
            if (errorAppender != null) {
                if (!blocking) {
                    error("Appender " + getName() + " is unable to write primary appenders. queue is full");
                }
                errorAppender.callAppender(event);
            }
        }
    }

    /**
     * Create an AsynchAppender.
     * @param appenderRefs The Appenders to reference.
     * @param errorRef An optional Appender to write to if the queue is full or other errors occur.
     * @param blocking True if the Appender should wait when the queue is full. The default is true.
     * @param size The size of the event queue. The default is 128.
     * @param name The name of the Appender.
     * @param filter The Filter or null.
     * @param config The Configuration.
     * @param suppress "true" if exceptions should be hidden from the application, "false" otherwise.
     * The default is "true".
     * @param <S> The actual type of the Serializable.
     * @return The AsynchAppender.
     */
    @PluginFactory
    public static <S extends Serializable> AsynchAppender<S> createAppender(
                                                @PluginElement("appender-ref") final AppenderRef[] appenderRefs,
                                                @PluginAttr("error-ref") final String errorRef,
                                                @PluginAttr("blocking") final String blocking,
                                                @PluginAttr("bufferSize") final String size,
                                                @PluginAttr("name") final String name,
                                                @PluginElement("filter") final Filter filter,
                                                @PluginConfiguration final Configuration config,
                                                @PluginAttr("suppressExceptions") final String suppress) {
        if (name == null) {
            LOGGER.error("No name provided for AsynchAppender");
            return null;
        }
        if (appenderRefs == null) {
            LOGGER.error("No appender references provided to AsynchAppender {}", name);
        }

        final boolean isBlocking = blocking == null ? true : Boolean.valueOf(blocking);
        final int queueSize = size == null ? DEFAULT_QUEUE_SIZE : Integer.parseInt(size);

        final boolean handleExceptions = suppress == null ? true : Boolean.valueOf(suppress);

        return new AsynchAppender<S>(name, filter, appenderRefs, errorRef, queueSize, isBlocking, handleExceptions,
                                  config);
    }

    /**
     * Thread that calls the Appenders.
     */
    private class AsynchThread extends Thread {

        private volatile boolean shutdown = false;
        private final List<AppenderControl> appenders;
        private final BlockingQueue<Serializable> queue;

        public AsynchThread(final List<AppenderControl> appenders, final BlockingQueue<Serializable> queue) {
            this.appenders = appenders;
            this.queue = queue;
        }

        @Override
        public void run() {
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
                boolean success = false;
                for (final AppenderControl control : appenders) {
                    try {
                        control.callAppender(event);
                        success = true;
                    } catch (final Exception ex) {
                        // If no appender is successful the error appender will get it.
                    }
                }
                if (!success && errorAppender != null) {
                    try {
                        errorAppender.callAppender(event);
                    } catch (final Exception ex) {
                        // Silently accept the error.
                    }
                }
            }
            // Process any remaining items in the queue.
            while (!queue.isEmpty()) {
                try {
                    final Log4jLogEvent event = Log4jLogEvent.deserialize(queue.take());
                    for (final AppenderControl control : appenders) {
                        control.callAppender(event);
                    }
                } catch (final InterruptedException ex) {
                    // May have been interrupted to shut down.
                }
            }
        }

        public void shutdown() {
            shutdown = true;
            if (queue.isEmpty()) {
                queue.offer(SHUTDOWN);
            }
        }
    }
}
