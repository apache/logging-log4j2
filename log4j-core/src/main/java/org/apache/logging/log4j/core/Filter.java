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
package org.apache.logging.log4j.core;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.EnglishEnums;

/**
 * Interface that must be implemented to allow custom event filtering. It is highly recommended that
 * applications make use of the Filters provided with this implementation before creating their own.
 *
 * <p>This interface supports "global" filters (i.e. - all events must pass through them first), attached to
 * specific loggers and associated with Appenders. It is recommended that, where possible, Filter implementations
 * create a generic filtering method that can be called from any of the filter methods.</p>
 */
public interface Filter extends LifeCycle {

    /**
     * The empty array.
     */
    Filter[] EMPTY_ARRAY = {};

    /**
     * Main {@linkplain org.apache.logging.log4j.core.config.plugins.Plugin#elementType() plugin element type} for
     * Filter plugins.
     *
     * @since 2.1
     */
    String ELEMENT_TYPE = "filter";

    /**
     * The result that can returned from a filter method call.
     */
    enum Result {
        /**
         * The event will be processed without further filtering based on the log Level.
         */
        ACCEPT,
        /**
         * No decision could be made, further filtering should occur.
         */
        NEUTRAL,
        /**
         * The event should not be processed.
         */
        DENY;

        /**
         * Returns the Result for the given string.
         *
         * @param name The Result enum name, case-insensitive. If null, returns, null
         * @return a Result enum value or null if name is null
         */
        public static Result toResult(final String name) {
            return toResult(name, null);
        }

        /**
         * Returns the Result for the given string.
         *
         * @param name The Result enum name, case-insensitive. If null, returns, defaultResult
         * @param defaultResult the Result to return if name is null
         * @return a Result enum value ({@code defaultResult} if name is null)
         */
        public static Result toResult(final String name, final Result defaultResult) {
            return EnglishEnums.valueOf(Result.class, name, defaultResult);
        }
    }

    /**
     * Returns the result that should be returned when the filter does not match the event.
     * @return the Result that should be returned when the filter does not match the event.
     */
    Result getOnMismatch();
    /**
     * Returns the result that should be returned when the filter matches the event.
     * @return the Result that should be returned when the filter matches the event.
     */
    Result getOnMatch();

    /**
     * Filter an event.
     * @param logger The Logger.
     * @param level The event logging Level.
     * @param marker The Marker for the event or null.
     * @param msg String text to filter on.
     * @param params An array of parameters or null.
     * @return the Result.
     */
    Result filter(Logger logger, Level level, Marker marker, String msg, Object... params);

    /**
     * Filter an event.
     *
     * @param logger The Logger.
     * @param level the event logging level.
     * @param marker The Marker for the event or null.
     * @param message The message.
     * @param p0 the message parameters
     * @return the Result.
     */
    Result filter(Logger logger, Level level, Marker marker, String message, Object p0);

    /**
     * Filter an event.
     *
     * @param logger The Logger.
     * @param level the event logging level.
     * @param marker The Marker for the event or null.
     * @param message The message.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @return the Result.
     */
    Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1);

    /**
     * Filter an event.
     *
     * @param logger The Logger.
     * @param level the event logging level.
     * @param marker The Marker for the event or null.
     * @param message The message.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @return the Result.
     */
    Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2);

    /**
     * Filter an event.
     *
     * @param logger The Logger.
     * @param level the event logging level.
     * @param marker The Marker for the event or null.
     * @param message The message.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @return the Result.
     */
    Result filter(
            Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3);

    /**
     * Filter an event.
     *
     * @param logger The Logger.
     * @param level the event logging level.
     * @param marker The Marker for the event or null.
     * @param message The message.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @param p4 the message parameters
     * @return the Result.
     */
    Result filter(
            Logger logger,
            Level level,
            Marker marker,
            String message,
            Object p0,
            Object p1,
            Object p2,
            Object p3,
            Object p4);

    /**
     * Filter an event.
     *
     * @param logger The Logger.
     * @param level the event logging level.
     * @param marker The Marker for the event or null.
     * @param message The message.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @param p4 the message parameters
     * @param p5 the message parameters
     * @return the Result.
     */
    Result filter(
            Logger logger,
            Level level,
            Marker marker,
            String message,
            Object p0,
            Object p1,
            Object p2,
            Object p3,
            Object p4,
            Object p5);

    /**
     * Filter an event.
     *
     * @param logger The Logger.
     * @param level the event logging level.
     * @param marker The Marker for the event or null.
     * @param message The message.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @param p4 the message parameters
     * @param p5 the message parameters
     * @param p6 the message parameters
     * @return the Result.
     */
    Result filter(
            Logger logger,
            Level level,
            Marker marker,
            String message,
            Object p0,
            Object p1,
            Object p2,
            Object p3,
            Object p4,
            Object p5,
            Object p6);

    /**
     * Filter an event.
     *
     * @param logger The Logger.
     * @param level the event logging level.
     * @param marker The Marker for the event or null.
     * @param message The message.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @param p4 the message parameters
     * @param p5 the message parameters
     * @param p6 the message parameters
     * @param p7 the message parameters
     * @return the Result.
     */
    Result filter(
            Logger logger,
            Level level,
            Marker marker,
            String message,
            Object p0,
            Object p1,
            Object p2,
            Object p3,
            Object p4,
            Object p5,
            Object p6,
            Object p7);

    /**
     * Filter an event.
     *
     * @param logger The Logger.
     * @param level the event logging level.
     * @param marker The Marker for the event or null.
     * @param message The message.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @param p4 the message parameters
     * @param p5 the message parameters
     * @param p6 the message parameters
     * @param p7 the message parameters
     * @param p8 the message parameters
     * @return the Result.
     */
    Result filter(
            Logger logger,
            Level level,
            Marker marker,
            String message,
            Object p0,
            Object p1,
            Object p2,
            Object p3,
            Object p4,
            Object p5,
            Object p6,
            Object p7,
            Object p8);

    /**
     * Filter an event.
     *
     * @param logger The Logger.
     * @param level the event logging level.
     * @param marker The Marker for the event or null.
     * @param message The message.
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
     * @return the Result.
     */
    Result filter(
            Logger logger,
            Level level,
            Marker marker,
            String message,
            Object p0,
            Object p1,
            Object p2,
            Object p3,
            Object p4,
            Object p5,
            Object p6,
            Object p7,
            Object p8,
            Object p9);

    /**
     * Filter an event.
     * @param logger The Logger.
     * @param level The event logging Level.
     * @param marker The Marker for the event or null.
     * @param msg Any Object.
     * @param t A Throwable or null.
     * @return the Result.
     */
    Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t);

    /**
     * Filter an event.
     * @param logger The Logger.
     * @param level The event logging Level.
     * @param marker The Marker for the event or null.
     * @param msg The Message
     * @param t A Throwable or null.
     * @return the Result.
     */
    Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t);

    /**
     * Filter an event.
     * @param logger The Logger.
     * @param level The event logging Level.
     * @param marker The Marker for the event or null.
     * @param msg The Message
     * @return the Result.
     */
    default Result filter(Logger logger, Level level, Marker marker, String msg) {
        return filter(logger, level, marker, msg, Constants.EMPTY_OBJECT_ARRAY);
    }

    /**
     * Filter an event.
     * @param event The Event to filter on.
     * @return the Result.
     */
    Result filter(LogEvent event);
}
