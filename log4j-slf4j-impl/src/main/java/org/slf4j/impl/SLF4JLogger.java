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
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.AbstractLoggerWrapper;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.helpers.EventDataConverter;
import org.slf4j.spi.LocationAwareLogger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 *
 */
public class SLF4JLogger implements LocationAwareLogger, Serializable {

    private static final long serialVersionUID = 7869000638091304316L;
    private static final String FQCN = SLF4JLogger.class.getName();
    private static final Marker EVENT_MARKER = MarkerFactory.getMarker("EVENT");
    private final boolean eventLogger;
    private transient AbstractLoggerWrapper logger;
    private final String name;
    private transient EventDataConverter converter;

    public SLF4JLogger(final AbstractLogger logger, final String name) {
        this.logger = new AbstractLoggerWrapper(logger, name, null);
        this.eventLogger = "EventLogger".equals(name);
        this.name = name;
        this.converter = createConverter();
    }

    @Override
    public void trace(final String format) {
        if (logger.isTraceEnabled()) {
            logger.log(null, FQCN, Level.TRACE, new SimpleMessage(format), null);
        }
    }

    @Override
    public void trace(final String format, final Object o) {
        if (logger.isTraceEnabled()) {
            logger.log(null, FQCN, Level.TRACE, new ParameterizedMessage(format, o), null);
        }
    }

    @Override
    public void trace(final String format, final Object arg1, final Object arg2) {
        if (logger.isTraceEnabled()) {
            final ParameterizedMessage msg = new ParameterizedMessage(format, arg1, arg2);
            logger.log(null, FQCN, Level.TRACE, msg, msg.getThrowable());
        }
    }

    @Override
    public void trace(final String format, final Object... args) {
        if (logger.isTraceEnabled()) {
            final ParameterizedMessage msg = new ParameterizedMessage(format, args);
            logger.log(null, FQCN, Level.TRACE, msg, msg.getThrowable());
        }
    }

    @Override
    public void trace(final String format, final Throwable t) {
        if (logger.isTraceEnabled()) {
            logger.log(null, FQCN, Level.TRACE, new SimpleMessage(format), t);
        }
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public boolean isTraceEnabled(final Marker marker) {
        return logger.isTraceEnabled((org.apache.logging.log4j.Marker) marker);
    }

    @Override
    public void trace(final Marker marker, final String s) {
        if (isTraceEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.TRACE, new SimpleMessage(s), null);
        }
    }

