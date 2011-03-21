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
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
@Plugin(name="filters", type="Core", printObject=true)
public class Filters implements Iterable<Filter> {

    private final List<Filter> filters;
    private final boolean hasFilters;
    public static final Filters EMPTY_FILTERS = new Filters();

    private Filters() {
        this.filters = new ArrayList<Filter>();
        this.hasFilters = false;
    }

    private Filters(List<Filter> filters) {
        if (filters == null) {
            this.filters = Collections.unmodifiableList(new ArrayList<Filter>());
            this.hasFilters = false;
            return;
        }
        this.filters = Collections.unmodifiableList(filters);
        this.hasFilters = this.filters.size() > 0;
    }

    public Filters addFilter(Filter filter) {
        List<Filter> filters = new ArrayList<Filter>(this.filters);
        filters.add(filter);
        return new Filters(Collections.unmodifiableList(filters));
    }

    public Filters removeFilter(Filter filter) {
        List<Filter> filters = new ArrayList<Filter>(this.filters);
        filters.remove(filter);
        return new Filters(Collections.unmodifiableList(filters));
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

    @PluginFactory
    public static Filters createFilters(@PluginElement("filters") Filter[] filters) {
        List<Filter> f = filters == null || filters.length == 0 ? new ArrayList<Filter>() : Arrays.asList(filters);
        return new Filters(f);
    }

}
