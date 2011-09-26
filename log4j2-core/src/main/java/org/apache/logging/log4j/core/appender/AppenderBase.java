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
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.Lifecycle;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.filter.Filterable;
import org.apache.logging.log4j.core.filter.Filters;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.Logger;

/**
 *
 * @doubt Appender should be refactored as mentioned elsewhere
 */
public abstract class AppenderBase extends Filterable implements Appender, Lifecycle {

    /**
     * Appenders set this by calling super.start().
     */
    private boolean started = false;

    private Layout layout = null;

    private final String name;

    private final boolean handleException;

    /**
     * Allow subclasses access to the status logger without creating another instance.
     */
    protected static final Logger logger = StatusLogger.getLogger();

    private ErrorHandler handler = new DefaultErrorHandler(this);

    public static final String NAME = "name";

    public AppenderBase(String name, Filters filters, Layout layout) {
        this(name, filters, layout, true);
    }

    public AppenderBase(String name, Filters filters, Layout layout, boolean handleException) {
        this.name = name;
        this.layout = layout;
        this.handleException = handleException;
        setFilters(filters);
    }

    public ErrorHandler getHandler() {
        return handler;
    }

    /**
     * The handler must be set before the appender is started.
     */
    public void setHandler(ErrorHandler handler) {
        if (handler == null) {
            logger.error("The handler cannot be set to null");
        }
        if (isStarted()) {
            logger.error("The handler cannot be changed once the appender is started");
            return;
        }
        this.handler = handler;
    }

    public void close() {

    }

    public String getName() {
        return name;
    }

    public Layout getLayout() {
        return layout;
    }

    /**
     * Appenders may override this method to indicate that a Layout is required.
     * @return false if no Layout is required.
     */
    public boolean requiresLayout() {
        return false;
    }

    /**
     * Some appenders need to propogate exceptions back to the application. When suppressException is false the
     * AppenderControl will allow the exception to percolate.
     */
    public boolean isExceptionSuppressed() {
        return handleException;
    }

    public void start() {
        if (layout == null && requiresLayout()) {
            logger.error("A layout is required and none was provided");
            return;
        }
        startFilters();
        this.started = true;
    }

    public void stop() {
        this.started = false;
        stopFilters();
    }

    public boolean isStarted() {
        return started;
    }

    public String toString() {
        return name;
    }

    /**
     * Handle an error with a message.
     * @param msg The message.
     */
    public void error(String msg) {
        handler.error(msg);
    }

    /**
     * Handle an error with a message and an exception.
     * @param msg The message.
     * @param t The Throwable.
     */
    public void error(String msg, Throwable t) {
        handler.error(msg, t);
    }

    /**
     * Handle an error with a message, and exception and a logging event.
     * @param msg The message.
     * @param event The LogEvent.
     * @param t The Throwable.
     */
    public void error(String msg, LogEvent event, Throwable t) {
        handler.error(msg, event, t);
    }

}
