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
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.ext.EventData;
import org.slf4j.spi.LocationAwareLogger;

import java.util.Map;

/**
 *
 */
public class Log4JLogger extends Logger implements LocationAwareLogger {

    private static final String FQCN = Log4JLogger.class.getName();
    private static Marker EVENT_MARKER = MarkerFactory.getMarker("EVENT");
    private final boolean eventLogger;

    public Log4JLogger(LoggerContext context, String name) {
        super(context, name);
        eventLogger = "EventLogger".equals(name);
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
        super.log((org.apache.logging.log4j.Marker) marker, fqcn, getLevel(i), msg, throwable);
    }

    public void trace(String format, Object o) {
        super.trace(format, o);
    }

    public void trace(String format, Object arg1, Object arg2) {
        super.trace(format, arg1, arg2);
    }

    public boolean isTraceEnabled(Marker marker) {
        return super.isTraceEnabled((org.apache.logging.log4j.Marker) marker);
    }

    public void trace(Marker marker, String s) {
        super.trace((org.apache.logging.log4j.Marker) marker, s);
    }

    public void trace(Marker marker, String s, Object o) {
        super.trace((org.apache.logging.log4j.Marker) marker, s, o);
    }

    public void trace(Marker marker, String s, Object o, Object o1) {
        super.trace((org.apache.logging.log4j.Marker) marker, s, o, o1);
    }

    public void trace(Marker marker, String s, Object[] objects) {
        super.trace((org.apache.logging.log4j.Marker) marker, s , objects);
    }

    public void trace(Marker marker, String s, Throwable throwable) {
        super.trace((org.apache.logging.log4j.Marker) marker, s, throwable);
    }

    public void debug(String format, Object o) {
        super.debug(format, o);
    }

    public void debug(String format, Object o, Object o1) {
        super.debug(format, o, o1);
    }

    public boolean isDebugEnabled(Marker marker) {
        return super.isDebugEnabled((org.apache.logging.log4j.Marker) marker);
    }

    public void debug(Marker marker, String s) {
        super.debug((org.apache.logging.log4j.Marker) marker, s);
    }

    public void debug(Marker marker, String s, Object o) {
        super.debug((org.apache.logging.log4j.Marker) marker, s, o);
    }

    public void debug(Marker marker, String s, Object o, Object o1) {
        super.debug((org.apache.logging.log4j.Marker) marker, s, o, o1);
    }

    public void debug(Marker marker, String s, Object[] objects) {
        super.debug((org.apache.logging.log4j.Marker) marker, s, objects);
    }

    public void debug(Marker marker, String s, Throwable throwable) {
        super.debug((org.apache.logging.log4j.Marker) marker, s, throwable);
    }

    public void info(String s, Object o) {
        super.info(s, o);
    }

    public void info(String s, Object o, Object o1) {
        super.info(s, o, o1);
    }

    public boolean isInfoEnabled(Marker marker) {
        return super.isInfoEnabled((org.apache.logging.log4j.Marker) marker);
    }

    public void info(Marker marker, String s) {
        super.info((org.apache.logging.log4j.Marker) marker, s);
    }

    public void info(Marker marker, String s, Object o) {
        super.info((org.apache.logging.log4j.Marker) marker, s, o);
    }

    public void info(Marker marker, String s, Object o, Object o1) {
        super.info((org.apache.logging.log4j.Marker) marker, s, o, o1);
    }

    public void info(Marker marker, String s, Object[] objects) {
        super.info((org.apache.logging.log4j.Marker) marker, s, objects);
    }

    public void info(Marker marker, String s, Throwable throwable) {
        super.info((org.apache.logging.log4j.Marker) marker, s, throwable);
    }

    public void warn(String s, Object o) {
        super.warn(s, o);
    }

    public void warn(String s, Object o, Object o1) {
        super.warn(s, o, o1);
    }

    public boolean isWarnEnabled(Marker marker) {
        return super.isWarnEnabled((org.apache.logging.log4j.Marker) marker);
    }

    public void warn(Marker marker, String s) {
        super.warn((org.apache.logging.log4j.Marker) marker, s);
    }

    public void warn(Marker marker, String s, Object o) {
        super.warn((org.apache.logging.log4j.Marker) marker, s, o);
    }

    public void warn(Marker marker, String s, Object o, Object o1) {
        super.warn((org.apache.logging.log4j.Marker) marker, s, o, o1);
    }

    public void warn(Marker marker, String s, Object[] objects) {
        super.warn((org.apache.logging.log4j.Marker) marker, s, objects);
    }

    public void warn(Marker marker, String s, Throwable throwable) {
        super.warn((org.apache.logging.log4j.Marker) marker, s, throwable);
    }

    public void error(String s, Object o) {
        super.error(s, o);
    }

    public void error(String s, Object o, Object o1) {
        super.error(s, o, o1);
    }

    public boolean isErrorEnabled(Marker marker) {
        return super.isErrorEnabled((org.apache.logging.log4j.Marker) marker);
    }

    public void error(Marker marker, String s) {
        super.error((org.apache.logging.log4j.Marker) marker, s);
    }

    public void error(Marker marker, String s, Object o) {
        super.error((org.apache.logging.log4j.Marker) marker, s, o);
    }

    public void error(Marker marker, String s, Object o, Object o1) {
        super.error((org.apache.logging.log4j.Marker) marker, s, o, o1);
    }

    public void error(Marker marker, String s, Object[] objects) {
        super.error((org.apache.logging.log4j.Marker) marker, s, objects);
    }

    public void error(Marker marker, String s, Throwable throwable) {
        super.error((org.apache.logging.log4j.Marker) marker, s, throwable);
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
