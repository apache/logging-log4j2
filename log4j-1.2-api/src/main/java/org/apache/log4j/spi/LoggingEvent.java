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
package org.apache.log4j.spi;

import java.util.Map;
import java.util.Set;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Priority;
import org.apache.log4j.bridge.LogEventAdapter;

/**
 *  No-op version of Log4j 1.2 LoggingEvent. This class is not directly used by Log4j 1.x clients but is used by
 *  the Log4j 2 LogEvent adapter to be compatible with Log4j 1.x components.
 */
public class LoggingEvent {

    /**
     * Returns the time when the application started, in milliseconds
     * elapsed since 01.01.1970.
     * @return the JVM start time.
     */
    public static long getStartTime() {
        return LogEventAdapter.getStartTime();
    }

    /**
     * The number of milliseconds elapsed from 1/1/1970 until logging event was created.
     */
    public final long timeStamp;

    /**
     * Constructs a new instance.
     */
    public LoggingEvent() {
        timeStamp = System.currentTimeMillis();
    }

    /**
     * Create new instance.
     *
     * @since 1.2.15
     * @param fqnOfCategoryClass Fully qualified class name of Logger implementation.
     * @param logger The logger generating this event.
     * @param timeStamp the timestamp of this logging event
     * @param level The level of this event.
     * @param message The message of this event.
     * @param threadName thread name
     * @param throwable The throwable of this event.
     * @param ndc Nested diagnostic context
     * @param info Location info
     * @param properties MDC properties
     */
    public LoggingEvent(
            final String fqnOfCategoryClass,
            final Category logger,
            final long timeStamp,
            final Level level,
            final Object message,
            final String threadName,
            final ThrowableInformation throwable,
            final String ndc,
            final LocationInfo info,
            final Map properties) {
        this.timeStamp = timeStamp;
    }

    /**
     * Instantiate a LoggingEvent from the supplied parameters.
     *
     * <p>
     * Except {@link #timeStamp} all the other fields of <code>LoggingEvent</code> are filled when actually needed.
     * <p>
     *
     * @param logger The logger generating this event.
     * @param timeStamp the timestamp of this logging event
     * @param level The level of this event.
     * @param message The message of this event.
     * @param throwable The throwable of this event.
     */
    public LoggingEvent(
            String fqnOfCategoryClass,
            Category logger,
            long timeStamp,
            Priority level,
            Object message,
            Throwable throwable) {
        this.timeStamp = timeStamp;
    }

    /**
     * Instantiate a LoggingEvent from the supplied parameters.
     *
     * <p>
     * Except {@link #timeStamp} all the other fields of <code>LoggingEvent</code> are filled when actually needed.
     * <p>
     *
     * @param logger The logger generating this event.
     * @param level The level of this event.
     * @param message The message of this event.
     * @param throwable The throwable of this event.
     */
    public LoggingEvent(
            final String fqnOfCategoryClass,
            final Category logger,
            final Priority level,
            final Object message,
            final Throwable throwable) {
        timeStamp = System.currentTimeMillis();
    }

    public String getFQNOfLoggerClass() {
        return null;
    }

    /**
     * Return the level of this event. Use this form instead of directly
     * accessing the <code>level</code> field.
     * @return Always returns null.
     */
    public Level getLevel() {
        return null;
    }

    /**
     * Set the location information for this logging event. The collected
     * information is cached for future use.
     * @return Always returns null.
     */
    public LocationInfo getLocationInformation() {
        return null;
    }

    /**
     * Gets the logger of the event.
     * Use should be restricted to cloning events.
     * @return Always returns null.
     * @since 1.2.15
     */
    public Category getLogger() {
        return null;
    }

    /**
     * Return the name of the logger. Use this form instead of directly
     * accessing the <code>categoryName</code> field.
     * @return Always returns null.
     */
    public String getLoggerName() {
        return null;
    }

    public Object getMDC(final String key) {
        return null;
    }

    /**
     * Obtain a copy of this thread's MDC prior to serialization or
     * asynchronous logging.
     */
    public void getMDCCopy() {}

    /**
     * Return the message for this logging event.
     *
     * <p>Before serialization, the returned object is the message
     * passed by the user to generate the logging event. After
     * serialization, the returned value equals the String form of the
     * message possibly after object rendering.
     * @return Always returns null.
     * @since 1.1 */
    public Object getMessage() {
        return null;
    }

    public String getNDC() {
        return null;
    }

    public Map getProperties() {
        return null;
    }

    public String getProperty(final String key) {
        return null;
    }

    public Set getPropertyKeySet() {
        return null;
    }

    public String getRenderedMessage() {
        return null;
    }

    public String getThreadName() {
        return null;
    }

    /**
     * Returns the throwable information contained within this
     * event. May be <code>null</code> if there is no such information.
     *
     * <p>Note that the {@link Throwable} object contained within a
     * {@link ThrowableInformation} does not survive serialization.
     * @return Always returns null.
     * @since 1.1 */
    public ThrowableInformation getThrowableInformation() {
        return null;
    }

    /**
     * Return this event's throwable's string[] representation.
     * @return Always returns null.
     */
    public String[] getThrowableStrRep() {
        return null;
    }

    public long getTimeStamp() {
        return 0;
    }

    public Object removeProperty(final String propName) {
        return null;
    }

    public void setProperty(final String propName, final String propValue) {}
}
