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
package org.apache.logging.log4j.core.filter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.plugins.PluginElement;

/**
 * Enhances a Class by allowing it to contain Filters.
 */
public abstract class AbstractFilterable extends AbstractLifeCycle implements Filterable {

    /**
     * Subclasses can extend this abstract Builder.
     *
     * @param <B> The type to build.
     */
    public abstract static class Builder<B extends Builder<B>> {

        @PluginElement("Filter")
        private Filter filter;

        @PluginElement("Properties")
        private Property[] propertyArray;

        @SuppressWarnings("unchecked")
        public B asBuilder() {
            return (B) this;
        }

        public Filter getFilter() {
            return filter;
        }

        public Property[] getPropertyArray() {
            return propertyArray;
        }

        public B setFilter(final Filter filter) {
            this.filter = filter;
            return asBuilder();
        }

        public B setPropertyArray(final Property[] properties) {
            this.propertyArray = properties;
            return asBuilder();
        }

    }

    /**
     * May be null.
     */
    private volatile Filter filter;

    private final Lock filterLock = new ReentrantLock();

    private final Property[] propertyArray;

    protected AbstractFilterable() {
        this(null, null);
    }

    protected AbstractFilterable(final Filter filter, final Property[] propertyArray) {
        this.filter = filter;
        this.propertyArray = propertyArray == null ? Property.EMPTY_ARRAY : propertyArray;
    }

    /**
     * Adds a filter.
     * @param filter The Filter to add.
     */
    @Override
    public void addFilter(final Filter filter) {
        if (filter == null) {
            return;
        }
        filterLock.lock();
        try {
            final var currentFilter = this.filter;
            if (currentFilter == null) {
                this.filter = filter;
            } else if (currentFilter instanceof CompositeFilter) {
                this.filter = ((CompositeFilter) currentFilter).addFilter(filter);
            } else {
                final Filter[] filters = new Filter[]{currentFilter, filter};
                this.filter = CompositeFilter.createFilters(filters);
            }
        } finally {
            filterLock.unlock();
        }
    }

    /**
     * Returns the Filter.
     * @return the Filter or null.
     */
    @Override
    public Filter getFilter() {
        return filter;
    }

    public Property[] getPropertyArray() {
        return propertyArray;
    }

    /**
     * Determines if a Filter is present.
     * @return false if no Filter is present.
     */
    @Override
    public boolean hasFilter() {
        return filter != null;
    }

    /**
     * Determine if the LogEvent should be processed or ignored.
     * @param event The LogEvent.
     * @return true if the LogEvent should be processed.
     */
    @Override
    public boolean isFiltered(final LogEvent event) {
        return filter != null && filter.filter(event) == Filter.Result.DENY;
    }

    /**
     * Removes a Filter.
     * @param filter The Filter to remove.
     */
    @Override
    public void removeFilter(final Filter filter) {
        if (filter == null) {
            return;
        }
        filterLock.lock();
        try {
            final var currentFilter = this.filter;
            if (currentFilter == filter || filter.equals(currentFilter)) {
                this.filter = null;
            } else if (currentFilter instanceof CompositeFilter) {
                CompositeFilter composite = (CompositeFilter) currentFilter;
                composite = composite.removeFilter(filter);
                if (composite.isEmpty()) {
                    this.filter = null;
                } else if (composite.size() == 1) {
                    this.filter = composite.iterator().next();
                } else {
                    this.filter = composite;
                }
            }
        } finally {
            filterLock.unlock();
        }
    }

    /**
     * Make the Filter available for use.
     */
    @Override
    public void start() {
        this.setStarting();
        if (filter != null) {
            filter.start();
        }
        this.setStarted();
    }

    /**
     * Cleanup the Filter.
     */
    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        return stop(timeout, timeUnit, true);
    }

    /**
     * Cleanup the Filter.
     */
    protected boolean stop(final long timeout, final TimeUnit timeUnit, final boolean changeLifeCycleState) {
        if (changeLifeCycleState) {
            this.setStopping();
        }
        boolean stopped = true;
        if (filter != null) {
            stopped = filter.stop(timeout, timeUnit);
        }
        if (changeLifeCycleState) {
            this.setStopped();
        }
        return stopped;
    }

}
