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

package org.apache.logging.log4j.core.appender.db;

import java.io.Flushable;
import java.util.ArrayList;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.ManagerFactory;

/**
 * Manager that allows database appenders to have their configuration reloaded without losing events.
 */
public abstract class AbstractDatabaseManager extends AbstractManager implements Flushable {
    private final ArrayList<LogEvent> buffer;
    private final int bufferSize;

    private boolean running = false;

    /**
     * Instantiates the base manager.
     *
     * @param name The manager name, which should include any configuration details that one might want to be able to
     *             reconfigure at runtime, such as database name, username, (hashed) password, etc.
     * @param bufferSize The size of the log event buffer.
     */
    protected AbstractDatabaseManager(final String name, final int bufferSize) {
        super(name);
        this.bufferSize = bufferSize;
        this.buffer = new ArrayList<LogEvent>(bufferSize + 1);
    }

    /**
     * Implementations should implement this method to perform any proprietary startup operations. This method will
     * never be called twice on the same instance. It is safe to throw any exceptions from this method. This method
     * does not necessarily connect to the database, as it is generally unreliable to connect once and use the same
     * connection for hours.
     */
    protected abstract void startupInternal() throws Exception;

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
                LOGGER.error("Could not perform database startup operations using logging manager [{}].",
                        this.getName(), e);
            }
        }
    }

    /**
     * Implementations should implement this method to perform any proprietary disconnection / shutdown operations. This
     * method will never be called twice on the same instance, and it will only be called <em>after</em>
     * {@link #startupInternal()}. It is safe to throw any exceptions from this method. This method does not
     * necessarily disconnect from the database for the same reasons outlined in {@link #startupInternal()}.
     */
    protected abstract void shutdownInternal() throws Exception;

    /**
     * This method is called from the {@link #release()} method when the appender is stopped or the appender's manager
     * is replaced. If it has not already been called, it calls {@link #shutdownInternal()} and catches any exceptions
     * it might throw.
     */
    public final synchronized void shutdown() {
        this.flush();
        if (this.isRunning()) {
            try {
                this.shutdownInternal();
            } catch (final Exception e) {
                LOGGER.warn("Error while performing database shutdown operations using logging manager [{}].",
                        this.getName(), e);
            } finally {
                this.running = false;
            }
        }
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

    /**
     * Connects to the database and starts a transaction (if applicable). With buffering enabled, this is called when
     * flushing the buffer begins, before the first call to {@link #writeInternal}. With buffering disabled, this is
     * called immediately before every invocation of {@link #writeInternal}.
     */
    protected abstract void connectAndStart();

    /**
     * Performs the actual writing of the event in an implementation-specific way. This method is called immediately
     * from {@link #write(LogEvent)} if buffering is off, or from {@link #flush()} if the buffer has reached its limit.
     *
     * @param event The event to write to the database.
     */
    protected abstract void writeInternal(LogEvent event);

    /**
     * Commits any active transaction (if applicable) and disconnects from the database (returns the connection to the
     * connection pool). With buffering enabled, this is called when flushing the buffer completes, after the last call
     * to {@link #writeInternal}. With buffering disabled, this is called immediately after every invocation of
     * {@link #writeInternal}.
     */
    protected abstract void commitAndClose();

    /**
     * This method is called automatically when the buffer size reaches its maximum or at the beginning of a call to
     * {@link #shutdown()}. It can also be called manually to flush events to the database.
     */
    @Override
    public final synchronized void flush() {
        if (this.isRunning() && this.buffer.size() > 0) {
            this.connectAndStart();
            try {
                for (final LogEvent event : this.buffer) {
                    this.writeInternal(event);
                }
            } finally {
                this.commitAndClose();
                // not sure if this should be done when writing the events failed
                this.buffer.clear();
            }
        }
    }

    /**
     * This method manages buffering and writing of events.
     *
     * @param event The event to write to the database.
     */
    public final synchronized void write(final LogEvent event) {
        if (this.bufferSize > 0) {
            this.buffer.add(event);
            if (this.buffer.size() >= this.bufferSize || event.isEndOfBatch()) {
                this.flush();
            }
        } else {
            this.connectAndStart();
            try {
                this.writeInternal(event);
            } finally {
                this.commitAndClose();
            }
        }
    }

    @Override
    public final void releaseSub() {
        this.shutdown();
    }

    @Override
    public final String toString() {
        return this.getName();
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
            final String name, final T data, final ManagerFactory<M, T> factory
    ) {
        return AbstractManager.getManager(name, factory, data);
    }

    /**
     * Implementations should extend this class for passing data between the getManager method and the manager factory
     * class.
     */
    protected abstract static class AbstractFactoryData {
        private final int bufferSize;

        /**
         * Constructs the base factory data.
         *
         * @param bufferSize The size of the buffer.
         */
        protected AbstractFactoryData(final int bufferSize) {
            this.bufferSize = bufferSize;
        }

        /**
         * Gets the buffer size.
         *
         * @return the buffer size.
         */
        public int getBufferSize() {
            return bufferSize;
        }
    }
}
