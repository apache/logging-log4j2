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
    public AppenderSkeleton() {
        super();
    }

    protected AppenderSkeleton(final boolean isActive) {
        super();
    }

    public void activateOptions() {
    }

    public void addFilter(Filter newFilter) {
        if(headFilter == null) {
            headFilter = tailFilter = newFilter;
        } else {
            tailFilter.setNext(newFilter);
            tailFilter = newFilter;
        }
    }

    protected abstract void append(LoggingEvent event);

    public void clearFilters() {
        headFilter = tailFilter = null;
    }

    public void finalize() {
    }

    public ErrorHandler getErrorHandler() {
        return this.errorHandler;
    }

    public Filter getFilter() {
        return headFilter;
    }

    public final Filter getFirstFilter() {
        return headFilter;
    }

    public Layout getLayout() {
        return layout;
    }

    public final String getName() {
        return this.name;
    }

    public Priority getThreshold() {
        return threshold;
    }

    public boolean isAsSevereAsThreshold(Priority priority) {
        return ((threshold == null) || priority.isGreaterOrEqual(threshold));
    }

    /**
     * This method is never going to be called in Log4j 2 so there isn't much point in having any code in it.
     * @param event The LoggingEvent.
     */
    public void doAppend(LoggingEvent event) {
    }

    /**
     * Set the {@link ErrorHandler} for this Appender.
     *
     * @since 0.9.0
     */
    public synchronized void setErrorHandler(ErrorHandler eh) {
        if (eh != null) {
            this.errorHandler = eh;
        }
    }

    public void setLayout(Layout layout) {
        this.layout = layout;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setThreshold(Priority threshold) {
        this.threshold = threshold;
    }

    public static class NoOpErrorHandler implements ErrorHandler {
        @Override
        public void setLogger(Logger logger) {

        }

        @Override
        public void error(String message, Exception e, int errorCode) {

        }

        @Override
        public void error(String message) {

        }

        @Override
        public void error(String message, Exception e, int errorCode, LoggingEvent event) {

        }

        @Override
        public void setAppender(Appender appender) {

        }

        @Override
        public void setBackupAppender(Appender appender) {

        }
    }
}
