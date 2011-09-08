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
package org.slf4j.impl;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.AbstractLoggerWrapper;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.ext.EventData;
import org.slf4j.spi.LocationAwareLogger;

import java.util.Map;

/**
 *
 */
public class SLF4JLogger extends AbstractLoggerWrapper implements LocationAwareLogger {

    private static final String FQCN = SLF4JLogger.class.getName();
    private static final Marker EVENT_MARKER = MarkerFactory.getMarker("EVENT");
    private final boolean eventLogger;

    public SLF4JLogger(AbstractLogger logger, String name) {
        super(logger, name);
        eventLogger = "EventLogger".equals(name);
    }

    @Override
    public void trace(String format) {
        if (isTraceEnabled()) {
            log(null, FQCN, Level.TRACE, new SimpleMessage(format), null);
        }
    }

    public void trace(String format, Object o) {
        if (isTraceEnabled()) {
            log(null, FQCN, Level.TRACE, new ParameterizedMessage(format, o), null);
        }
    }

    public void trace(String format, Object arg1, Object arg2) {
        if (isTraceEnabled()) {
            log(null, FQCN, Level.TRACE, new ParameterizedMessage(format, arg1, arg2), null);
        }
    }

    public boolean isTraceEnabled(Marker marker) {
        return logger.isTraceEnabled((org.apache.logging.log4j.Marker) marker);
    }

