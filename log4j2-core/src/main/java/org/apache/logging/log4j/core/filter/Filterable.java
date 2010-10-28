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

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Lifecycle;
import org.apache.logging.log4j.core.LogEvent;

import java.util.Iterator;

/**
 *
 */
public class Filterable {
    private volatile Filters filters = new Filters(null);
    private boolean hasFilters;

    public synchronized void addFilter(Filter filter) {
        filters = Filters.addFilter(filters, filter);
        hasFilters = filters.hasFilters();
    }

    public synchronized void removeFilter(Filter filter) {
        filters = Filters.removeFilter(filters, filter);
         hasFilters = filters.hasFilters();
    }

    public synchronized void clearFilters() {
        filters = new Filters(null);
        hasFilters = false;
    }

    public Iterator<Filter> getFilters() {
        return filters.iterator();
    }

    public boolean hasFilters() {
        return hasFilters;
    }

    public int filterCount() {
        return filters.size();
    }

    protected void startFilters() {
        for (Filter filter : filters) {
            if ((filter instanceof Lifecycle)) {
                ((Lifecycle)filter).start();
            }
        }
    }

    protected void stopFilters() {
        for (Filter filter : filters) {
            if ((filter instanceof Lifecycle)) {
                ((Lifecycle)filter).stop();
            }
        }
    }

    protected synchronized void setFilters(Filters newFilters) {
        filters = newFilters == null ? new Filters(null) : newFilters;
        hasFilters = filters.hasFilters();
    }

    protected boolean isFiltered(LogEvent event) {
        if (hasFilters) {
            for (Filter filter : filters) {
                if (filter.filter(event) == Filter.Result.DENY) {
                    return true;
                }
            }
        }
        return false;
    }

}
