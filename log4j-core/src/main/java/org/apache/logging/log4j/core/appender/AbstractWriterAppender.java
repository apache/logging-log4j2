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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.Property;

/**
 * Appends log events as strings to a writer.
 *
 * @param <M>
 *            The kind of {@link WriterManager} under management
 */
public abstract class AbstractWriterAppender<M extends WriterManager> extends AbstractAppender {

    /**
     * Immediate flush means that the underlying writer will be flushed at the
     * end of each append operation. Immediate flush is slower but ensures that
     * each append request is actually written. If <code>immediateFlush</code>
     * is set to {@code false}, then there is a good chance that the last few
     * logs events are not actually written to persistent media if and when the
     * application crashes.
     */
    protected final boolean immediateFlush;

    private final M manager;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();

    /**
     * Instantiates.
     *
     * @param name
     *            The name of the Appender.
     * @param layout
     *            The layout to format the message.
     * @param filter
     *            The filter to associate with the Appender.
     * @param ignoreExceptions
     *            If true, exceptions will be logged and suppressed.
     *            If false errors will be logged and then passed to the application.
     * @param immediateFlush
     *            Underlying writer will be flushed at the end of each append operation.
     * @param properties
     *            Optional properties.
     * @param manager
     *            The OutputStreamManager.
     */
    protected AbstractWriterAppender(
            final String name,
            final StringLayout layout,
            final Filter filter,
            final boolean ignoreExceptions,
            final boolean immediateFlush,
            final Property[] properties,
            final M manager) {
        super(name, filter, layout, ignoreExceptions, properties);
        this.manager = manager;
        this.immediateFlush = immediateFlush;
    }

    /**
     * Instantiates.
     *
     * @param name
     *            The name of the Appender.
     * @param layout
     *            The layout to format the message.
     * @param manager
     *            The OutputStreamManager.
     * @deprecated Use {@link #AbstractWriterAppender(String, StringLayout, Filter, boolean, boolean, Property[], WriterManager)}.
     */
    @Deprecated
    protected AbstractWriterAppender(
            final String name,
            final StringLayout layout,
            final Filter filter,
            final boolean ignoreExceptions,
            final boolean immediateFlush,
            final M manager) {
        super(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);
        this.manager = manager;
        this.immediateFlush = immediateFlush;
    }

    /**
     * Actual writing occurs here.
     * <p>
     * Most subclasses will need to override this method.
     * </p>
     *
     * @param event
     *            The LogEvent.
     */
    @Override
    public void append(final LogEvent event) {
        readLock.lock();
        try {
            final String str = getStringLayout().toSerializable(event);
            if (str.length() > 0) {
                manager.write(str);
                if (this.immediateFlush || event.isEndOfBatch()) {
                    manager.flush();
                }
            }
        } catch (final AppenderLoggingException ex) {
            error("Unable to write " + manager.getName() + " for appender " + getName(), event, ex);
            throw ex;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Gets the manager.
     *
     * @return the manager.
     */
    public M getManager() {
        return manager;
    }

    public StringLayout getStringLayout() {
        return (StringLayout) getLayout();
    }

    @Override
    public void start() {
        if (getLayout() == null) {
            LOGGER.error("No layout set for the appender named [{}].", getName());
        }
        if (manager == null) {
            LOGGER.error("No OutputStreamManager set for the appender named [{}].", getName());
        }
        super.start();
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        boolean stopped = super.stop(timeout, timeUnit, false);
        stopped &= manager.stop(timeout, timeUnit);
        setStopped();
        return stopped;
    }
}
