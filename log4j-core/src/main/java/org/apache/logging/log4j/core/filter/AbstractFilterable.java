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

import java.util.Iterator;

import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;

/**
 * Enhances a Class by allowing it to contain Filters.
 */
public abstract class AbstractFilterable extends AbstractLifeCycle implements Filterable {

    private static final long serialVersionUID = 1L;

    /**
     * May be null.
     */
    private volatile CompositeFilter filter;

    protected AbstractFilterable(final Filter filter) {
        if (filter != null) {
            this.filter = CompositeFilter.createFilters(new Filter[]{filter});
        }
    }

    protected AbstractFilterable() {
    }

    /**
     * Returns the Filter.
     * @return the Filter or null.
     */
    @Override
    public Filter getFilter() {
        if (filter != null && filter.size() == 1) {
            final Iterator<Filter> iter = filter.iterator();
            return iter.next();
        }
        return filter;
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
            this.filter = CompositeFilter.createFilters(new Filter[] {filter});
        } else {
            this.filter = this.filter.addFilter(filter);
        }
    }

    /**
     * Removes a Filter.
     * @param filter The Filter to remove.
     */
    @Override
    public synchronized void removeFilter(final Filter filter) {
        if (this.filter == null) {
            return;
        }  else  {
            CompositeFilter temp = this.filter.removeFilter(filter);
            if (temp.size() >= 1) {
                this.filter = temp;
            }  else {
                this.filter = null;
            }
        }
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
    public void stop() {
        this.setStopping();
       if (filter != null) {
           filter.stop();
       }
       this.setStopped();
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

}
