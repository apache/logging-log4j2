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
public class Filterable implements Filtering {

    private volatile Filter filter = null;

    protected Filterable(Filter filter) {
        this.filter = filter;
    }

    protected Filterable() {
    }

    /**
     * Return the Filter
     * @return the Filter.
     */
    public Filter getFilter() {
        return filter;
    }

    public synchronized void addFilter(Filter filter) {
        if (this.filter == null) {
            this.filter = filter;
        } else if (filter instanceof CompositeFilter) {
            this.filter = ((CompositeFilter) this.filter).addFilter(filter);
        } else {
            Filter[] filters = new Filter[] {this.filter, filter};
            this.filter = CompositeFilter.createFilters(filters);
        }
    }

    public synchronized void removeFilter(Filter filter) {
        if (this.filter == filter) {
            this.filter = null;
        } else if (filter instanceof CompositeFilter) {
            CompositeFilter composite = (CompositeFilter) filter;
            composite = composite.removeFilter(filter);
            if (composite.size() > 1) {
                this.filter = composite;
            } else if (composite.size() == 1) {
                Iterator<Filter> iter = composite.iterator();
                this.filter = iter.next();
            } else {
                this.filter = null;
            }
        }
    }

    public boolean hasFilter() {
        return filter != null;
    }

    public void startFilter() {
       if (filter != null && filter instanceof Lifecycle) {
           ((Lifecycle) filter).start();
       }
    }

    public void stopFilter() {
       if (filter != null && filter instanceof Lifecycle) {
           ((Lifecycle) filter).stop();
       }
    }

    public boolean isFiltered(LogEvent event) {
        return filter != null && filter.filter(event) == Filter.Result.DENY;
    }

}
