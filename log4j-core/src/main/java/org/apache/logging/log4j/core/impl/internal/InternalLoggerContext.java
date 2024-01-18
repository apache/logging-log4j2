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
package org.apache.logging.log4j.core.impl.internal;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogBuilder;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.ExtendedLogger;

/**
 * Creates a SimpleLoggerContext compatible with log4j-core. This class is internal to Log4j.
 */
public class InternalLoggerContext extends LoggerContext {

    private final org.apache.logging.log4j.spi.LoggerContext parentLoggerContext;

    private static final LoggerConfig LOGGER_CONFIG = new LoggerConfig.RootLogger();

    public InternalLoggerContext(org.apache.logging.log4j.spi.LoggerContext loggerContext) {
        super();
        this.parentLoggerContext = loggerContext;
        setStarted();
    }

    @Override
    protected Logger newInstance(final LoggerContext ctx, final String name, final MessageFactory messageFactory) {
        return new InternalLogger(this, name);
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        return false;
    }

    private class InternalLogger extends Logger {
        private final ExtendedLogger logger;
        private final InternalLoggerContext loggerContext;

        public InternalLogger(InternalLoggerContext loggerContext, String name) {
            super(loggerContext, name);
            this.loggerContext = loggerContext;
            this.logger = parentLoggerContext.getLogger(name);
        }

        @Override
        public Logger getParent() {
            return null;
        }

        @Override
        public LoggerContext getContext() {
            return loggerContext;
        }

        @Override
        public void setLevel(final Level level) {}

        @Override
        public LoggerConfig get() {
            return LOGGER_CONFIG;
        }

        @Override
        protected boolean requiresLocation() {
            return false;
        }

        @Override
        protected void doLogMessage(
                String fqcn,
                StackTraceElement location,
                Level level,
                Marker marker,
                Message message,
                Throwable throwable) {
            logger.log(level, marker, message, throwable);
        }

        @Override
        public boolean isEnabled(Level level, Marker marker, String message, Throwable t) {
            return logger.isEnabled(level, marker, message, t);
        }

        @Override
        public boolean isEnabled(Level level, Marker marker, String message) {
            return logger.isEnabled(level, marker, message);
        }

        @Override
        public boolean isEnabled(Level level, Marker marker, String message, Object... params) {
            return logger.isEnabled(level, marker, message, params);
        }

        @Override
        public boolean isEnabled(Level level, Marker marker, String message, Object p0) {
            return logger.isEnabled(level, marker, message, p0);
        }

        @Override
        public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1) {
            return logger.isEnabled(level, marker, message, p0, p1);
        }

        @Override
        public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2) {
            return logger.isEnabled(level, marker, message, p0, p1, p2);
        }

        @Override
        public boolean isEnabled(
                Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
            return logger.isEnabled(level, marker, message, p0, p1, p2, p3);
        }

        @Override
        public boolean isEnabled(
                Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
            return logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4);
        }

        @Override
        public boolean isEnabled(
                Level level,
                Marker marker,
                String message,
                Object p0,
                Object p1,
                Object p2,
                Object p3,
                Object p4,
                Object p5) {
            return logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5);
        }

        @Override
        public boolean isEnabled(
                Level level,
                Marker marker,
                String message,
                Object p0,
                Object p1,
                Object p2,
                Object p3,
                Object p4,
                Object p5,
                Object p6) {
            return logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6);
        }

        @Override
        public boolean isEnabled(
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
                Object p7) {
            return logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
        }

        @Override
        public boolean isEnabled(
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
                Object p8) {
            return logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
        }

        @Override
        public boolean isEnabled(
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
                Object p9) {
            return logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
        }

        @Override
        public boolean isEnabled(Level level, Marker marker, CharSequence message, Throwable t) {
            return logger.isEnabled(level, marker, message, t);
        }

        @Override
        public boolean isEnabled(Level level, Marker marker, Object message, Throwable t) {
            return logger.isEnabled(level, marker, message, t);
        }

        @Override
        public boolean isEnabled(Level level, Marker marker, Message message, Throwable t) {
            return logger.isEnabled(level, marker, message, t);
        }

        @Override
        public void addAppender(Appender appender) {}

        @Override
        public void removeAppender(Appender appender) {}

        @Override
        public Map<String, Appender> getAppenders() {
            return Collections.emptyMap();
        }

        @Override
        public Iterator<Filter> getFilters() {
            return Collections.emptyIterator();
        }

        @Override
        public Level getLevel() {
            return logger.getLevel();
        }

        @Override
        public int filterCount() {
            return 0;
        }

        @Override
        public void addFilter(Filter filter) {}

        @Override
        public boolean isAdditive() {
            return false;
        }

        @Override
        public void setAdditive(boolean additive) {}

        @Override
        public LogBuilder atLevel(Level level) {
            return logger.atLevel(level);
        }

        @Override
        protected void updateConfiguration(Configuration newConfig) {}
    }
}
