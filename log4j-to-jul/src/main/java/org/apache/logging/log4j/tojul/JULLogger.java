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
package org.apache.logging.log4j.tojul;

import static java.util.Objects.requireNonNull;

import java.util.logging.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;

/**
 * Implementation of {@link org.apache.logging.log4j.Logger} that's backed by a {@link Logger}.
 *
 * This implementation currently ignores {@link Marker}.
 *
 * @author <a href="http://www.vorburger.ch">Michael Vorburger.ch</a> for Google
 */
final class JULLogger extends AbstractLogger {
    private static final long serialVersionUID = 1L;

    private final Logger logger;

    // This implementation is inspired by org.apache.logging.slf4j.SLF4JLogger

    public JULLogger(final String name, final MessageFactory messageFactory, final Logger logger) {
        super(name, messageFactory);
        this.logger = requireNonNull(logger, "logger");
    }

    public JULLogger(final String name, final Logger logger) {
        super(name);
        this.logger = requireNonNull(logger, "logger");
    }

    public Logger getWrappedLogger() {
        return logger;
    }

    @Override
    public void logMessage(
            final String fqcn, final Level level, final Marker marker, final Message message, final Throwable t) {
        final java.util.logging.Level julLevel = convertLevel(level);
        if (!logger.isLoggable(julLevel)) {
            return;
        }
        final LazyLog4jLogRecord record =
                new LazyLog4jLogRecord(fqcn, julLevel, message.getFormattedMessage()); // NOT getFormat()
        // NOT record.setParameters(message.getParameters()); BECAUSE getFormattedMessage() NOT getFormat()
        record.setLoggerName(getName());
        record.setThrown(t == null ? message.getThrowable() : t);
        logger.log(record);
    }

    // Convert Level in Log4j scale to JUL scale.
    // See getLevel() for the mapping. Note that JUL's FINEST & CONFIG are never returned because Log4j has no such
    // levels, and
    // that Log4j's FATAL is simply mapped to JUL's SEVERE as is Log4j's ERROR because JUL does not distinguish between
    // ERROR and FATAL.
    private java.util.logging.Level convertLevel(final Level level) {
        switch (level.getStandardLevel()) {
                // Test in logical order of likely frequency of use
                // Must be kept in sync with #getLevel()
            case ALL:
                return java.util.logging.Level.ALL;
            case TRACE:
                return java.util.logging.Level.FINER;
            case DEBUG:
                return java.util.logging.Level.FINE;
            case INFO:
                return java.util.logging.Level.INFO;
            case WARN:
                return java.util.logging.Level.WARNING;
            case ERROR:
                return java.util.logging.Level.SEVERE;
            case FATAL:
                return java.util.logging.Level.SEVERE;
            case OFF:
                return java.util.logging.Level.OFF;
            default:
                // This is tempting: throw new IllegalStateException("Impossible Log4j Level encountered: " +
                // level.intLevel());
                // But it's not a great idea, security wise. If an attacker *SOMEHOW* managed to create a Log4j Level
                // instance
                // with an unexpected level (through JVM de-serialization, despite readResolve() { return
                // Level.valueOf(this.name); },
                // or whatever other means), then we would blow up in a very unexpected place and way. Let us therefore
                // instead just
                // return SEVERE for unexpected values, because that's more likely to be noticed than a FINER.
                // Greetings, Michael Vorburger.ch <http://www.vorburger.ch>, for Google, on 2021.12.24.
                return java.util.logging.Level.SEVERE;
        }
    }

    /**
     * Level in Log4j scale.
     * JUL Levels are mapped as follows:
     * <ul>
     * <li>OFF => OFF
     * <li>SEVERE => ERROR
     * <li>WARNING => WARN
     * <li>INFO => INFO
     * <li>CONFIG => INFO
     * <li>FINE => DEBUG
     * <li>FINER => TRACE (as in https://github.com/apache/logging-log4j2/blob/a58a06bf2365165ac5abdde931bb4ecd1adf0b3c/log4j-jul/src/main/java/org/apache/logging/log4j/jul/DefaultLevelConverter.java#L55-L75)
     * <li>FINEST => TRACE
     * <li>ALL => ALL
     * </ul>
     *
     * Numeric JUL Levels that don't match the known levels are matched to the closest one.
     * For example, anything between OFF (Integer.MAX_VALUE) and SEVERE (1000) is returned as a Log4j FATAL.
     */
    @Override
    public Level getLevel() {
        final int julLevel = getEffectiveJULLevel().intValue();
        // Test in logical order of likely frequency of use
        // Must be kept in sync with #convertLevel()
        if (julLevel == java.util.logging.Level.ALL.intValue()) {
            return Level.ALL;
        }
        if (julLevel <= java.util.logging.Level.FINER.intValue()) {
            return Level.TRACE;
        }
        if (julLevel <= java.util.logging.Level.FINE.intValue()) { // includes FINER
            return Level.DEBUG;
        }
        if (julLevel <= java.util.logging.Level.INFO.intValue()) { // includes CONFIG
            return Level.INFO;
        }
        if (julLevel <= java.util.logging.Level.WARNING.intValue()) {
            return Level.WARN;
        }
        if (julLevel <= java.util.logging.Level.SEVERE.intValue()) {
            return Level.ERROR;
        }
        return Level.OFF;
    }

    private java.util.logging.Level getEffectiveJULLevel() {
        Logger current = logger;
        while (current.getLevel() == null && current.getParent() != null) {
            current = current.getParent();
        }
        if (current.getLevel() != null) {
            return current.getLevel();
        }
        // This is a safety fallback that is typically never reached, because usually the root Logger.getLogger("") has
        // a Level.
        // Since JDK 8 the LogManager$RootLogger does not have a default level, just a default effective level of INFO.
        return java.util.logging.Level.INFO;
    }

    private boolean isEnabledFor(final Level level, final Marker marker) {
        // E.g. we're logging WARN and more, so getLevel() is 300, if we're asked if we're
        // enabled for level ERROR which is 200, isLessSpecificThan() tests for >= so return true.
        return getLevel().isLessSpecificThan(level);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final Message data, final Throwable t) {
        return isEnabledFor(level, marker);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final CharSequence data, final Throwable t) {
        return isEnabledFor(level, marker);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final Object data, final Throwable t) {
        return isEnabledFor(level, marker);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String data) {
        return isEnabledFor(level, marker);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String data, final Object... p1) {
        return isEnabledFor(level, marker);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0) {
        return isEnabledFor(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level, final Marker marker, final String message, final Object p0, final Object p1) {
        return isEnabledFor(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2) {
        return isEnabledFor(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        return isEnabledFor(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {
        return isEnabledFor(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        return isEnabledFor(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        return isEnabledFor(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        return isEnabledFor(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        return isEnabledFor(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
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
        return isEnabledFor(level, marker);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String data, final Throwable t) {
        return isEnabledFor(level, marker);
    }
}
