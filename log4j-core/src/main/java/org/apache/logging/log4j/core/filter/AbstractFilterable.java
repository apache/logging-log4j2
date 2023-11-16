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

import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LifeCycle2;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.PluginElement;

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

        // We are calling this attribute propertyArray because we use the more generic "properties" in several places
        // with different types: Array, Map and List.
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

        /**
         * Sets the filter.
         *
         * @param filter The filter
         * @return this
         * @deprecated Use {@link #setFilter(Filter)}.
         */
        @Deprecated
        public B withFilter(final Filter filter) {
            return setFilter(filter);
        }
    }

    /**
     * May be null.
     */
    private volatile Filter filter;

    @PluginElement("Properties")
    private final Property[] propertyArray;

    protected AbstractFilterable() {
        this(null, Property.EMPTY_ARRAY);
    }

    protected AbstractFilterable(final Filter filter) {
        this(filter, Property.EMPTY_ARRAY);
    }

    /**
     * @since 2.11.2
     */
    protected AbstractFilterable(final Filter filter, final Property[] propertyArray) {
        this.filter = filter;
        this.propertyArray = propertyArray == null ? Property.EMPTY_ARRAY : propertyArray;
    }

    /**
     * Adds a filter.
     * @param filter The Filter to add.
     */
    @Override
    public synchronized void addFilter(final Filter filter) {
        if (filter == null) {
            return;
        }
        if (this.filter == null) {
            this.filter = filter;
        } else if (this.filter instanceof CompositeFilter) {
            this.filter = ((CompositeFilter) this.filter).addFilter(filter);
        } else {
            final Filter[] filters = new Filter[] {this.filter, filter};
            this.filter = CompositeFilter.createFilters(filters);
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
    public synchronized void removeFilter(final Filter filter) {
        if (this.filter == null || filter == null) {
            return;
        }
        if (this.filter == filter || this.filter.equals(filter)) {
            this.filter = null;
        } else if (this.filter instanceof CompositeFilter) {
            CompositeFilter composite = (CompositeFilter) this.filter;
            composite = composite.removeFilter(filter);
            if (composite.size() > 1) {
                this.filter = composite;
            } else if (composite.size() == 1) {
                final Iterator<Filter> iter = composite.iterator();
                this.filter = iter.next();
            } else {
                this.filter = null;
            }
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
            if (filter instanceof LifeCycle2) {
                stopped = ((LifeCycle2) filter).stop(timeout, timeUnit);
            } else {
                filter.stop();
                stopped = true;
            }
        }
        if (changeLifeCycleState) {
            this.setStopped();
        }
        return stopped;
    }

    public Property[] getPropertyArray() {
        return propertyArray;
    }
}
