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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.message.Message;

/**
 * Users should extend this class to implement filters. Filters can be either context wide or attached to
 * an appender. A filter may choose to support being called only from the context or only from an appender in
 * which case it will only implement the required method(s). The rest will default to return {@link org.apache.logging.log4j.core.Filter.Result#NEUTRAL}.
 * <p>
 * Garbage-free note: the methods with unrolled varargs by default delegate to the
 * {@link #filter(Logger, Level, Marker, String, Object...) filter method with vararg parameters}.
 * Subclasses that want to be garbage-free should override these methods to implement the appropriate filtering
 * without creating a vararg array.
 * </p>
 */
public abstract class AbstractFilter extends AbstractLifeCycle implements Filter {

    public abstract static class AbstractFilterBuilder<B extends AbstractFilterBuilder<B>> {

        public static final String ATTR_ON_MISMATCH = "onMismatch";
        public static final String ATTR_ON_MATCH = "onMatch";

        @PluginBuilderAttribute(ATTR_ON_MATCH)
        private Result onMatch = Result.NEUTRAL;

        @PluginBuilderAttribute(ATTR_ON_MISMATCH)
        private Result onMismatch = Result.DENY;

        public Result getOnMatch() {
            return onMatch;
        }

        public Result getOnMismatch() {
            return onMismatch;
        }

        /**
         * Sets the Result to return when the filter matches. Defaults to Result.NEUTRAL.
         * @param onMatch the Result to return when the filter matches.
         * @return this
         */
        public B setOnMatch(final Result onMatch) {
            this.onMatch = onMatch;
            return asBuilder();
        }

        /**
         * Sets the Result to return when the filter does not match. The default is Result.DENY.
         * @param onMismatch the Result to return when the filter does not match.
         * @return this
         */
        public B setOnMismatch(final Result onMismatch) {
            this.onMismatch = onMismatch;
            return asBuilder();
        }

        @SuppressWarnings("unchecked")
        public B asBuilder() {
            return (B) this;
        }
    }

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
    protected boolean equalsImpl(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equalsImpl(obj)) {
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
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final Message msg, final Throwable t) {
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
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final Object msg, final Throwable t) {
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
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final String msg, final Object... params) {
        return Result.NEUTRAL;
    }

    /**
     * Appender Filter method. The default returns NEUTRAL.
     * @param logger the Logger.
     * @param level The logging Level.
     * @param marker The Marker, if any.
     * @param msg The message, if present.
     * @param p0 the message parameters
     * @return The Result of filtering.
     * @since 2.7
     */
    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final String msg, final Object p0) {
        return filter(logger, level, marker, msg, new Object[] {p0});
    }

    /**
     * Appender Filter method. The default returns NEUTRAL.
     * @param logger the Logger.
     * @param level The logging Level.
     * @param marker The Marker, if any.
     * @param msg The message, if present.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @return The Result of filtering.
     * @since 2.7
     */
    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1) {
        return filter(logger, level, marker, msg, new Object[] {p0, p1});
    }

    /**
     * Appender Filter method. The default returns NEUTRAL.
     * @param logger the Logger.
     * @param level The logging Level.
     * @param marker The Marker, if any.
     * @param msg The message, if present.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @return The Result of filtering.
     * @since 2.7
     */
    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2) {
        return filter(logger, level, marker, msg, new Object[] {p0, p1, p2});
    }

    /**
     * Appender Filter method. The default returns NEUTRAL.
     * @param logger the Logger.
     * @param level The logging Level.
     * @param marker The Marker, if any.
     * @param msg The message, if present.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @return The Result of filtering.
     * @since 2.7
     */
    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        return filter(logger, level, marker, msg, new Object[] {p0, p1, p2, p3});
    }

    /**
     * Appender Filter method. The default returns NEUTRAL.
     * @param logger the Logger.
     * @param level The logging Level.
     * @param marker The Marker, if any.
     * @param msg The message, if present.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @param p4 the message parameters
     * @return The Result of filtering.
     * @since 2.7
     */
    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {
        return filter(logger, level, marker, msg, new Object[] {p0, p1, p2, p3, p4});
    }

    /**
     * Appender Filter method. The default returns NEUTRAL.
     * @param logger the Logger.
     * @param level The logging Level.
     * @param marker The Marker, if any.
     * @param msg The message, if present.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @param p4 the message parameters
     * @param p5 the message parameters
     * @return The Result of filtering.
     * @since 2.7
     */
    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        return filter(logger, level, marker, msg, new Object[] {p0, p1, p2, p3, p4, p5});
    }

    /**
     * Appender Filter method. The default returns NEUTRAL.
     * @param logger the Logger.
     * @param level The logging Level.
     * @param marker The Marker, if any.
     * @param msg The message, if present.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @param p4 the message parameters
     * @param p5 the message parameters
     * @param p6 the message parameters
     * @return The Result of filtering.
     * @since 2.7
     */
    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        return filter(logger, level, marker, msg, new Object[] {p0, p1, p2, p3, p4, p5, p6});
    }

    /**
     * Appender Filter method. The default returns NEUTRAL.
     * @param logger the Logger.
     * @param level The logging Level.
     * @param marker The Marker, if any.
     * @param msg The message, if present.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @param p4 the message parameters
     * @param p5 the message parameters
     * @param p6 the message parameters
     * @param p7 the message parameters
     * @return The Result of filtering.
     * @since 2.7
     */
    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        return filter(logger, level, marker, msg, new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
    }

    /**
     * Appender Filter method. The default returns NEUTRAL.
     * @param logger the Logger.
     * @param level The logging Level.
     * @param marker The Marker, if any.
     * @param msg The message, if present.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @param p4 the message parameters
     * @param p5 the message parameters
     * @param p6 the message parameters
     * @param p7 the message parameters
     * @param p8 the message parameters
     * @return The Result of filtering.
     * @since 2.7
     */
    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        return filter(logger, level, marker, msg, new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
    }

    /**
     * Appender Filter method. The default returns NEUTRAL.
     * @param logger the Logger.
     * @param level The logging Level.
     * @param marker The Marker, if any.
     * @param msg The message, if present.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @param p4 the message parameters
     * @param p5 the message parameters
     * @param p6 the message parameters
     * @param p7 the message parameters
     * @param p8 the message parameters
     * @param p9 the message parameters
     * @return The Result of filtering.
     * @since 2.7
     */
    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        return filter(logger, level, marker, msg, new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9});
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
    protected int hashCodeImpl() {
        final int prime = 31;
        int result = super.hashCodeImpl();
        result = prime * result + ((onMatch == null) ? 0 : onMatch.hashCode());
        result = prime * result + ((onMismatch == null) ? 0 : onMismatch.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
