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
package org.apache.logging.log4j.core.appender;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.status.StatusLogger;

/**
 *
 */
public class DefaultErrorHandler implements ErrorHandler {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final int MAX_EXCEPTIONS = 3;

    private static final long EXCEPTION_INTERVAL = TimeUnit.MINUTES.toNanos(5);

    private int exceptionCount = 0;

    private long lastException = System.nanoTime() - EXCEPTION_INTERVAL - 1;

    private final Appender appender;

    public DefaultErrorHandler(final Appender appender) {
        this.appender = appender;
    }


    /**
     * Handle an error with a message.
     * @param msg The message.
     */
    @Override
    public void error(final String msg) {
        final long current = System.nanoTime();
        if (current - lastException > EXCEPTION_INTERVAL || exceptionCount++ < MAX_EXCEPTIONS) {
            LOGGER.error(msg);
        }
        lastException = current;
    }

    /**
     * Handle an error with a message and an exception.
     * @param msg The message.
     * @param t The Throwable.
     */
    @Override
    public void error(final String msg, final Throwable t) {
        final long current = System.nanoTime();
        if (current - lastException > EXCEPTION_INTERVAL || exceptionCount++ < MAX_EXCEPTIONS) {
            LOGGER.error(msg, t);
        }
        lastException = current;
        if (!appender.ignoreExceptions() && t != null && !(t instanceof AppenderLoggingException)) {
            throw new AppenderLoggingException(msg, t);
        }
    }

    /**
     * Handle an error with a message, and exception and a logging event.
     * @param msg The message.
     * @param event The LogEvent.
     * @param t The Throwable.
     */
    @Override
    public void error(final String msg, final LogEvent event, final Throwable t) {
        final long current = System.nanoTime();
        if (current - lastException > EXCEPTION_INTERVAL || exceptionCount++ < MAX_EXCEPTIONS) {
            LOGGER.error(msg, t);
        }
        lastException = current;
        if (!appender.ignoreExceptions() && t != null && !(t instanceof AppenderLoggingException)) {
            throw new AppenderLoggingException(msg, t);
        }
    }

    public Appender getAppender() {
        return appender;
    }
}
