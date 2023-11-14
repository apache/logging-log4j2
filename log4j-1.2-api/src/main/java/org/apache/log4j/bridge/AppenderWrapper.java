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
package org.apache.log4j.bridge;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.bridge.AppenderAdapter.Adapter;
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
     * Adapts a Log4j 2.x appender into a Log4j 1.x appender. Applying this method
     * on the result of {@link AppenderAdapter#adapt(Appender)} should return the
     * original Log4j 1.x appender.
     *
     * @param appender a Log4j 2.x appender
     * @return a Log4j 1.x appender or {@code null} if the parameter is {@code null}
     */
    public static Appender adapt(final org.apache.logging.log4j.core.Appender appender) {
        if (appender instanceof Appender) {
            return (Appender) appender;
        }
        if (appender instanceof Adapter) {
            final Adapter adapter = (Adapter) appender;
            // Don't unwrap an appender with filters
            if (!adapter.hasFilter()) {
                return adapter.getAppender();
            }
        }
        if (appender != null) {
            return new AppenderWrapper(appender);
        }
        return null;
    }

    /**
     * Constructs a new instance for a Core Appender.
     *
     * @param appender a Core Appender.
     */
    public AppenderWrapper(final org.apache.logging.log4j.core.Appender appender) {
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
    public void addFilter(final Filter newFilter) {
        if (appender instanceof AbstractFilterable) {
            ((AbstractFilterable) appender).addFilter(FilterAdapter.adapt(newFilter));
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
    public void doAppend(final LoggingEvent event) {
        if (event instanceof LogEventAdapter) {
            appender.append(((LogEventAdapter) event).getEvent());
        }
    }

    @Override
    public String getName() {
        return appender.getName();
    }

    @Override
    public void setErrorHandler(final ErrorHandler errorHandler) {
        appender.setHandler(new ErrorHandlerAdapter(errorHandler));
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return ((ErrorHandlerAdapter) appender.getHandler()).getHandler();
    }

    @Override
    public void setLayout(final Layout layout) {
        // Log4j 2 doesn't support this.
    }

    @Override
    public Layout getLayout() {
        return new LayoutWrapper(appender.getLayout());
    }

    @Override
    public void setName(final String name) {
        // Log4j 2 doesn't support this.
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }
}
