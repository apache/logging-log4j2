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

import java.util.ArrayList;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.ManagerFactory;

/**
 * Manager that allows database appenders to have their configuration reloaded without losing events.
 */
public abstract class AbstractDatabaseManager extends AbstractManager {
    private final ArrayList<LogEvent> buffer;
    private final int bufferSize;

    private boolean connected = false;

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
     * Implementations should implement this method to perform any proprietary connection operations. This method will
     * never be called twice on the same instance. It is safe to throw any exceptions from this method.
     */
    protected abstract void connectInternal() throws Exception;

    /**
     * This method is called within the appender when the appender is started. If it has not already been called, it
     * calls {@link #connectInternal()} and catches any exceptions it might throw.
     */
    public final synchronized void connect() {
        if (!this.isConnected()) {
            try {
                this.connectInternal();
                this.connected = true;
            } catch (final Exception e) {
                LOGGER.error("Could not connect to database using logging manager [{}].", this.getName(), e);
            }
        }
    }

    /**
     * Implementations should implement this method to perform any proprietary disconnection / shutdown operations. This
     * method will never be called twice on the same instance, and it will only be called <em>after</em>
     * {@link #connectInternal()}. It is safe to throw any exceptions from this method.
     */
    protected abstract void disconnectInternal() throws Exception;

    /**
     * This method is called from the {@link #release()} method when the appender is stopped or the appender's manager
     * is replaced. If it has not already been called, it calls {@link #disconnectInternal()} and catches any exceptions
     * it might throw.
     */
    public final synchronized void disconnect() {
        this.flush();
        if (this.isConnected()) {
            try {
                this.disconnectInternal();
            } catch (final Exception e) {
                LOGGER.warn("Error while disconnecting from database using logging manager [{}].", this.getName(), e);
            } finally {
                this.connected = false;
            }
        }
    }

    /**
     * Indicates whether the manager is currently connected {@link #connect()} has been called and {@link #disconnect()}
     * has not been called).
     *
     * @return {@code true} if the manager is connected.
     */
    public final boolean isConnected() {
        return this.connected;
    }

    /**
     * Performs the actual writing of the event in an implementation-specific way. This method is called immediately
     * from {@link #write(LogEvent)} if buffering is off, or from {@link #flush()} if the buffer has reached its limit.
     *
     * @param event The event to write to the database.
     */
    protected abstract void writeInternal(LogEvent event);

    /**
     * This method is called automatically when the buffer size reaches its maximum or at the beginning of a call to
     * {@link #disconnect()}. It can also be called manually to flush events to the database.
     */
    public final synchronized void flush() {
        if (this.isConnected() && this.buffer.size() > 0) {
            for (final LogEvent event : this.buffer) {
                this.writeInternal(event);
            }
            this.buffer.clear();
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
            this.writeInternal(event);
        }
    }

    @Override
    public final void releaseSub() {
        this.disconnect();
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