    @Override
    public void trace(final Marker marker, final String s, final Object o) {
        if (isTraceEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.TRACE,
                new ParameterizedMessage(s, o), null);
        }
    }

    @Override
    public void trace(final Marker marker, final String s, final Object o, final Object o1) {
        if (isTraceEnabled(marker)) {
            final ParameterizedMessage msg = new ParameterizedMessage(s, o, o1);
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.TRACE, msg, msg.getThrowable());
        }
    }

    @Override
    public void trace(final Marker marker, final String s, final Object... objects) {
        if (isTraceEnabled(marker)) {
            final ParameterizedMessage msg = new ParameterizedMessage(s, objects);
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.TRACE, msg, msg.getThrowable());
        }
    }

    @Override
    public void trace(final Marker marker, final String s, final Throwable throwable) {
        if (isTraceEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.TRACE,
                new SimpleMessage(s), throwable);
        }
    }

    @Override
    public void debug(final String format) {
        if (logger.isDebugEnabled()) {
            logger.log(null, FQCN, Level.DEBUG, new SimpleMessage(format), null);
        }
    }

    @Override
    public void debug(final String format, final Object o) {
        if (logger.isDebugEnabled()) {
            logger.log(null, FQCN, Level.DEBUG, new ParameterizedMessage(format, o), null);
        }
    }

    @Override
    public void debug(final String format, final Object arg1, final Object arg2) {
        if (logger.isDebugEnabled()) {
            final ParameterizedMessage msg = new ParameterizedMessage(format, arg1, arg2);
            logger.log(null, FQCN, Level.DEBUG, msg, msg.getThrowable());
        }
    }

    @Override
    public void debug(final String format, final Object... args) {
        if (logger.isDebugEnabled()) {
            final ParameterizedMessage msg = new ParameterizedMessage(format, args);
            logger.log(null, FQCN, Level.DEBUG, msg, msg.getThrowable());
        }
    }

    @Override
    public void debug(final String format, final Throwable t) {
        if (logger.isDebugEnabled()) {
            logger.log(null, FQCN, Level.DEBUG, new SimpleMessage(format), t);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public boolean isDebugEnabled(final Marker marker) {
        return logger.isDebugEnabled((org.apache.logging.log4j.Marker) marker);
    }

    @Override
    public void debug(final Marker marker, final String s) {
        if (isDebugEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.DEBUG, new SimpleMessage(s), null);
        }
    }

    @Override
    public void debug(final Marker marker, final String s, final Object o) {
        if (isDebugEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.DEBUG,
                new ParameterizedMessage(s, o), null);
        }
    }

    @Override
    public void debug(final Marker marker, final String s, final Object o, final Object o1) {
        if (isDebugEnabled(marker)) {
            final ParameterizedMessage msg = new ParameterizedMessage(s, o, o1);
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.DEBUG, msg, msg.getThrowable());
        }
    }

    @Override
    public void debug(final Marker marker, final String s, final Object... objects) {
        if (isDebugEnabled(marker)) {
            final ParameterizedMessage msg = new ParameterizedMessage(s, objects);
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.DEBUG, msg, msg.getThrowable());
        }
    }

    @Override
    public void debug(final Marker marker, final String s, final Throwable throwable) {
        if (isDebugEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.DEBUG,
                new SimpleMessage(s), throwable);
        }
    }

    @Override
    public void info(final String format) {
        if (logger.isInfoEnabled()) {
            logger.log(null, FQCN, Level.INFO, new SimpleMessage(format), null);
        }
    }

    @Override
    public void info(final String format, final Object o) {
        if (logger.isInfoEnabled()) {
            logger.log(null, FQCN, Level.INFO, new ParameterizedMessage(format, o), null);
        }
    }

    @Override
    public void info(final String format, final Object arg1, final Object arg2) {
        if (logger.isInfoEnabled()) {
            final ParameterizedMessage msg = new ParameterizedMessage(format, arg1, arg2);
            logger.log(null, FQCN, Level.INFO, msg, msg.getThrowable());
        }
    }

    @Override
    public void info(final String format, final Object... args) {
        if (logger.isInfoEnabled()) {
            final ParameterizedMessage msg = new ParameterizedMessage(format, args);
            logger.log(null, FQCN, Level.INFO, msg, msg.getThrowable());
        }
    }

    @Override
    public void info(final String format, final Throwable t) {
        if (logger.isInfoEnabled()) {
            logger.log(null, FQCN, Level.INFO, new SimpleMessage(format), t);
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public boolean isInfoEnabled(final Marker marker) {
        return logger.isInfoEnabled((org.apache.logging.log4j.Marker) marker);
    }

    @Override
    public void info(final Marker marker, final String s) {
        if (isInfoEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.INFO, new SimpleMessage(s), null);
        }
    }

    @Override
    public void info(final Marker marker, final String s, final Object o) {
        if (isInfoEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.INFO,
                new ParameterizedMessage(s, o), null);
        }
    }

    @Override
    public void info(final Marker marker, final String s, final Object o, final Object o1) {
        if (isInfoEnabled(marker)) {
            final ParameterizedMessage msg = new ParameterizedMessage(s, o, o1);
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.INFO, msg, msg.getThrowable());
        }
    }

    @Override
    public void info(final Marker marker, final String s, final Object... objects) {
        if (isInfoEnabled(marker)) {
            final ParameterizedMessage msg = new ParameterizedMessage(s, objects);
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.INFO, msg, msg.getThrowable());
        }
    }

    @Override
    public void info(final Marker marker, final String s, final Throwable throwable) {
        if (isInfoEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.INFO,
                new SimpleMessage(s), throwable);
        }
    }

    @Override
    public void warn(final String format) {
        if (logger.isWarnEnabled()) {
            logger.log(null, FQCN, Level.WARN, new SimpleMessage(format), null);
        }
    }

    @Override
    public void warn(final String format, final Object o) {
        if (logger.isWarnEnabled()) {
            logger.log(null, FQCN, Level.WARN, new ParameterizedMessage(format, o), null);
        }
    }

    @Override
    public void warn(final String format, final Object arg1, final Object arg2) {
        if (logger.isWarnEnabled()) {
            final ParameterizedMessage msg = new ParameterizedMessage(format, arg1, arg2);
            logger.log(null, FQCN, Level.WARN, msg, msg.getThrowable());
        }
    }

    @Override
    public void warn(final String format, final Object... args) {
        if (logger.isWarnEnabled()) {
            final ParameterizedMessage msg = new ParameterizedMessage(format, args);
            logger.log(null, FQCN, Level.WARN, msg, msg.getThrowable());
        }
    }

    @Override
    public void warn(final String format, final Throwable t) {
        if (logger.isWarnEnabled()) {
            logger.log(null, FQCN, Level.WARN, new SimpleMessage(format), t);
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public boolean isWarnEnabled(final Marker marker) {
        return logger.isWarnEnabled((org.apache.logging.log4j.Marker) marker);
    }

    @Override
    public void warn(final Marker marker, final String s) {
        if (isWarnEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.WARN, new SimpleMessage(s), null);
        }
    }

    @Override
    public void warn(final Marker marker, final String s, final Object o) {
        if (isWarnEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.WARN,
                new ParameterizedMessage(s, o), null);
        }
    }

    @Override
    public void warn(final Marker marker, final String s, final Object o, final Object o1) {
        if (isWarnEnabled(marker)) {
            final ParameterizedMessage msg = new ParameterizedMessage(s, o, o1);
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.WARN, msg, msg.getThrowable());
        }
    }

    @Override
    public void warn(final Marker marker, final String s, final Object... objects) {
        if (isWarnEnabled(marker)) {
            final ParameterizedMessage msg = new ParameterizedMessage(s, objects);
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.WARN, msg, msg.getThrowable());
        }
    }

    @Override
    public void warn(final Marker marker, final String s, final Throwable throwable) {
        if (isWarnEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.WARN,
                new SimpleMessage(s), throwable);
        }
    }

    @Override
    public void error(final String format) {
        if (logger.isErrorEnabled()) {
            logger.log(null, FQCN, Level.ERROR, new SimpleMessage(format), null);
        }
    }

    @Override
    public void error(final String format, final Object o) {
        if (logger.isErrorEnabled()) {
            logger.log(null, FQCN, Level.ERROR, new ParameterizedMessage(format, o), null);
        }
    }

    @Override
    public void error(final String format, final Object arg1, final Object arg2) {
        if (logger.isErrorEnabled()) {
            final ParameterizedMessage msg = new ParameterizedMessage(format, arg1, arg2);
            logger.log(null, FQCN, Level.ERROR, msg, msg.getThrowable());
        }
    }

    @Override
    public void error(final String format, final Object... args) {
        if (logger.isErrorEnabled()) {
            final ParameterizedMessage msg = new ParameterizedMessage(format, args);
            logger.log(null, FQCN, Level.ERROR, msg, msg.getThrowable());
        }
    }

    @Override
    public void error(final String format, final Throwable t) {
        if (logger.isErrorEnabled()) {
            logger.log(null, FQCN, Level.ERROR, new SimpleMessage(format), t);
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public boolean isErrorEnabled(final Marker marker) {
        return logger.isErrorEnabled((org.apache.logging.log4j.Marker) marker);
    }

    @Override
    public void error(final Marker marker, final String s) {
        if (isErrorEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.ERROR, new SimpleMessage(s), null);
        }
    }

    @Override
    public void error(final Marker marker, final String s, final Object o) {
        if (isErrorEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.ERROR,
                new ParameterizedMessage(s, o), null);
        }
    }

    @Override
    public void error(final Marker marker, final String s, final Object o, final Object o1) {
        if (isErrorEnabled(marker)) {
            final ParameterizedMessage msg = new ParameterizedMessage(s, o, o1);
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.ERROR, msg, msg.getThrowable());
        }
    }

    @Override
    public void error(final Marker marker, final String s, final Object... objects) {
        if (isErrorEnabled(marker)) {
            final ParameterizedMessage msg = new ParameterizedMessage(s, objects);
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.ERROR, msg, msg.getThrowable());
        }
    }

    @Override
    public void error(final Marker marker, final String s, final Throwable throwable) {
        if (isErrorEnabled(marker)) {
            logger.log((org.apache.logging.log4j.Marker) marker, FQCN, Level.ERROR,
                new SimpleMessage(s), throwable);
        }
    }


    @Override
    public void log(final Marker marker, final String fqcn, final int i, final String s1, final Object[] objects,
                    Throwable throwable) {
        if (!logger.isEnabled(getLevel(i), (org.apache.logging.log4j.Marker) marker, s1)) {
            return;
        }
        Message msg;
        if (eventLogger && marker != null && marker.contains(EVENT_MARKER) && converter != null) {
            msg = converter.convertEvent(s1, objects, throwable);
       } else if (objects == null) {
            msg = new SimpleMessage(s1);
        } else {
            msg = new ParameterizedMessage(s1, objects, throwable);
            if (throwable != null) {
                throwable = msg.getThrowable();
            }
        }
        logger.log((org.apache.logging.log4j.Marker) marker, fqcn, getLevel(i), msg, throwable);
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Always treat de-serialization as a full-blown constructor, by
     * validating the final state of the de-serialized object.
     */
    private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
        //always perform the default de-serialization first
        aInputStream.defaultReadObject();
        logger = new AbstractLoggerWrapper((AbstractLogger) LogManager.getLogger(name), name, null);
        converter = createConverter();
    }

    /**
     * This is the default implementation of writeObject.
     * Customise if necessary.
     */
    private void writeObject(ObjectOutputStream aOutputStream
    ) throws IOException {
        //perform the default serialization for all non-transient, non-static fields
        aOutputStream.defaultWriteObject();
    }

    private EventDataConverter createConverter() {
        try {
            Class.forName("org.slf4j.ext.EventData");
            return new EventDataConverter();
        } catch (ClassNotFoundException cnfe) {
            return null;
        }
    }

    private Level getLevel(final int i) {
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
