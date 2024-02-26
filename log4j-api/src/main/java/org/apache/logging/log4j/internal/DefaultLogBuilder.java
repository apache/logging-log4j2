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
package org.apache.logging.log4j.internal;

import java.util.Arrays;
import org.apache.logging.log4j.BridgeAware;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogBuilder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LambdaUtil;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.apache.logging.log4j.util.Supplier;

/**
 * Collects data for a log event and then logs it. This class should be considered private.
 */
public class DefaultLogBuilder implements BridgeAware, LogBuilder {

    private static Message EMPTY_MESSAGE = new SimpleMessage("");
    private static final String FQCN = DefaultLogBuilder.class.getName();
    private static final Logger LOGGER = StatusLogger.getLogger();

    private ExtendedLogger logger;
    private Level level;
    private Marker marker;
    private Throwable throwable;
    private StackTraceElement location;
    private volatile boolean inUse;
    private long threadId;
    private String fqcn = FQCN;

    public DefaultLogBuilder(final ExtendedLogger logger, final Level level) {
        this.logger = logger;
        this.level = level;
        this.threadId = Thread.currentThread().getId();
        this.inUse = level != null;
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
        this.inUse = true;
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
    public LogBuilder withLocation(StackTraceElement location) {
        this.location = location;
        return this;
    }

    public boolean isInUse() {
        return inUse;
    }

    @Override
    public void log(Message message) {
        if (isValid() && isEnabled(message)) {
            logMessage(message);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public Message logAndGet(final Supplier<Message> messageSupplier) {
        Message message = null;
        if (isValid() && isEnabled(message = messageSupplier.get())) {
            logMessage(message);
        }
        return message;
    }

    @Override
    public void log(final CharSequence message) {
        if (isValid() && isEnabled(message)) {
            logMessage(logger.getMessageFactory().newMessage(message));
        }
    }

    @Override
    public void log(String message) {
        if (isValid() && isEnabled(message)) {
            logMessage(logger.getMessageFactory().newMessage(message));
        }
    }

    @Override
    public void log(String message, Object... params) {
        if (isValid() && isEnabled(message, params)) {
            logMessage(logger.getMessageFactory().newMessage(message, params));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void log(String message, Supplier<?>... params) {
        final Object[] objs;
        if (isValid() && isEnabled(message, objs = LambdaUtil.getAll(params))) {
            logMessage(logger.getMessageFactory().newMessage(message, objs));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void log(final Supplier<Message> messageSupplier) {
        logAndGet(messageSupplier);
    }

    @Override
    public void log(Object message) {
        if (isValid() && isEnabled(message)) {
            logMessage(logger.getMessageFactory().newMessage(message));
        }
    }

    @Override
    public void log(String message, Object p0) {
        if (isValid() && isEnabled(message, p0)) {
            logMessage(logger.getMessageFactory().newMessage(message, p0));
        }
    }

    @Override
    public void log(String message, Object p0, Object p1) {
        if (isValid() && isEnabled(message, p0, p1)) {
            logMessage(logger.getMessageFactory().newMessage(message, p0, p1));
        }
    }

    @Override
    public void log(String message, Object p0, Object p1, Object p2) {
        if (isValid() && isEnabled(message, p0, p1, p2)) {
            logMessage(logger.getMessageFactory().newMessage(message, p0, p1, p2));
        }
    }

    @Override
    public void log(String message, Object p0, Object p1, Object p2, Object p3) {
        if (isValid() && isEnabled(message, p0, p1, p2, p3)) {
            logMessage(logger.getMessageFactory().newMessage(message, p0, p1, p2, p3));
        }
    }

    @Override
    public void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
        if (isValid() && isEnabled(message, p0, p1, p2, p3, p4)) {
            logMessage(logger.getMessageFactory().newMessage(message, p0, p1, p2, p3, p4));
        }
    }

    @Override
    public void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
        if (isValid() && isEnabled(message, p0, p1, p2, p3, p4, p5)) {
            logMessage(logger.getMessageFactory().newMessage(message, p0, p1, p2, p3, p4, p5));
        }
    }

    @Override
    public void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
        if (isValid() && isEnabled(message, p0, p1, p2, p3, p4, p5, p6)) {
            logMessage(logger.getMessageFactory().newMessage(message, p0, p1, p2, p3, p4, p5, p6));
        }
    }

    @Override
    public void log(
            String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
        if (isValid() && isEnabled(message, p0, p1, p2, p3, p4, p5, p6, p7)) {
            logMessage(logger.getMessageFactory().newMessage(message, p0, p1, p2, p3, p4, p5, p6, p7));
        }
    }

    @Override
    public void log(
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
        if (isValid() && isEnabled(message, p0, p1, p2, p3, p4, p5, p6, p7, p8)) {
            logMessage(logger.getMessageFactory().newMessage(message, p0, p1, p2, p3, p4, p5, p6, p7, p8));
        }
    }

    @Override
    public void log(
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
        if (isValid() && isEnabled(message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9)) {
            logMessage(logger.getMessageFactory().newMessage(message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9));
        }
    }

    @Override
    public void log() {
        if (isValid() && isEnabled(EMPTY_MESSAGE)) {
            logMessage(EMPTY_MESSAGE);
        }
    }

    private void logMessage(Message message) {
        try {
            logger.logMessage(level, marker, fqcn, location, message, throwable);
        } finally {
            inUse = false;
        }
    }

    private boolean isValid() {
        if (!inUse) {
            LOGGER.warn("Attempt to reuse LogBuilder was ignored. {}", StackLocatorUtil.getCallerClass(2));
            return false;
        }
        if (this.threadId != Thread.currentThread().getId()) {
            LOGGER.warn("LogBuilder can only be used on the owning thread. {}", StackLocatorUtil.getCallerClass(2));
            return false;
        }
        return true;
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
}
