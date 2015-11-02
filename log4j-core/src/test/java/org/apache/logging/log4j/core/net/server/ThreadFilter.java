package org.apache.logging.log4j.core.net.server;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.filter.AbstractFilter;

public class ThreadFilter extends AbstractFilter {

    private static final long serialVersionUID = 1L;

    public ThreadFilter(final Result onMatch, final Result onMismatch) {
        super(onMatch, onMismatch);
    }

    @Override
    public Filter.Result filter(final LogEvent event) {
        return event.getThreadName().equals(Thread.currentThread().getName()) ? onMatch : onMismatch;
    }
}