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
package org.apache.log4j;

import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.OptionHandler;

/**
 * The base class for Appenders in Log4j 1. Appenders constructed using this are ignored in Log4j 2.
 */
public abstract class AppenderSkeleton implements Appender, OptionHandler {

    protected Layout layout;

    protected String name;

    protected Priority threshold;

    protected ErrorHandler errorHandler = new NoOpErrorHandler();

    protected Filter headFilter;

    protected Filter tailFilter;

    protected boolean closed = false;

    /**
     * Create new instance.
     */
    public AppenderSkeleton() {}

    protected AppenderSkeleton(final boolean isActive) {}

    @Override
    public void activateOptions() {}

    @Override
    public void addFilter(final Filter newFilter) {
        if (headFilter == null) {
            headFilter = tailFilter = newFilter;
        } else {
            tailFilter.setNext(newFilter);
            tailFilter = newFilter;
        }
    }

    protected abstract void append(LoggingEvent event);

    @Override
    public void clearFilters() {
        headFilter = tailFilter = null;
    }

    @Override
    public void finalize() {}

    @Override
    public ErrorHandler getErrorHandler() {
        return this.errorHandler;
    }

    @Override
    public Filter getFilter() {
        return headFilter;
    }

    public final Filter getFirstFilter() {
        return headFilter;
    }

    @Override
    public Layout getLayout() {
        return layout;
    }

    @Override
    public final String getName() {
        return this.name;
    }

    public Priority getThreshold() {
        return threshold;
    }

    public boolean isAsSevereAsThreshold(final Priority priority) {
        return ((threshold == null) || priority.isGreaterOrEqual(threshold));
    }

    @Override
    public synchronized void doAppend(final LoggingEvent event) {
        // Threshold checks and filtering is performed by the AppenderWrapper.
        append(event);
    }

    /**
     * Sets the {@link ErrorHandler} for this Appender.
     *
     * @since 0.9.0
     */
    @Override
    public synchronized void setErrorHandler(final ErrorHandler eh) {
        if (eh != null) {
            this.errorHandler = eh;
        }
    }

    @Override
    public void setLayout(final Layout layout) {
        this.layout = layout;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    public void setThreshold(final Priority threshold) {
        this.threshold = threshold;
    }

    public static class NoOpErrorHandler implements ErrorHandler {
        @Override
        public void setLogger(final Logger logger) {}

        @Override
        public void error(final String message, final Exception e, final int errorCode) {}

        @Override
        public void error(final String message) {}

        @Override
        public void error(final String message, final Exception e, final int errorCode, final LoggingEvent event) {}

        @Override
        public void setAppender(final Appender appender) {}

        @Override
        public void setBackupAppender(final Appender appender) {}
    }
}
