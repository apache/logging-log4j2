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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;

/**
 * Users should extend this class to implement filters. Filters can be either context wide or attached to
 * an appender. A filter may choose to support being called only from the context or only from an appender in
 * which case it will only implement the required method(s). The rest will default to return NEUTRAL.
 *
 */
public abstract class AbstractFilter extends AbstractLifeCycle implements Filter {
    
    private static final long serialVersionUID = 1L;

    /**
     * The onMatch Result.
     */
    protected final Result onMatch;

    /**
     * The onMismatch Result.
     */
    protected final Result onMismatch;

    /**
     * The default constructor.
     */
    protected AbstractFilter() {
        this(null, null);
    }

    /**
     * Constructor that allows the onMatch and onMismatch actions to be set.
     * @param onMatch The result to return when a match occurs.
     * @param onMismatch The result to return when a match dos not occur.
     */
    protected AbstractFilter(final Result onMatch, final Result onMismatch) {
        this.onMatch = onMatch == null ? Result.NEUTRAL : onMatch;
        this.onMismatch = onMismatch == null ? Result.DENY : onMismatch;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AbstractFilter other = (AbstractFilter) obj;
        if (onMatch != other.onMatch) {
            return false;
        }
        if (onMismatch != other.onMismatch) {
            return false;
        }
        return true;
    }

    /**
     * Context Filter method. The default returns NEUTRAL.
     * @param event The LogEvent.
     * @return The Result of filtering.
     */
    @Override
    public Result filter(final LogEvent event) {
        return Result.NEUTRAL;
    }

    /**
     * Appender Filter method. The default returns NEUTRAL.
     * @param logger the Logger.
     * @param level The logging Level.
     * @param marker The Marker, if any.
     * @param msg The message, if present.
     * @param t A throwable or null.
     * @return The Result of filtering.
     */
    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Message msg,
                         final Throwable t) {
        return Result.NEUTRAL;
    }

    /**
     * Appender Filter method. The default returns NEUTRAL.
     * @param logger the Logger.
     * @param level The logging Level.
     * @param marker The Marker, if any.
     * @param msg The message, if present.
     * @param t A throwable or null.
     * @return The Result of filtering.
     */
    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Object msg,
                         final Throwable t) {
        return Result.NEUTRAL;
    }

    /**
     * Appender Filter method. The default returns NEUTRAL.
     * @param logger the Logger.
     * @param level The logging Level.
     * @param marker The Marker, if any.
     * @param msg The message, if present.
     * @param params An array of parameters or null.
     * @return The Result of filtering.
     */
    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
                         final Object... params) {
        return Result.NEUTRAL;
    }

    /**
     * Returns the Result to be returned when a match occurs.
     * @return the onMatch Result.
     */
    @Override
    public final Result getOnMatch() {
        return onMatch;
    }

    /**
     * Returns the Result to be returned when a match does not occur.
     * @return the onMismatch Result.
     */
    @Override
    public final Result getOnMismatch() {
        return onMismatch;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((onMatch == null) ? 0 : onMatch.hashCode());
        result = prime * result + ((onMismatch == null) ? 0 : onMismatch.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
