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

package org.apache.logging.log4j.jpl;

import java.lang.System.Logger;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import org.apache.logging.log4j.spi.ExtendedLogger;

/**
 * JPL {@link Logger logger} implementation that uses Log4j.
 * Implement all default {@link Logger} methods to ensure proper class resolution
 *
 * @since 2.14
 */
public class Log4jSystemLogger implements Logger {

    private final ExtendedLogger logger;

    private static final String FQCN = Log4jSystemLogger.class.getName();

    public Log4jSystemLogger(final ExtendedLogger logger) {
        this.logger = logger;
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isLoggable(final Level level) {
        return logger.isEnabled(getLevel(level));
    }

    @Override
    public void log(Level level, String msg) {
        log(level, (ResourceBundle) null, msg, (Object[]) null);
    }

    @Override
    public void log(Level level, Supplier<String> msgSupplier) {
        Objects.requireNonNull(msgSupplier);
        if (isLoggable(Objects.requireNonNull(level))) {
            log(level, (ResourceBundle) null, msgSupplier.get(), (Object[]) null);
        }
    }

    @Override
    public void log(Level level, Object obj) {
        Objects.requireNonNull(obj);
        if (isLoggable(Objects.requireNonNull(level))) {
            log(level, (ResourceBundle) null, obj.toString(), (Object[]) null);
        }
    }

    @Override
    public void log(Level level, String msg, Throwable thrown) {
        log(level, null, msg, thrown);
    }

    @Override
    public void log(Level level, Supplier<String> msgSupplier, Throwable thrown) {
        Objects.requireNonNull(msgSupplier);
        if (isLoggable(Objects.requireNonNull(level))) {
            log(level, null, msgSupplier.get(), thrown);
        }
    }

    @Override
    public void log(Level level, String format, Object... params) {
        log(level, null, format, params);
    }

    @Override
    public void log(final Level level, final ResourceBundle bundle, final String msg, final Throwable thrown) {
        logger.logIfEnabled(FQCN, getLevel(level), null, getResource(bundle, msg), thrown);
    }

    @Override
    public void log(final Level level, final ResourceBundle bundle, final String format, final Object... params) {
        logger.logIfEnabled(FQCN, getLevel(level), null, getResource(bundle, format), params);
    }

    private static org.apache.logging.log4j.Level getLevel(final Level level) {
        switch (level) {
            case OFF:
                return org.apache.logging.log4j.Level.OFF;
            case ERROR:
                return org.apache.logging.log4j.Level.ERROR;
            case WARNING:
                return org.apache.logging.log4j.Level.WARN;
            case INFO:
                return org.apache.logging.log4j.Level.INFO;
            case DEBUG:
                return org.apache.logging.log4j.Level.DEBUG;
            case TRACE:
                return org.apache.logging.log4j.Level.TRACE;
            case ALL:
                return org.apache.logging.log4j.Level.ALL;
        }
        return org.apache.logging.log4j.Level.ERROR;
    }

    private static String getResource(ResourceBundle bundle, String msg) {
        if (bundle == null || msg == null) {
            return msg;
        }
        try {
            return bundle.getString(msg);
        } catch (MissingResourceException e) {
            // ignore
            return msg;
        } catch (ClassCastException ex) {
            return bundle.getObject(msg).toString();
        }
    }
}
