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

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.filter.AbstractFilterable;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.Logger;

/**
 * Abstract base class for Appenders. Although Appenders do not have to extend this class, doing so
 * will simplify their implementation.
 *
 * @param <T> The {@link Layout}'s {@link Serializable} type.
 */
public abstract class AbstractAppender<T extends Serializable> extends AbstractFilterable
    implements Appender<T>, LifeCycle {
    /**
     * Allow subclasses access to the status logger without creating another instance.
     */
    protected static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * Appenders set this by calling super.start().
     */
    private boolean started = false;

    private final Layout<T> layout;

    private final String name;

    private final boolean handleException;

    private ErrorHandler handler = new DefaultErrorHandler(this);

    /**
     * Constructor that defaults to suppressing exceptions.
     * @param name The Appender name.
     * @param filter The Filter to associate with the Appender.
     * @param layout The layout to use to format the event.
     */
    protected AbstractAppender(final String name, final Filter filter, final Layout<T> layout) {
        this(name, filter, layout, true);
    }

    /**
     * Constructor.
     * @param name The Appender name.
     * @param filter The Filter to associate with the Appender.
     * @param layout The layout to use to format the event.
     * @param handleException If true, exceptions will be logged and suppressed. If false errors will be
     * logged and then passed to the application.
     */
    protected AbstractAppender(final String name, final Filter filter, final Layout<T> layout,
                               final boolean handleException) {
        super(filter);
        this.name = name;
        this.layout = layout;
        this.handleException = handleException;
    }

    /**
     * Returns the ErrorHandler, if any.
     * @return The ErrorHandler.
     */
    public ErrorHandler getHandler() {
        return handler;
    }

    /**
     * The handler must be set before the appender is started.
     * @param handler The ErrorHandler to use.
     */
    public void setHandler(final ErrorHandler handler) {
        if (handler == null) {
            LOGGER.error("The handler cannot be set to null");
        }
        if (isStarted()) {
            LOGGER.error("The handler cannot be changed once the appender is started");
            return;
        }
        this.handler = handler;
    }

    /**
     * Close the stream associated with the Appender.
     */
    public void close() {

    }

    /**
     * Returns the name of the Appender.
     * @return The name of the Appender.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the Layout for the appender.
     * @return The Layout used to format the event.
     */
    public Layout<T> getLayout() {
        return layout;
    }

    /**
     * Some appenders need to propogate exceptions back to the application. When suppressException is false the
     * AppenderControl will allow the exception to percolate.
     * @return true if exceptions will be supressed, false otherwise.
     */
    public boolean isExceptionSuppressed() {
        return handleException;
    }

    /**
     * Start the Appender.
     */
    public void start() {
        startFilter();
        this.started = true;
    }

    /**
     * Stop the Appender.
     */
    public void stop() {
        this.started = false;
        stopFilter();
    }

    /**
     * Returns true if the Appender is started, false otherwise.
     * @return true if the Appender is started, false otherwise.
     */
    public boolean isStarted() {
        return started;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Handle an error with a message.
     * @param msg The message.
     */
    public void error(final String msg) {
        handler.error(msg);
    }

    /**
     * Handle an error with a message and an exception.
     * @param msg The message.
     * @param t The Throwable.
     */
    public void error(final String msg, final Throwable t) {
        handler.error(msg, t);
    }

    /**
     * Handle an error with a message, and exception and a logging event.
     * @param msg The message.
     * @param event The LogEvent.
     * @param t The Throwable.
     */
    public void error(final String msg, final LogEvent event, final Throwable t) {
        handler.error(msg, event, t);
    }

}
