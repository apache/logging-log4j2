package org.apache.logging.log4j.core.filter;

import org.apache.logging.log4j.core.Filter;

import java.util.Iterator;

/**
 * Interface implemented by Classes that allow filtering to occur.
 */
public interface Filtering {

    /**
     * Adds a new Filter.
     * @param filter The Filter to add.
     */
    void addFilter(Filter filter);

    /**
     * Removes a Filter.
     * @param filter The Filter to remove.
     */
    void removeFilter(Filter filter);

    /**
     * Removes all Filters.
     */
    void clearFilters();

    /**
     * Returns an Iterator for all the Filters.
     * @return an Iterator for all the Filters.
     */
    Iterator<Filter> getFilters();

    /**
     * Determins if any Filters are present.
     * @return true if any Filters are present, false otherwise.
     */
    boolean hasFilters();

    /**
     * Returns the number of Filters associated with the Object.
     * @return the number of Filters associated with the Object.
     */
    int filterCount();
}
