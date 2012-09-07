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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
public class SLF4JLogger implements LocationAwareLogger {

    private static final String FQCN = SLF4JLogger.class.getName();
    private static final Marker EVENT_MARKER = MarkerFactory.getMarker("EVENT");
    private final boolean eventLogger;
    private final AbstractLoggerWrapper logger;
    private final String name;

    public SLF4JLogger(AbstractLogger logger, String name) {
        Logger l = LogManager.getLogger(name);
        this.logger = new AbstractLoggerWrapper(logger, name);
        eventLogger = "EventLogger".equals(name);
        this.name = name;
    }

    public void trace(String format) {
        if (logger.isTraceEnabled()) {
            logger.log(null, FQCN, Level.TRACE, new SimpleMessage(format), null);
        }
    }

    public void trace(String format, Object o) {
        if (logger.isTraceEnabled()) {
            logger.log(null, FQCN, Level.TRACE, new ParameterizedMessage(format, o), null);
        }
    }

    public void trace(String format, Object arg1, Object arg2) {
        if (logger.isTraceEnabled()) {
            ParameterizedMessage msg = new ParameterizedMessage(format, arg1, arg2);
            logger.log(null, FQCN, Level.TRACE, msg, msg.getThrowable());
        }
    }

    public void trace(String format, Object[] args) {
        if (logger.isTraceEnabled()) {
            ParameterizedMessage msg = new ParameterizedMessage(format, args);
            logger.log(null, FQCN, Level.TRACE, msg, msg.getThrowable());
        }
    }

    public void trace(String format, Throwable t) {
        if (logger.isTraceEnabled()) {
            logger.log(null, FQCN, Level.TRACE, new SimpleMessage(format), t);
        }
    }

    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    public boolean isTraceEnabled(Marker marker) {
        return logger.isTraceEnabled((org.apache.logging.log4j.Marker) marker);
    }

