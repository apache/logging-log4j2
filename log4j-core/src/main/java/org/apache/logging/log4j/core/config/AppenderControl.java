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
package org.apache.logging.log4j.core.config;

import java.util.Objects;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.filter.AbstractFilterable;
import org.apache.logging.log4j.core.filter.Filterable;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Wraps an {@link Appender} with details an appender implementation shouldn't need to know about.
 */
public class AppenderControl extends AbstractFilterable {

    /**
     * The empty array.
     */
    static final AppenderControl[] EMPTY_ARRAY = {};

    private final ThreadLocal<AppenderControl> recursive = new ThreadLocal<>();
    private final Appender appender;
    private final Level level;
    private final int intLevel;
    private final String appenderName;

    /**
     * Constructor.
     *
     * @param appender The target Appender.
     * @param level the Level to filter on.
     * @param filter the Filter(s) to apply.
     */
    public AppenderControl(final Appender appender, final Level level, final Filter filter) {
        super(filter);
        this.appender = Objects.requireNonNull(appender, "appender");
        this.appenderName = appender.getName();
        this.level = level;
        this.intLevel = level == null ? Level.ALL.intLevel() : level.intLevel();
        start();
    }

    /**
     * Returns the name the appender had when this AppenderControl was constructed.
     *
     * @return the appender name
     */
    public String getAppenderName() {
        return appenderName;
    }

    /**
     * Returns the Appender.
     *
     * @return the Appender.
     */
    public Appender getAppender() {
        return appender;
    }

    /**
     * Call the appender.
     *
     * @param event The event to process.
     */
    public void callAppender(final LogEvent event) {
        if (shouldSkip(event)) {
            return;
        }
        callAppenderPreventRecursion(event);
    }

    private boolean shouldSkip(final LogEvent event) {
        return isFilteredByAppenderControl(event) || isFilteredByLevel(event) || isRecursiveCall();
    }

    @PerformanceSensitive
    private boolean isFilteredByAppenderControl(final LogEvent event) {
        final Filter filter = getFilter();
        return filter != null && Filter.Result.DENY == filter.filter(event);
    }

    @PerformanceSensitive
    private boolean isFilteredByLevel(final LogEvent event) {
        return level != null && intLevel < event.getLevel().intLevel();
    }

    @PerformanceSensitive
    private boolean isRecursiveCall() {
        if (recursive.get() != null) {
            appenderErrorHandlerMessage("Recursive call to appender ");
            return true;
        }
        return false;
    }

    private String appenderErrorHandlerMessage(final String prefix) {
        final String result = createErrorMsg(prefix);
        appender.getHandler().error(result);
        return result;
    }

    private void callAppenderPreventRecursion(final LogEvent event) {
        try {
            recursive.set(this);
            callAppender0(event);
        } finally {
            recursive.set(null);
        }
    }

    private void callAppender0(final LogEvent event) {
        ensureAppenderStarted();
        if (!isFilteredByAppender(event)) {
            tryCallAppender(event);
        }
    }

    private void ensureAppenderStarted() {
        if (!appender.isStarted()) {
            handleError("Attempted to append to non-started appender ");
        }
    }

    private void handleError(final String prefix) {
        final String msg = appenderErrorHandlerMessage(prefix);
        if (!appender.ignoreExceptions()) {
            throw new AppenderLoggingException(msg);
        }
    }

    private String createErrorMsg(final String prefix) {
        return prefix + appender.getName();
    }

    private boolean isFilteredByAppender(final LogEvent event) {
        return appender instanceof Filterable && ((Filterable) appender).isFiltered(event);
    }

    private void tryCallAppender(final LogEvent event) {
        try {
            appender.append(event);
        } catch (final RuntimeException error) {
            handleAppenderError(event, error);
        } catch (final Throwable throwable) {
            handleAppenderError(event, new AppenderLoggingException(throwable));
        }
    }

    private void handleAppenderError(final LogEvent event, final RuntimeException ex) {
        appender.getHandler().error(createErrorMsg("An exception occurred processing Appender "), event, ex);
        if (!appender.ignoreExceptions()) {
            throw ex;
        }
    }

    // AppenderControl is a helper object whose purpose is to make it
    // easier for LoggerConfig to manage and invoke Appenders.
    // LoggerConfig manages Appenders by their name. To facilitate this,
    // two AppenderControl objects are considered equal if and only
    // if they have the same appender name.
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof AppenderControl)) {
            return false;
        }
        final AppenderControl other = (AppenderControl) obj;
        return Objects.equals(appenderName, other.appenderName);
    }

    @Override
    public int hashCode() {
        return appenderName.hashCode();
    }

    @Override
    public String toString() {
        return super.toString() + "[appender=" + appender + ", appenderName=" + appenderName + ", level=" + level
                + ", intLevel=" + intLevel + ", recursive=" + recursive + ", filter=" + getFilter() + "]";
    }
}
