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
package org.apache.logging.log4j.kit.logger.internal;

import java.util.Arrays;
import org.apache.logging.log4j.BridgeAware;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogBuilder;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.recycler.Recycler;
import org.apache.logging.log4j.spi.recycler.RecyclerAware;
import org.apache.logging.log4j.util.LambdaUtil;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.apache.logging.log4j.util.Strings;
import org.apache.logging.log4j.util.Supplier;

/**
 * Collects data for a log event and then logs it. This class should be considered private.
 */
public class DefaultLogBuilder implements BridgeAware, LogBuilder, RecyclerAware<LogBuilder> {

    private static final String FQCN = DefaultLogBuilder.class.getName();
    private static final Message EMPTY_MESSAGE = new SimpleMessage(Strings.EMPTY);

    private ExtendedLogger logger;
    private Level level;
    private Marker marker;
    private Throwable throwable;
    private StackTraceElement location;
    private String fqcn = FQCN;
    private Recycler<LogBuilder> recycler;

    public DefaultLogBuilder(final ExtendedLogger logger, final Level level) {
        this.logger = logger;
        this.level = level;
    }

    public DefaultLogBuilder() {
        this(null, null);
    }

    @Override
    public void setEntryPoint(final String fqcn) {
        this.fqcn = fqcn;
    }

    /**
     * This method should be considered internal. It is used to reset the LogBuilder for a new log message.
     * @param level The logging level for this event.
     * @return This LogBuilder instance.
     */
    public LogBuilder reset(final ExtendedLogger logger, final Level level) {
        this.logger = logger;
        this.level = level;
        this.marker = null;
        this.throwable = null;
        this.location = null;
        return this;
    }

    @Override
    public LogBuilder withMarker(final Marker marker) {
        this.marker = marker;
        return this;
    }

    @Override
    public LogBuilder withThrowable(final Throwable throwable) {
        this.throwable = throwable;
        return this;
    }

    @Override
    public LogBuilder withLocation() {
        location = StackLocatorUtil.getStackTraceElement(2);
        return this;
    }

    @Override
    public LogBuilder withLocation(final StackTraceElement location) {
        this.location = location;
        return this;
    }