    public void trace(Marker marker, String s) {
        if (isTraceEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.TRACE, new SimpleMessage(s), null);
        }
    }

    public void trace(Marker marker, String s, Object o) {
        if (isTraceEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.TRACE,
                new ParameterizedMessage(s, o), null);
        }
    }

    public void trace(Marker marker, String s, Object o, Object o1) {
        if (isTraceEnabled(marker)) {
            ParameterizedMessage msg = new ParameterizedMessage(s, o, o1);
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.TRACE, msg, msg.getThrowable());
        }
    }

    public void trace(Marker marker, String s, Object[] objects) {
        if (isTraceEnabled(marker)) {
            ParameterizedMessage msg = new ParameterizedMessage(s, objects);
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.TRACE, msg, msg.getThrowable());
        }
    }

    public void trace(Marker marker, String s, Throwable throwable) {
        if (isTraceEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.TRACE,
                new SimpleMessage(s), throwable);
        }
    }

    public void debug(String format) {
        if (logger.isDebugEnabled()) {
            logger.log(null, FQCN, Level.DEBUG, new SimpleMessage(format), null);
        }
    }

    public void debug(String format, Object o) {
        if (logger.isDebugEnabled()) {
            logger.log(null, FQCN, Level.DEBUG, new ParameterizedMessage(format, o), null);
        }
    }

    public void debug(String format, Object arg1, Object arg2) {
        if (logger.isDebugEnabled()) {
            ParameterizedMessage msg = new ParameterizedMessage(format, arg1, arg2);
            logger.log(null, FQCN, Level.DEBUG, msg, msg.getThrowable());
        }
    }

    public void debug(String format, Object[] args) {
        if (logger.isDebugEnabled()) {
            ParameterizedMessage msg = new ParameterizedMessage(format, args);
            logger.log(null, FQCN, Level.DEBUG, msg, msg.getThrowable());
        }
    }

    public void debug(String format, Throwable t) {
        if (logger.isDebugEnabled()) {
            logger.log(null, FQCN, Level.DEBUG, new SimpleMessage(format), t);
        }
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled((org.apache.logging.log4j.Marker) marker);
    }

    public void debug(Marker marker, String s) {
        if (isDebugEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.DEBUG, new SimpleMessage(s), null);
        }
    }

    public void debug(Marker marker, String s, Object o) {
        if (isDebugEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.DEBUG,
                new ParameterizedMessage(s, o), null);
        }
    }

    public void debug(Marker marker, String s, Object o, Object o1) {
        if (isDebugEnabled(marker)) {
            ParameterizedMessage msg = new ParameterizedMessage(s, o, o1);
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.DEBUG, msg, msg.getThrowable());
        }
    }

    public void debug(Marker marker, String s, Object[] objects) {
        if (isDebugEnabled(marker)) {
            ParameterizedMessage msg = new ParameterizedMessage(s, objects);
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.DEBUG, msg, msg.getThrowable());
        }
    }

    public void debug(Marker marker, String s, Throwable throwable) {
        if (isDebugEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.DEBUG,
                new SimpleMessage(s), throwable);
        }
    }

    public void info(String format) {
        if (logger.isInfoEnabled()) {
            logger.log(null, FQCN, Level.INFO, new SimpleMessage(format), null);
        }
    }

    public void info(String format, Object o) {
        if (logger.isInfoEnabled()) {
            logger.log(null, FQCN, Level.INFO, new ParameterizedMessage(format, o), null);
        }
    }

    public void info(String format, Object arg1, Object arg2) {
        if (logger.isInfoEnabled()) {
            ParameterizedMessage msg = new ParameterizedMessage(format, arg1, arg2);
            logger.log(null, FQCN, Level.INFO, msg, msg.getThrowable());
        }
    }

    public void info(String format, Object[] args) {
        if (logger.isInfoEnabled()) {
            ParameterizedMessage msg = new ParameterizedMessage(format, args);
            logger.log(null, FQCN, Level.INFO, msg, msg.getThrowable());
        }
    }

    public void info(String format, Throwable t) {
        if (logger.isInfoEnabled()) {
            logger.log(null, FQCN, Level.INFO, new SimpleMessage(format), t);
        }
    }

    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled((org.apache.logging.log4j.Marker) marker);
    }

    public void info(Marker marker, String s) {
        if (isInfoEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.INFO, new SimpleMessage(s), null);
        }
    }

    public void info(Marker marker, String s, Object o) {
        if (isInfoEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.INFO,
                new ParameterizedMessage(s, o), null);
        }
    }

    public void info(Marker marker, String s, Object o, Object o1) {
        if (isInfoEnabled(marker)) {
            ParameterizedMessage msg = new ParameterizedMessage(s, o, o1);
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.INFO, msg, msg.getThrowable());
        }
    }

    public void info(Marker marker, String s, Object[] objects) {
        if (isInfoEnabled(marker)) {
            ParameterizedMessage msg = new ParameterizedMessage(s, objects);
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.INFO, msg, msg.getThrowable());
        }
    }

    public void info(Marker marker, String s, Throwable throwable) {
        if (isInfoEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.INFO,
                new SimpleMessage(s), throwable);
        }
    }

    public void warn(String format) {
        if (logger.isWarnEnabled()) {
            logger.log(null, FQCN, Level.WARN, new SimpleMessage(format), null);
        }
    }

    public void warn(String format, Object o) {
        if (logger.isWarnEnabled()) {
            logger.log(null, FQCN, Level.WARN, new ParameterizedMessage(format, o), null);
        }
    }

    public void warn(String format, Object arg1, Object arg2) {
        if (logger.isWarnEnabled()) {
            ParameterizedMessage msg = new ParameterizedMessage(format, arg1, arg2);
            logger.log(null, FQCN, Level.WARN, msg, msg.getThrowable());
        }
    }

    public void warn(String format, Object[] args) {
        if (logger.isWarnEnabled()) {
            ParameterizedMessage msg = new ParameterizedMessage(format, args);
            logger.log(null, FQCN, Level.WARN, msg, msg.getThrowable());
        }
    }

    public void warn(String format, Throwable t) {
        if (logger.isWarnEnabled()) {
            logger.log(null, FQCN, Level.WARN, new SimpleMessage(format), t);
        }
    }

    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled((org.apache.logging.log4j.Marker) marker);
    }

    public void warn(Marker marker, String s) {
        if (isWarnEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.WARN, new SimpleMessage(s), null);
        }
    }

    public void warn(Marker marker, String s, Object o) {
        if (isWarnEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.WARN,
                new ParameterizedMessage(s, o), null);
        }
    }

    public void warn(Marker marker, String s, Object o, Object o1) {
        if (isWarnEnabled(marker)) {
            ParameterizedMessage msg = new ParameterizedMessage(s, o, o1);
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.WARN, msg, msg.getThrowable());
        }
    }

    public void warn(Marker marker, String s, Object[] objects) {
        if (isWarnEnabled(marker)) {
            ParameterizedMessage msg = new ParameterizedMessage(s, objects);
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.WARN, msg, msg.getThrowable());
        }
    }

    public void warn(Marker marker, String s, Throwable throwable) {
        if (isWarnEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.WARN,
                new SimpleMessage(s), throwable);
        }
    }

    public void error(String format) {
        if (logger.isErrorEnabled()) {
            logger.log(null, FQCN, Level.ERROR, new SimpleMessage(format), null);
        }
    }

    public void error(String format, Object o) {
        if (logger.isErrorEnabled()) {
            logger.log(null, FQCN, Level.ERROR, new ParameterizedMessage(format, o), null);
        }
    }

    public void error(String format, Object arg1, Object arg2) {
        if (logger.isErrorEnabled()) {
            ParameterizedMessage msg = new ParameterizedMessage(format, arg1, arg2);
            logger.log(null, FQCN, Level.ERROR, msg, msg.getThrowable());
        }
    }

    public void error(String format, Object[] args) {
        if (logger.isErrorEnabled()) {
            ParameterizedMessage msg = new ParameterizedMessage(format, args);
            logger.log(null, FQCN, Level.ERROR, msg, msg.getThrowable());
        }
    }

    public void error(String format, Throwable t) {
        if (logger.isErrorEnabled()) {
            logger.log(null, FQCN, Level.ERROR, new SimpleMessage(format), t);
        }
    }

    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    public boolean isErrorEnabled(Marker marker) {
        return logger.isErrorEnabled((org.apache.logging.log4j.Marker) marker);
    }

    public void error(Marker marker, String s) {
        if (isErrorEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.ERROR, new SimpleMessage(s), null);
        }
    }

    public void error(Marker marker, String s, Object o) {
        if (isErrorEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.ERROR,
                new ParameterizedMessage(s, o), null);
        }
    }

    public void error(Marker marker, String s, Object o, Object o1) {
        if (isErrorEnabled(marker)) {
            ParameterizedMessage msg = new ParameterizedMessage(s, o, o1);
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.ERROR, msg, msg.getThrowable());
        }
    }

    public void error(Marker marker, String s, Object[] objects) {
        if (isErrorEnabled(marker)) {
            ParameterizedMessage msg = new ParameterizedMessage(s, objects);
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.ERROR, msg, msg.getThrowable());
        }
    }

    public void error(Marker marker, String s, Throwable throwable) {
        if (isErrorEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.ERROR,
                new SimpleMessage(s), throwable);
        }
    }


    public void log(Marker marker, String fqcn, int i, String s1, Object[] objects, Throwable throwable) {
        if (!logger.isEnabled(getLevel(i), (org.apache.logging.log4j.Marker) marker, s1)) {
            return;
        }
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

       } else if (objects == null) {
            msg = new SimpleMessage(s1);
        } else {
            msg = new ParameterizedMessage(s1, objects, throwable);
            if (throwable != null) {
                throwable = ((ParameterizedMessage) msg).getThrowable();
            }
        }
        logger.log((org.apache.logging.log4j.Marker) marker, fqcn, getLevel(i), msg, throwable);
    }

    public String getName() {
        return name;
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
