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
package org.apache.log4j.bridge;

import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.logging.log4j.spi.StandardLevel;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * Converts a Log4j 2 LogEvent into the components needed by a Log4j 1.x LoggingEvent.
 * This class requires Log4j 2.
 */
public class LogEventAdapter extends LoggingEvent {

    private static final long JVM_START_TIME = initStartTime();

    private final LogEvent event;

    public LogEventAdapter(LogEvent event) {
        this.event = event;
    }

    /**
     * Returns the time when the application started, in milliseconds
     * elapsed since 01.01.1970.
     * @return the time when the JVM started.
     */
    public static long getStartTime() {
        return JVM_START_TIME;
    }

    /**
     * Returns the result of {@code ManagementFactory.getRuntimeMXBean().getStartTime()},
     * or the current system time if JMX is not available.
     */
    private static long initStartTime() {
        // We'd like to call ManagementFactory.getRuntimeMXBean().getStartTime(),
        // but Google App Engine throws a java.lang.NoClassDefFoundError
        // "java.lang.management.ManagementFactory is a restricted class".
        // The reflection is necessary because without it, Google App Engine
        // will refuse to initialize this class.
        try {
            final Class<?> factoryClass = Loader.loadSystemClass("java.lang.management.ManagementFactory");
            final Method getRuntimeMXBean = factoryClass.getMethod("getRuntimeMXBean");
            final Object runtimeMXBean = getRuntimeMXBean.invoke(null);

            final Class<?> runtimeMXBeanClass = Loader.loadSystemClass("java.lang.management.RuntimeMXBean");
            final Method getStartTime = runtimeMXBeanClass.getMethod("getStartTime");
            return (Long) getStartTime.invoke(runtimeMXBean);
        } catch (final Throwable t) {
            StatusLogger.getLogger().error("Unable to call ManagementFactory.getRuntimeMXBean().getStartTime(), "
                    + "using system time for OnStartupTriggeringPolicy", t);
            // We have little option but to declare "now" as the beginning of time.
            return System.currentTimeMillis();
        }
    }

    public LogEvent getEvent() {
        return this.event;
    }

    /**
     * Set the location information for this logging event. The collected
     * information is cached for future use.
     */
    @Override
    public LocationInfo getLocationInformation() {
        return new LocationInfo(event.getSource());
    }

    /**
     * Return the level of this event. Use this form instead of directly
     * accessing the <code>level</code> field.
     */
    @Override
    public Level getLevel() {
        switch (StandardLevel.getStandardLevel(event.getLevel().intLevel())) {
            case TRACE:
                return Level.TRACE;
            case DEBUG:
                return Level.DEBUG;
            case INFO:
                return Level.INFO;
            case WARN:
                return Level.WARN;
            case FATAL:
                return Level.FATAL;
            case OFF:
                return Level.OFF;
            case ALL:
                return Level.ALL;
            default:
                return Level.ERROR;
        }
    }

    /**
     * Return the name of the logger. Use this form instead of directly
     * accessing the <code>categoryName</code> field.
     */
    @Override
    public String getLoggerName() {
        return event.getLoggerName();
    }

    @Override
    public long getTimeStamp() {
        return event.getTimeMillis();
    }

    /**
     * Gets the logger of the event.
     */
    @Override
    public Category getLogger() {
        return Category.getInstance(event.getLoggerName());
    }

    /*
     Return the message for this logging event.
    */
    @Override
    public Object getMessage() {
        return event.getMessage();
    }

    /*
     * This method returns the NDC for this event.
     */
    @Override
    public String getNDC() {
        return event.getContextStack().toString();
    }

    /*
     Returns the context corresponding to the <code>key</code> parameter.
     */
    @Override
    public Object getMDC(String key) {
        if (event.getContextData() != null) {
            return event.getContextData().getValue(key);
        }
        return null;
    }

    /**
     * Obtain a copy of this thread's MDC prior to serialization or
     * asynchronous logging.
     */
    @Override
    public void getMDCCopy() {
    }

    @Override
    public String getRenderedMessage() {
        return event.getMessage().getFormattedMessage();
    }

    @Override
    public String getThreadName() {
        return event.getThreadName();
    }

    /**
     * Returns the throwable information contained within this
     * event. May be <code>null</code> if there is no such information.
     *
     * <p>Note that the {@link Throwable} object contained within a
     * {@link ThrowableInformation} does not survive serialization.
     *
     * @since 1.1
     */
    @Override
    public ThrowableInformation getThrowableInformation() {
        if (event.getThrown() != null) {
            return new ThrowableInformation(event.getThrown());
        }
        return null;
    }

    /**
     * Return this event's throwable's string[] representaion.
     */
    @Override
    public String[] getThrowableStrRep() {
        if (event.getThrown() != null) {
            return Throwables.toStringList(event.getThrown()).toArray(Strings.EMPTY_ARRAY);
        }
        return null;
    }

    @Override
    public String getProperty(final String key) {
        return event.getContextData().getValue(key);
    }

    @Override
    public Set getPropertyKeySet() {
        return event.getContextData().toMap().keySet();
    }

    @Override
    public Map getProperties() {
        return event.getContextData().toMap();
    }
}
