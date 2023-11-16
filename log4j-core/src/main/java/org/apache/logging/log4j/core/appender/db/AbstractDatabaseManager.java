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
package org.apache.logging.log4j.core.appender.db;

import java.io.Flushable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.config.Configuration;

/**
 * Manager that allows database appenders to have their configuration reloaded without losing events.
 */
public abstract class AbstractDatabaseManager extends AbstractManager implements Flushable {

    /**
     * Implementations should extend this class for passing data between the getManager method and the manager factory
     * class.
     */
    protected abstract static class AbstractFactoryData extends AbstractManager.AbstractFactoryData {
        private final int bufferSize;
        private final Layout<? extends Serializable> layout;

        /**
         * Constructs the base factory data.
         *
         * @param bufferSize The size of the buffer.
         * @param layout The appender-level layout
         * @deprecated Use {@link AbstractFactoryData#AbstractFactoryData(Configuration, int, Layout)}.
         */
        protected AbstractFactoryData(final int bufferSize, final Layout<? extends Serializable> layout) {
            this(null, bufferSize, layout);
        }

        /**
         * Constructs the base factory data.
         * @param configuration Configuration creating this instance.
         * @param bufferSize The size of the buffer.
         * @param layout The appender-level layout
         */
        protected AbstractFactoryData(
                final Configuration configuration, final int bufferSize, final Layout<? extends Serializable> layout) {
            super(configuration);
            this.bufferSize = bufferSize;
            this.layout = layout;
        }

        /**
         * Gets the buffer size.
         *
         * @return the buffer size.
         */
        public int getBufferSize() {
            return bufferSize;
        }

        /**
         * Gets the layout.
         *
         * @return the layout.
         */
        public Layout<? extends Serializable> getLayout() {
            return layout;
        }
    }

    /**
     * Implementations should define their own getManager method and call this method from that to create or get
     * existing managers.
     *
     * @param name The manager name, which should include any configuration details that one might want to be able to
     *             reconfigure at runtime, such as database name, username, (hashed) password, etc.
     * @param data The concrete instance of {@link AbstractFactoryData} appropriate for the given manager.
     * @param factory A factory instance for creating the appropriate manager.
     * @param <M> The concrete manager type.
     * @param <T> The concrete {@link AbstractFactoryData} type.
     * @return a new or existing manager of the specified type and name.
     */
    protected static <M extends AbstractDatabaseManager, T extends AbstractFactoryData> M getManager(
            final String name, final T data, final ManagerFactory<M, T> factory) {
        return AbstractManager.getManager(name, factory, data);
    }

    private final ArrayList<LogEvent> buffer;
    private final int bufferSize;

    private final Layout<? extends Serializable> layout;

    private boolean running;

    /**
     * Constructs the base manager.
     *
     * @param name The manager name, which should include any configuration details that one might want to be able to
     *             reconfigure at runtime, such as database name, username, (hashed) password, etc.
     * @param bufferSize The size of the log event buffer.
     * @deprecated Use {@link AbstractDatabaseManager#AbstractDatabaseManager(String, int, Layout, Configuration)}.
     */
    @Deprecated
    protected AbstractDatabaseManager(final String name, final int bufferSize) {
        this(name, bufferSize, null);
    }

    /**
     * Constructs the base manager.
     *
     * @param name The manager name, which should include any configuration details that one might want to be able to
     *             reconfigure at runtime, such as database name, username, (hashed) password, etc.
     * @param layout the Appender-level layout.
     * @param bufferSize The size of the log event buffer.
     * @deprecated Use {@link AbstractDatabaseManager#AbstractDatabaseManager(String, int, Layout, Configuration)}.
     */
    @Deprecated
    protected AbstractDatabaseManager(
            final String name, final int bufferSize, final Layout<? extends Serializable> layout) {
        this(name, bufferSize, layout, null);
    }

    /**
     * Constructs the base manager.
     *
     * @param name The manager name, which should include any configuration details that one might want to be able to
     *             reconfigure at runtime, such as database name, username, (hashed) password, etc.
     * @param layout the Appender-level layout.
     * @param bufferSize The size of the log event buffer.
     * @param configuration My configuration.
     */
    protected AbstractDatabaseManager(
            final String name,
            final int bufferSize,
            final Layout<? extends Serializable> layout,
            final Configuration configuration) {
        // null configuration allowed for backward compatibility.
        // TODO should super track Configuration instead of LoggerContext?
        super(configuration != null ? configuration.getLoggerContext() : null, name);
        this.bufferSize = bufferSize;
        this.buffer = new ArrayList<>(bufferSize + 1);
        this.layout = layout; // A null layout is allowed.
    }

    protected void buffer(final LogEvent event) {
        this.buffer.add(event.toImmutable());
        if (this.buffer.size() >= this.bufferSize || event.isEndOfBatch()) {
            this.flush();
        }
    }

