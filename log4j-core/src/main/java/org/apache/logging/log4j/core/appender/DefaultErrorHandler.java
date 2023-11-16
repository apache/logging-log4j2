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
package org.apache.logging.log4j.core.appender;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * The default {@link ErrorHandler} implementation falling back to {@link StatusLogger}.
 * <p>
 * It avoids flooding the {@link StatusLogger} by allowing either the first 3 errors or errors once every 5 minutes.
 * </p>
 */
public class DefaultErrorHandler implements ErrorHandler {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final int MAX_EXCEPTION_COUNT = 3;

    private static final long EXCEPTION_INTERVAL_NANOS = TimeUnit.MINUTES.toNanos(5);

    private int exceptionCount = 0;

    private long lastExceptionInstantNanos = System.nanoTime() - EXCEPTION_INTERVAL_NANOS - 1;

    private final Appender appender;

    public DefaultErrorHandler(final Appender appender) {
        this.appender = requireNonNull(appender, "appender");
    }

    /**
     * Handle an error with a message.
     * @param msg a message
     */
    @Override
    public void error(final String msg) {
        final boolean allowed = acquirePermit();
        if (allowed) {
            LOGGER.error(msg);
        }
    }

    /**
     * Handle an error with a message and an exception.
     *
     * @param msg a message
     * @param error a {@link Throwable}
     */
    @Override
    public void error(final String msg, final Throwable error) {
        final boolean allowed = acquirePermit();
        if (allowed) {
            LOGGER.error(msg, error);
        }
        if (!appender.ignoreExceptions() && error != null && !(error instanceof AppenderLoggingException)) {
            throw new AppenderLoggingException(msg, error);
        }
    }

    /**
     * Handle an error with a message, an exception, and a logging event.
     *
     * @param msg a message
     * @param event a {@link LogEvent}
     * @param error a {@link Throwable}
     */
    @Override
    public void error(final String msg, final LogEvent event, final Throwable error) {
        final boolean allowed = acquirePermit();
        if (allowed) {
            LOGGER.error(msg, error);
        }
        if (!appender.ignoreExceptions() && error != null && !(error instanceof AppenderLoggingException)) {
            throw new AppenderLoggingException(msg, error);
        }
    }

    private boolean acquirePermit() {
        final long currentInstantNanos = System.nanoTime();
        synchronized (this) {
            if (currentInstantNanos - lastExceptionInstantNanos > EXCEPTION_INTERVAL_NANOS) {
                lastExceptionInstantNanos = currentInstantNanos;
                return true;
            } else if (exceptionCount < MAX_EXCEPTION_COUNT) {
                exceptionCount++;
                lastExceptionInstantNanos = currentInstantNanos;
                return true;
            } else {
                return false;
            }
        }
    }

    public Appender getAppender() {
        return appender;
    }
}
