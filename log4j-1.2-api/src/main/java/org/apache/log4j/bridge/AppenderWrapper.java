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
package org.apache.log4j.bridge;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.filter.AbstractFilterable;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Wraps a Log4j 2 Appender in an empty Log4j 1 Appender so it can be extracted when constructing the configuration.
 * Allows a Log4j 1 Appender to reference a Log4j 2 Appender.
 */
public class AppenderWrapper implements Appender {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private final org.apache.logging.log4j.core.Appender appender;

    /**
     * Constructs a new instance for a Core Appender.
     *
     * @param appender a Core Appender.
     */
    public AppenderWrapper(org.apache.logging.log4j.core.Appender appender) {
        this.appender = appender;
    }

    /**
     * Gets the wrapped Core Appender.
     *
     * @return the wrapped Core Appender.
     */
    public org.apache.logging.log4j.core.Appender getAppender() {
        return appender;
    }

    @Override
    public void addFilter(Filter newFilter) {
        if (appender instanceof AbstractFilterable) {
            ((AbstractFilterable) appender).addFilter(FilterAdapter.convertFilter(newFilter));
        } else {
            LOGGER.warn("Unable to add filter to appender {}, it does not support filters", appender.getName());
        }
    }

    @Override
    public Filter getFilter() {
        return null;
    }

    @Override
    public void clearFilters() {
        // noop
    }

    @Override
    public void close() {
        // Not supported with Log4j 2.
    }

    @Override
    public void doAppend(LoggingEvent event) {
        if (event instanceof LogEventAdapter) {
            appender.append(((LogEventAdapter) event).getEvent());
        }
    }

    @Override
    public String getName() {
        return appender.getName();
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        appender.setHandler(new ErrorHandlerAdapter(errorHandler));
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return ((ErrorHandlerAdapter) appender.getHandler()).getHandler();
    }

    @Override
    public void setLayout(Layout layout) {
        // Log4j 2 doesn't support this.
    }

    @Override
    public Layout getLayout() {
        return new LayoutWrapper(appender.getLayout());
    }

    @Override
    public void setName(String name) {
        // Log4j 2 doesn't support this.
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }
}
