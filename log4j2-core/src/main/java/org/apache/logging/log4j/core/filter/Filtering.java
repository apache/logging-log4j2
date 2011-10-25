package org.apache.logging.log4j.core.filter;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;

import java.util.Iterator;

/**
 * Interface implemented by Classes that allow filtering to occur.
 */
public interface Filtering {

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