    public void trace(Marker marker, String s) {
        if (isTraceEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.TRACE, new SimpleMessage(s), null);
        }
    }

    public void trace(Marker marker, String s, Object o) {
        if (isTraceEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.TRACE, new ParameterizedMessage(s, o), null);
        }
    }

    public void trace(Marker marker, String s, Object o, Object o1) {
        if (isTraceEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.TRACE, new ParameterizedMessage(s, o, o1), null);
        }
    }

    public void trace(Marker marker, String s, Object[] objects) {
        if (isTraceEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.TRACE,
                new ParameterizedMessage(s, objects), null);
        }
    }

    public void trace(Marker marker, String s, Throwable throwable) {
        if (isTraceEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.TRACE,
                new ParameterizedMessage(s, null, throwable), throwable);
        }
    }

    @Override
    public void debug(String format) {
        if (isDebugEnabled()) {
            log(null, FQCN, Level.DEBUG, new SimpleMessage(format), null);
        }
    }

    public void debug(String format, Object o) {
        if (isDebugEnabled()) {
            log(null, FQCN, Level.DEBUG, new ParameterizedMessage(format, o), null);
        }
    }

    public void debug(String format, Object arg1, Object arg2) {
        if (isDebugEnabled()) {
            log(null, FQCN, Level.DEBUG, new ParameterizedMessage(format, arg1, arg2), null);
        }
    }

    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled((org.apache.logging.log4j.Marker) marker);
    }

    public void debug(Marker marker, String s) {
        if (isDebugEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.DEBUG, new SimpleMessage(s), null);
        }
    }

    public void debug(Marker marker, String s, Object o) {
        if (isDebugEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.DEBUG, new ParameterizedMessage(s, o), null);
        }
    }

    public void debug(Marker marker, String s, Object o, Object o1) {
        if (isDebugEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.DEBUG, new ParameterizedMessage(s, o, o1), null);
        }
    }

    public void debug(Marker marker, String s, Object[] objects) {
        if (isDebugEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.DEBUG,
                new ParameterizedMessage(s, objects), null);
        }
    }

    public void debug(Marker marker, String s, Throwable throwable) {
        if (isDebugEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.DEBUG,
                new ParameterizedMessage(s, null, throwable), throwable);
        }
    }

    @Override
    public void info(String format) {
        if (isInfoEnabled()) {
            log(null, FQCN, Level.INFO, new SimpleMessage(format), null);
        }
    }

    public void info(String format, Object o) {
        if (isInfoEnabled()) {
            log(null, FQCN, Level.INFO, new ParameterizedMessage(format, o), null);
        }
    }

    public void info(String format, Object arg1, Object arg2) {
        if (isInfoEnabled()) {
            log(null, FQCN, Level.INFO, new ParameterizedMessage(format, arg1, arg2), null);
        }
    }

    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled((org.apache.logging.log4j.Marker) marker);
    }

    public void info(Marker marker, String s) {
        if (isInfoEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.INFO, new SimpleMessage(s), null);
        }
    }

    public void info(Marker marker, String s, Object o) {
        if (isInfoEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.INFO, new ParameterizedMessage(s, o), null);
        }
    }

    public void info(Marker marker, String s, Object o, Object o1) {
        if (isInfoEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.INFO, new ParameterizedMessage(s, o, o1), null);
        }
    }

    public void info(Marker marker, String s, Object[] objects) {
        if (isInfoEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.INFO,
                new ParameterizedMessage(s, objects), null);
        }
    }

    public void info(Marker marker, String s, Throwable throwable) {
        if (isInfoEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.INFO,
                new ParameterizedMessage(s, null, throwable), throwable);
        }
    }

    @Override
    public void warn(String format) {
        if (isWarnEnabled()) {
            log(null, FQCN, Level.WARN, new SimpleMessage(format), null);
        }
    }

    public void warn(String format, Object o) {
        if (isWarnEnabled()) {
            log(null, FQCN, Level.WARN, new ParameterizedMessage(format, o), null);
        }
    }

    public void warn(String format, Object arg1, Object arg2) {
        if (isWarnEnabled()) {
            log(null, FQCN, Level.WARN, new ParameterizedMessage(format, arg1, arg2), null);
        }
    }

    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled((org.apache.logging.log4j.Marker) marker);
    }

    public void warn(Marker marker, String s) {
        if (isWarnEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.WARN, new SimpleMessage(s), null);
        }
    }

    public void warn(Marker marker, String s, Object o) {
        if (isDebugEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.WARN, new ParameterizedMessage(s, o), null);
        }
    }

    public void warn(Marker marker, String s, Object o, Object o1) {
        if (isDebugEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.WARN, new ParameterizedMessage(s, o, o1), null);
        }
    }

    public void warn(Marker marker, String s, Object[] objects) {
        if (isDebugEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.WARN,
                new ParameterizedMessage(s, objects), null);
        }
    }

    public void warn(Marker marker, String s, Throwable throwable) {
        if (isDebugEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.WARN,
                new ParameterizedMessage(s, null, throwable), throwable);
        }
    }

    @Override
    public void error(String format) {
        if (isErrorEnabled()) {
            log(null, FQCN, Level.ERROR, new SimpleMessage(format), null);
        }
    }

    public void error(String format, Object o) {
        if (isErrorEnabled()) {
            log(null, FQCN, Level.ERROR, new ParameterizedMessage(format, o), null);
        }
    }

    public void error(String format, Object arg1, Object arg2) {
        if (isErrorEnabled()) {
            log(null, FQCN, Level.ERROR, new ParameterizedMessage(format, arg1, arg2), null);
        }
    }

    public boolean isErrorEnabled(Marker marker) {
        return logger.isErrorEnabled((org.apache.logging.log4j.Marker) marker);
    }

    public void error(Marker marker, String s) {
        if (isErrorEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.ERROR, new SimpleMessage(s), null);
        }
    }

    public void error(Marker marker, String s, Object o) {
        if (isErrorEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.ERROR, new ParameterizedMessage(s, o), null);
        }
    }

    public void error(Marker marker, String s, Object o, Object o1) {
        if (isErrorEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.ERROR, new ParameterizedMessage(s, o, o1), null);
        }
    }

    public void error(Marker marker, String s, Object[] objects) {
        if (isErrorEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.ERROR,
                new ParameterizedMessage(s, objects), null);
        }
    }

    public void error(Marker marker, String s, Throwable throwable) {
        if (isErrorEnabled(marker)) {
            log((org.apache.logging.log4j.Marker) marker, FQCN, Level.ERROR,
                new ParameterizedMessage(s, null, throwable), throwable);
        }
    }

    public void log(Marker marker, String fqcn, int i, String s1, Object[] objects, Throwable throwable) {
        Message msg;
        if (eventLogger && marker != null && marker.contains(EVENT_MARKER)) {
            try {
                EventData data = (objects != null && objects[0] instanceof EventData) ? (EventData) objects[0] :
                    new EventData(s1);
                msg = new StructuredDataMessage(data.getEventId(), data.getMessage(), data.getEventType());
                for (Map.Entry entry : data.getEventMap().entrySet()) {
                    String key = entry.getKey().toString();
                    if (EventData.EVENT_TYPE.equals(key) || EventData.EVENT_ID.equals(key) ||
                        EventData.EVENT_MESSAGE.equals(key)) {
                        continue;
                    }
                    ((StructuredDataMessage) msg).put(entry.getKey().toString(), entry.getValue().toString());
                }
            } catch (Exception ex) {
                msg = new ParameterizedMessage(s1, objects, throwable);
            }

        } else {
            msg = new ParameterizedMessage(s1, objects, throwable);
        }
        log((org.apache.logging.log4j.Marker) marker, fqcn, getLevel(i), msg, throwable);
    }

    public String getName() {
        return name;
    }

    @Override
    protected String getFQCN() {
        return FQCN;
    }

    private Level getLevel(int i) {

        switch (i) {
            case TRACE_INT :
                return Level.TRACE;
            case DEBUG_INT :
                return Level.DEBUG;
            case INFO_INT :
                return Level.INFO;
            case WARN_INT :
                return Level.WARN;
            case ERROR_INT :
                return Level.ERROR;
        }
        return Level.ERROR;
    }
}
