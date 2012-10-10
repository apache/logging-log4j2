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
package org.apache.logging.log4j.core.filter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Lifecycle;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.message.Message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Composes and invokes one or more filters.
 */
@Plugin(name = "filters", type = "Core", printObject = true)
public final class CompositeFilter implements Iterable<Filter>, Filter, Lifecycle {

    private final List<Filter> filters;
    private final boolean hasFilters;

    private boolean isStarted;

    private CompositeFilter() {
        this.filters = new ArrayList<Filter>();
        this.hasFilters = false;
    }

    private CompositeFilter(List<Filter> filters) {
        if (filters == null) {
            this.filters = Collections.unmodifiableList(new ArrayList<Filter>());
            this.hasFilters = false;
            return;
        }
        this.filters = Collections.unmodifiableList(filters);
        this.hasFilters = this.filters.size() > 0;
    }

    public CompositeFilter addFilter(Filter filter) {
        List<Filter> filters = new ArrayList<Filter>(this.filters);
        filters.add(filter);
        return new CompositeFilter(Collections.unmodifiableList(filters));
    }

    public CompositeFilter removeFilter(Filter filter) {
        List<Filter> filters = new ArrayList<Filter>(this.filters);
        filters.remove(filter);
        return new CompositeFilter(Collections.unmodifiableList(filters));
    }

    public Iterator<Filter> iterator() {
        return filters.iterator();
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public boolean hasFilters() {
        return hasFilters;
    }

    public int size() {
        return filters.size();
    }

    public void start() {
        for (Filter filter : filters) {
            if (filter instanceof Lifecycle) {
                ((Lifecycle) filter).start();
            }
        }
        isStarted = true;
    }

    public void stop() {
        for (Filter filter : filters) {
            if (filter instanceof Lifecycle) {
                ((Lifecycle) filter).stop();
            }
        }
        isStarted = false;
    }

    public boolean isStarted() {
        return isStarted;
    }

    /**
     * Returns the result that should be returned when the filter does not match the event.
     * 
     * @return the Result that should be returned when the filter does not match the event.
     */
    public Result getOnMismatch() {
        return Result.NEUTRAL;
    }

    /**
     * Returns the result that should be returned when the filter matches the event.
     * 
     * @return the Result that should be returned when the filter matches the event.
     */
    public Result getOnMatch() {
        return Result.NEUTRAL;
    }

    /**
     * Filter an event.
     * 
     * @param logger
     *            The Logger.
     * @param level
     *            The event logging Level.
     * @param marker
     *            The Marker for the event or null.
     * @param msg
     *            String text to filter on.
     * @param params
     *            An array of parameters or null.
     * @return the Result.
     */
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
        Result result = Result.NEUTRAL;
        for (Filter filter : filters) {
            result = filter.filter(logger, level, marker, msg, params);
            if (result == Result.ACCEPT || result == Result.DENY) {
                return result;
            }
        }
        return result;
    }

    /**
     * Filter an event.
     * 
     * @param logger
     *            The Logger.
     * @param level
     *            The event logging Level.
     * @param marker
     *            The Marker for the event or null.
     * @param msg
     *            Any Object.
     * @param t
     *            A Throwable or null.
     * @return the Result.
     */
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        Result result = Result.NEUTRAL;
        for (Filter filter : filters) {
            result = filter.filter(logger, level, marker, msg, t);
            if (result == Result.ACCEPT || result == Result.DENY) {
                return result;
            }
        }
        return result;
    }

    /**
     * Filter an event.
     * 
     * @param logger
     *            The Logger.
     * @param level
     *            The event logging Level.
     * @param marker
     *            The Marker for the event or null.
     * @param msg
     *            The Message
     * @param t
     *            A Throwable or null.
     * @return the Result.
     */
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        Result result = Result.NEUTRAL;
        for (Filter filter : filters) {
            result = filter.filter(logger, level, marker, msg, t);
            if (result == Result.ACCEPT || result == Result.DENY) {
                return result;
            }
        }
        return result;
    }

    /**
     * Filter an event.
     * 
     * @param event
     *            The Event to filter on.
     * @return the Result.
     */
    public Result filter(LogEvent event) {
        Result result = Result.NEUTRAL;
        for (Filter filter : filters) {
            result = filter.filter(event);
            if (result == Result.ACCEPT || result == Result.DENY) {
                return result;
            }
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Filter filter : filters) {
            if (sb.length() == 0) {
                sb.append("{");
            } else {
                sb.append(", ");
            }
            sb.append(filter.toString());
        }
        if (sb.length() > 0) {
            sb.append("}");
        }
        return sb.toString();
    }

    /**
     * Create a CompositeFilter.
     * 
     * @param filters
     *            An array of Filters to call.
     * @return The CompositeFilter.
     */
    @PluginFactory
    public static CompositeFilter createFilters(@PluginElement("filters") Filter[] filters) {
        List<Filter> f = filters == null || filters.length == 0 ? new ArrayList<Filter>() : Arrays.asList(filters);
        return new CompositeFilter(f);
    }

}
