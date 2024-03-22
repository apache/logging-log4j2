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
package org.apache.logging.slf4j;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogBuilder;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.LoggerNameAwareMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.util.Constants;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;
import org.slf4j.spi.LocationAwareLogger;

public class SLF4JLogger extends AbstractLogger {

    private static final long serialVersionUID = 1L;
    /**
     * Logback supports turbo filters, that can override the logger's level.
     * Therefore we can never return a no-op builder.
     */
    private static final boolean LAZY_LEVEL_CHECK = "ch.qos.logback.classic.LoggerContext"
            .equals(LoggerFactory.getILoggerFactory().getClass().getName());

    private static final ThreadLocal<SLF4JLogBuilder> logBuilder = ThreadLocal.withInitial(SLF4JLogBuilder::new);

    private final org.slf4j.Logger logger;
    private final LocationAwareLogger locationAwareLogger;

    public SLF4JLogger(final String name, final MessageFactory messageFactory, final org.slf4j.Logger logger) {
        super(name, messageFactory);
        this.logger = logger;
        this.locationAwareLogger = logger instanceof LocationAwareLogger ? (LocationAwareLogger) logger : null;
    }

    public SLF4JLogger(final String name, final org.slf4j.Logger logger) {
        super(name);
        this.logger = logger;
        this.locationAwareLogger = logger instanceof LocationAwareLogger ? (LocationAwareLogger) logger : null;
    }

    private int convertLevel(final Level level) {
        switch (level.getStandardLevel()) {
            case DEBUG:
                return LocationAwareLogger.DEBUG_INT;
            case TRACE:
                return LocationAwareLogger.TRACE_INT;
            case INFO:
                return LocationAwareLogger.INFO_INT;
            case WARN:
                return LocationAwareLogger.WARN_INT;
            case ERROR:
                return LocationAwareLogger.ERROR_INT;
            default:
                return LocationAwareLogger.ERROR_INT;
        }
    }

    @Override
    public Level getLevel() {
        if (logger.isTraceEnabled()) {
            return Level.TRACE;
        }
        if (logger.isDebugEnabled()) {
            return Level.DEBUG;
        }
        if (logger.isInfoEnabled()) {
            return Level.INFO;
        }
        if (logger.isWarnEnabled()) {
            return Level.WARN;
        }
        if (logger.isErrorEnabled()) {
            return Level.ERROR;
        }
        // Option: throw new IllegalStateException("Unknown SLF4JLevel");
        // Option: return Level.ALL;
        return Level.OFF;
    }

    public org.slf4j.Logger getLogger() {
        return locationAwareLogger != null ? locationAwareLogger : logger;
    }

    private static org.slf4j.Marker getMarker(final Marker marker) {
        // No marker is provided in the common case, small methods
        // are optimized more effectively.
        return marker == null ? null : convertMarker(marker);
    }

    private static org.slf4j.Marker convertMarker(final Marker marker) {
        final org.slf4j.Marker slf4jMarker = MarkerFactory.getMarker(marker.getName());
        final Marker[] parents = marker.getParents();
        if (parents != null) {
            for (final Marker parent : parents) {
                final org.slf4j.Marker slf4jParent = getMarker(parent);
                if (!slf4jMarker.contains(slf4jParent)) {
                    slf4jMarker.add(slf4jParent);
                }
            }
        }
        return slf4jMarker;
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

    private boolean isEnabledFor(final Level level, final Marker marker) {
        final org.slf4j.Marker slf4jMarker = getMarker(marker);
        switch (level.getStandardLevel()) {
            case DEBUG:
                return logger.isDebugEnabled(slf4jMarker);
            case TRACE:
                return logger.isTraceEnabled(slf4jMarker);
            case INFO:
                return logger.isInfoEnabled(slf4jMarker);
            case WARN:
                return logger.isWarnEnabled(slf4jMarker);
            case ERROR:
                return logger.isErrorEnabled(slf4jMarker);
            default:
                return logger.isErrorEnabled(slf4jMarker);
        }
    }

    @Override
    public void logMessage(
            final String fqcn, final Level level, final Marker marker, final Message message, final Throwable t) {
        final org.slf4j.Marker slf4jMarker = getMarker(marker);
        final String formattedMessage = message.getFormattedMessage();
        if (locationAwareLogger != null) {
            if (message instanceof LoggerNameAwareMessage) {
                ((LoggerNameAwareMessage) message).setLoggerName(getName());
            }
            locationAwareLogger.log(slf4jMarker, fqcn, convertLevel(level), formattedMessage, null, t);
        } else {
            switch (level.getStandardLevel()) {
                case DEBUG:
                    logger.debug(slf4jMarker, formattedMessage, t);
                    break;
                case TRACE:
                    logger.trace(slf4jMarker, formattedMessage, t);
                    break;
                case INFO:
                    logger.info(slf4jMarker, formattedMessage, t);
                    break;
                case WARN:
                    logger.warn(slf4jMarker, formattedMessage, t);
                    break;
                case ERROR:
                    logger.error(slf4jMarker, formattedMessage, t);
                    break;
                default:
                    logger.error(slf4jMarker, formattedMessage, t);
                    break;
            }
        }
    }

    @Override
    public LogBuilder always() {
        return atLevel(Level.OFF);
    }

    @Override
    public LogBuilder atTrace() {
        return atLevel(Level.TRACE);
    }

    @Override
    public LogBuilder atDebug() {
        return atLevel(Level.DEBUG);
    }

    @Override
    public LogBuilder atInfo() {
        return atLevel(Level.INFO);
    }

    @Override
    public LogBuilder atWarn() {
        return atLevel(Level.WARN);
    }

    @Override
    public LogBuilder atError() {
        return atLevel(Level.ERROR);
    }

    @Override
    public LogBuilder atFatal() {
        return atLevel(Level.TRACE);
    }

    @Override
    protected LogBuilder getLogBuilder(final Level level) {
        final SLF4JLogBuilder builder = logBuilder.get();
        return Constants.ENABLE_THREADLOCALS && !builder.isInUse()
                ? builder.reset(this, level)
                : new SLF4JLogBuilder(this, level);
    }

    @Override
    public LogBuilder atLevel(final Level level) {
        // TODO: wrap SLF4J 2.x LoggingEventBuilder
        if (LAZY_LEVEL_CHECK) {
            return getLogBuilder(level);
        }
        return super.atLevel(level);
    }
}