    @Override
    public void log(final Message message) {
        if (isEnabled(message)) {
            logMessage(message);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public Message logAndGet(final Supplier<Message> messageSupplier) {
        Message message = null;
        if (isEnabled(message = messageSupplier.get())) {
            logMessage(message);
        }
        return message;
    }

    @Override
    public void log(final CharSequence message) {
        if (isEnabled(message)) {
            logMessage(logger.getMessageFactory().newMessage(message));
        }
    }

    @Override
    public void log(final String message) {
        if (isEnabled(message)) {
            logMessage(logger.getMessageFactory().newMessage(message));
        }
    }

    @Override
    public void log(final String message, final Object... params) {
        if (isEnabled(message, params)) {
            logMessage(logger.getMessageFactory().newMessage(message, params));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void log(String message, Supplier<?>... params) {
        final Object[] objs;
        if (isEnabled(message, objs = LambdaUtil.getAll(params))) {
            logMessage(logger.getMessageFactory().newMessage(message, objs));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void log(final Supplier<Message> messageSupplier) {
        logAndGet(messageSupplier);
    }

    @Override
    public void log(final Object message) {
        if (isEnabled(message)) {
            logMessage(logger.getMessageFactory().newMessage(message));
        }
    }

    @Override
    public void log(final String message, final Object p0) {
        if (isEnabled(message, p0)) {
            logMessage(logger.getMessageFactory().newMessage(message, p0));
        }
    }

    @Override
    public void log(final String message, final Object p0, final Object p1) {
        if (isEnabled(message, p0, p1)) {
            logMessage(logger.getMessageFactory().newMessage(message, p0, p1));
        }
    }

    @Override
    public void log(final String message, final Object p0, final Object p1, final Object p2) {
        if (isEnabled(message, p0, p1, p2)) {
            logMessage(logger.getMessageFactory().newMessage(message, p0, p1, p2));
        }
    }

    @Override
    public void log(final String message, final Object p0, final Object p1, final Object p2, final Object p3) {
        if (isEnabled(message, p0, p1, p2, p3)) {
            logMessage(logger.getMessageFactory().newMessage(message, p0, p1, p2, p3));
        }
    }

    @Override
    public void log(
            final String message, final Object p0, final Object p1, final Object p2, final Object p3, final Object p4) {
        if (isEnabled(message, p0, p1, p2, p3, p4)) {
            logMessage(logger.getMessageFactory().newMessage(message, p0, p1, p2, p3, p4));
        }
    }

    @Override
    public void log(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        if (isEnabled(message, p0, p1, p2, p3, p4, p5)) {
            logMessage(logger.getMessageFactory().newMessage(message, p0, p1, p2, p3, p4, p5));
        }
    }

    @Override
    public void log(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        if (isEnabled(message, p0, p1, p2, p3, p4, p5, p6)) {
            logMessage(logger.getMessageFactory().newMessage(message, p0, p1, p2, p3, p4, p5, p6));
        }
    }

    @Override
    public void log(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        if (isEnabled(message, p0, p1, p2, p3, p4, p5, p6, p7)) {
            logMessage(logger.getMessageFactory().newMessage(message, p0, p1, p2, p3, p4, p5, p6, p7));
        }
    }

    @Override
    public void log(
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
        if (isEnabled(message, p0, p1, p2, p3, p4, p5, p6, p7, p8)) {
            logMessage(logger.getMessageFactory().newMessage(message, p0, p1, p2, p3, p4, p5, p6, p7, p8));
        }
    }

    @Override
    public void log(
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
        if (isEnabled(message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9)) {
            logMessage(logger.getMessageFactory().newMessage(message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9));
        }
    }

    @Override
    public void log() {
        if (isEnabled(EMPTY_MESSAGE)) {
            logMessage(EMPTY_MESSAGE);
        }
    }

    private void logMessage(final Message message) {
        try {
            logger.logMessage(level, marker, fqcn, location, message, throwable);
        } finally {
            this.level = null;
            this.marker = null;
            this.throwable = null;
            this.location = null;
            if (recycler != null) {
                recycler.release(this);
            }
        }
    }

    protected boolean isEnabled(Message message) {
        return logger.isEnabled(level, marker, message, throwable);
    }

    protected boolean isEnabled(CharSequence message) {
        return logger.isEnabled(level, marker, message, throwable);
    }

    protected boolean isEnabled(String message) {
        return logger.isEnabled(level, marker, message, throwable);
    }

    protected boolean isEnabled(String message, Object... params) {
        final Object[] newParams;
        if (throwable != null) {
            newParams = Arrays.copyOf(params, params.length + 1);
            newParams[params.length] = throwable;
        } else {
            newParams = params;
        }
        return logger.isEnabled(level, marker, message, newParams);
    }

    protected boolean isEnabled(Object message) {
        return logger.isEnabled(level, marker, message, throwable);
    }

    protected boolean isEnabled(String message, Object p0) {
        return throwable != null
                ? logger.isEnabled(level, marker, message, p0, throwable)
                : logger.isEnabled(level, marker, message, p0);
    }

    protected boolean isEnabled(String message, Object p0, Object p1) {
        return throwable != null
                ? logger.isEnabled(level, marker, message, p0, p1, throwable)
                : logger.isEnabled(level, marker, message, p0, p1);
    }

    protected boolean isEnabled(String message, Object p0, Object p1, Object p2) {
        return throwable != null
                ? logger.isEnabled(level, marker, message, p0, p1, p2, throwable)
                : logger.isEnabled(level, marker, message, p0, p1, p2);
    }

    protected boolean isEnabled(String message, Object p0, Object p1, Object p2, Object p3) {
        return throwable != null
                ? logger.isEnabled(level, marker, message, p0, p1, p2, p3, throwable)
                : logger.isEnabled(level, marker, message, p0, p1, p2, p3);
    }

    protected boolean isEnabled(String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
        return throwable != null
                ? logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, throwable)
                : logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4);
    }

    protected boolean isEnabled(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
        return throwable != null
                ? logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, throwable)
                : logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5);
    }

    protected boolean isEnabled(
            String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
        return throwable != null
                ? logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, throwable)
                : logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    protected boolean isEnabled(
            String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
        return throwable != null
                ? logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, throwable)
                : logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    protected boolean isEnabled(
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
        return throwable != null
                ? logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, throwable)
                : logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    protected boolean isEnabled(
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
        return throwable != null
                ? logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, throwable)
                : logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    @Override
    public void setRecycler(Recycler<LogBuilder> recycler) {
        this.recycler = recycler;
    }
}
