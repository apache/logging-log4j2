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
package org.apache.logging.slf4j;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.LoggerNameAwareMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.slf4j.MarkerFactory;
import org.slf4j.spi.LocationAwareLogger;

/**
 *
 */
public class SLF4JLogger extends AbstractLogger {

    private final org.slf4j.Logger logger;
    private final LocationAwareLogger locationAwareLogger;

    public SLF4JLogger(final String name, final org.slf4j.Logger logger) {
        super(name);
        this.logger = logger;
        this.locationAwareLogger = logger instanceof LocationAwareLogger ? (LocationAwareLogger) logger : null;
    }

    public SLF4JLogger(final String name, final MessageFactory messageFactory, final org.slf4j.Logger logger) {
        super(name, messageFactory);
        this.logger = logger;
        this.locationAwareLogger = logger instanceof LocationAwareLogger ? (LocationAwareLogger) logger : null;
    }

    @Override
    public void log(final Marker marker, final String fqcn, final Level level, final Message data,
                       final Throwable t) {
        if (locationAwareLogger != null) {
            if (data instanceof LoggerNameAwareMessage) {
                ((LoggerNameAwareMessage) data).setLoggerName(getName());
            }
            locationAwareLogger.log(getMarker(marker), fqcn, convertLevel(level), data.getFormattedMessage(),
                data.getParameters(), t);
        } else {
            switch (level) {
                case DEBUG :
                    logger.debug(getMarker(marker), data.getFormattedMessage(), data.getParameters(), t);
                    break;
                case TRACE :
                    logger.trace(getMarker(marker), data.getFormattedMessage(), data.getParameters(), t);
                    break;
                case INFO :
                    logger.info(getMarker(marker), data.getFormattedMessage(), data.getParameters(), t);
                    break;
                case WARN :
                    logger.warn(getMarker(marker), data.getFormattedMessage(), data.getParameters(), t);
                    break;
                case ERROR :
                    logger.error(getMarker(marker), data.getFormattedMessage(), data.getParameters(), t);
                    break;
                default :
                    logger.error(getMarker(marker), data.getFormattedMessage(), data.getParameters(), t);
                    break;
            }
        }
    }

    private org.slf4j.Marker getMarker(final Marker marker) {
        if (marker == null) {
            return null;
        }
        final Marker parent = marker.getParent();
        final org.slf4j.Marker parentMarker = parent == null ? null : getMarker(parent);
        final org.slf4j.Marker slf4jMarker = MarkerFactory.getMarker(marker.getName());
        if (parentMarker != null && !slf4jMarker.contains(parentMarker)) {
            slf4jMarker.add(parentMarker);
        }
        return slf4jMarker;
    }

    private int convertLevel(final Level level) {
        switch (level) {
            case DEBUG :
                return LocationAwareLogger.DEBUG_INT;
            case TRACE :
                return LocationAwareLogger.TRACE_INT;
            case INFO :
                return LocationAwareLogger.INFO_INT;
            case WARN :
                return LocationAwareLogger.WARN_INT;
            case ERROR :
                return LocationAwareLogger.ERROR_INT;
            default :
                return LocationAwareLogger.ERROR_INT;
        }
    }

    @Override
    protected boolean isEnabled(final Level level, final Marker marker, final String data) {
        return isEnabledFor(level, marker);
    }

    @Override
    protected boolean isEnabled(final Level level, final Marker marker, final String data, final Throwable t) {
        return isEnabledFor(level, marker);
    }

    @Override
    protected boolean isEnabled(final Level level, final Marker marker, final String data, final Object... p1) {
        return isEnabledFor(level, marker);
    }

    @Override
    protected boolean isEnabled(final Level level, final Marker marker, final Object data, final Throwable t) {
        return isEnabledFor(level, marker);
    }

    @Override
    protected boolean isEnabled(final Level level, final Marker marker, final Message data, final Throwable t) {
        return isEnabledFor(level, marker);
    }

    private boolean isEnabledFor(final Level level, final Marker marker) {
        final org.slf4j.Marker slf4jMarker = getMarker(marker);
        switch (level) {
            case DEBUG :
                return logger.isDebugEnabled(slf4jMarker);
            case TRACE :
                return logger.isTraceEnabled(slf4jMarker);
            case INFO :
                return logger.isInfoEnabled(slf4jMarker);
            case WARN :
                return logger.isWarnEnabled(slf4jMarker);
            case ERROR :
                return logger.isErrorEnabled(slf4jMarker);
            default :
                return logger.isErrorEnabled(slf4jMarker);

        }
    }

    public org.slf4j.Logger getLogger() {
        return locationAwareLogger != null ? locationAwareLogger : logger;
    }

}
