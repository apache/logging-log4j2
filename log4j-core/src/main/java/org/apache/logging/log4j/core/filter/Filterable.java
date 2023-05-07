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

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LogEvent;

/**
 * Interface implemented by Classes that allow filtering to occur.
 *
 * <p>
 * Extends {@link LifeCycle} since filters have a life cycle.
 * </p>
 */
public interface Filterable extends LifeCycle {

    /**
     * Adds a new Filter. If a Filter already exists it is converted to a CompositeFilter.
     * @param filter The Filter to add.
     */
    void addFilter(Filter filter);

    /**
     * Removes a Filter.
     * @param filter The Filter to remove.
     */
    void removeFilter(Filter filter);

    /**
     * Returns an Iterator for all the Filters.
     * @return an Iterator for all the Filters.
     */
    Filter getFilter();

    /**
     * Determine if a Filter is present.
     * @return true if a Filter is present, false otherwise.
     */
    boolean hasFilter();

    /**
     * Determines if the event should be filtered.
     * @param event The LogEvent.
     * @return true if the event should be filtered, false otherwise.
     */
    boolean isFiltered(LogEvent event);
}