    /**
     * Commits any active transaction (if applicable) and disconnects from the database (returns the connection to the
     * connection pool). With buffering enabled, this is called when flushing the buffer completes, after the last call
     * to {@link #writeInternal}. With buffering disabled, this is called immediately after every invocation of
     * {@link #writeInternal}.
     * @return true if all resources were closed normally, false otherwise.
     */
    protected abstract boolean commitAndClose();

    /**
     * Connects to the database and starts a transaction (if applicable). With buffering enabled, this is called when
     * flushing the buffer begins, before the first call to {@link #writeInternal}. With buffering disabled, this is
     * called immediately before every invocation of {@link #writeInternal}.
     */
    protected abstract void connectAndStart();

    /**
     * This method is called automatically when the buffer size reaches its maximum or at the beginning of a call to
     * {@link #shutdown()}. It can also be called manually to flush events to the database.
     */
    @Override
    public final synchronized void flush() {
        if (this.isRunning() && isBuffered()) {
            this.connectAndStart();
            try {
                for (final LogEvent event : this.buffer) {
                    this.writeInternal(event, layout != null ? layout.toSerializable(event) : null);
                }
            } finally {
                this.commitAndClose();
                // not sure if this should be done when writing the events failed
                this.buffer.clear();
            }
        }
    }

    protected boolean isBuffered() {
        return this.bufferSize > 0;
    }

    /**
     * Indicates whether the manager is currently connected {@link #startup()} has been called and {@link #shutdown()}
     * has not been called).
     *
     * @return {@code true} if the manager is connected.
     */
    public final boolean isRunning() {
        return this.running;
    }

    @Override
    public final boolean releaseSub(final long timeout, final TimeUnit timeUnit) {
        return this.shutdown();
    }

    /**
     * This method is called from the {@link #close()} method when the appender is stopped or the appender's manager
     * is replaced. If it has not already been called, it calls {@link #shutdownInternal()} and catches any exceptions
     * it might throw.
     * @return true if all resources were closed normally, false otherwise.
     */
    public final synchronized boolean shutdown() {
        boolean closed = true;
        this.flush();
        if (this.isRunning()) {
            try {
                closed &= this.shutdownInternal();
            } catch (final Exception e) {
                logWarn("Caught exception while performing database shutdown operations", e);
                closed = false;
            } finally {
                this.running = false;
            }
        }
        return closed;
    }

    /**
     * Implementations should implement this method to perform any proprietary disconnection / shutdown operations. This
     * method will never be called twice on the same instance, and it will only be called <em>after</em>
     * {@link #startupInternal()}. It is safe to throw any exceptions from this method. This method does not
     * necessarily disconnect from the database for the same reasons outlined in {@link #startupInternal()}.
     * @return true if all resources were closed normally, false otherwise.
     */
    protected abstract boolean shutdownInternal() throws Exception;

    /**
     * This method is called within the appender when the appender is started. If it has not already been called, it
     * calls {@link #startupInternal()} and catches any exceptions it might throw.
     */
    public final synchronized void startup() {
        if (!this.isRunning()) {
            try {
                this.startupInternal();
                this.running = true;
            } catch (final Exception e) {
                logError("Could not perform database startup operations", e);
            }
        }
    }

    /**
     * Implementations should implement this method to perform any proprietary startup operations. This method will
     * never be called twice on the same instance. It is safe to throw any exceptions from this method. This method
     * does not necessarily connect to the database, as it is generally unreliable to connect once and use the same
     * connection for hours.
     */
    protected abstract void startupInternal() throws Exception;

    @Override
    public final String toString() {
        return this.getName();
    }

    /**
     * This method manages buffering and writing of events.
     *
     * @param event The event to write to the database.
     * @deprecated since 2.11.0 Use {@link #write(LogEvent, Serializable)}.
     */
    @Deprecated
    public final synchronized void write(final LogEvent event) {
        write(event, null);
    }

    /**
     * This method manages buffering and writing of events.
     *
     * @param event The event to write to the database.
     * @param serializable Serializable event
     */
    public final synchronized void write(final LogEvent event, final Serializable serializable) {
        if (isBuffered()) {
            buffer(event);
        } else {
            writeThrough(event, serializable);
        }
    }

    /**
     * Performs the actual writing of the event in an implementation-specific way. This method is called immediately
     * from {@link #write(LogEvent, Serializable)} if buffering is off, or from {@link #flush()} if the buffer has reached its limit.
     *
     * @param event The event to write to the database.
     * @deprecated Use {@link #writeInternal(LogEvent, Serializable)}.
     */
    @Deprecated
    protected void writeInternal(final LogEvent event) {
        writeInternal(event, null);
    }

    /**
     * Performs the actual writing of the event in an implementation-specific way. This method is called immediately
     * from {@link #write(LogEvent, Serializable)} if buffering is off, or from {@link #flush()} if the buffer has reached its limit.
     *
     * @param event The event to write to the database.
     */
    protected abstract void writeInternal(LogEvent event, Serializable serializable);

    protected void writeThrough(final LogEvent event, final Serializable serializable) {
        this.connectAndStart();
        try {
            this.writeInternal(event, serializable);
        } finally {
            this.commitAndClose();
        }
    }
}
