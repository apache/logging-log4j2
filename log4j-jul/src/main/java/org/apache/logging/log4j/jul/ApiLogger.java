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
package org.apache.logging.log4j.jul;

import java.util.ResourceBundle;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Log4j API implementation of the JUL {@link Logger} class. <strong>Note that this implementation does
 * <em>not</em> use the {@link java.util.logging.Handler} class.</strong> Instead, logging is delegated to the
 * underlying Log4j {@link org.apache.logging.log4j.Logger} which may be implemented in one of many different ways.
 * Consult the documentation for your Log4j Provider for more details.
 * <p>Note that the methods {@link #getParent()} and {@link #setLevel(java.util.logging.Level)} are not supported by
 * this implementation. If you need support for these methods, then you'll need to use log4j-core. The
 * {@link #getParent()} method will not fail (thanks to JUL API limitations), but it won't necessarily be
 * accurate!</p>
 * <p>Also note that {@link #setParent(java.util.logging.Logger)} is explicitly unsupported. Parent loggers are
 * determined using the syntax of the logger name; not through an arbitrary graph of loggers.</p>
 *
 * @since 2.1
 */
public class ApiLogger extends Logger {

    private static final org.apache.logging.log4j.Logger LOGGER = StatusLogger.getLogger();
    private static final String MUTATOR_DISABLED =
            "Ignoring call to `j.u.l.Logger.{}({})`, since the Log4j API does not provide methods to modify the underlying implementation.\n"
                    + "To modify the configuration using JUL, use an `AbstractLoggerAdapter` appropriate for your logging implementation.\n"
                    + "See https://logging.apache.org/log4j/3.x/log4j-jul.html#log4j.jul.loggerAdapter for more information.";

    private final WrappedLogger logger;
    private static final String FQCN = ApiLogger.class.getName();

    ApiLogger(final ExtendedLogger logger) {
        super(logger.getName(), null);
        this.logger = new WrappedLogger(logger);
    }

    @Override
    public void log(final LogRecord record) {
        if (isFiltered(record)) {
            return;
        }
        final org.apache.logging.log4j.Level level = LevelTranslator.toLevel(record.getLevel());
        final Object[] parameters = record.getParameters();
        final MessageFactory messageFactory = logger.getMessageFactory();
        final Message message = parameters == null
                ? messageFactory.newMessage(record.getMessage()) /* LOG4J2-1251: not formatted case */
                : messageFactory.newMessage(record.getMessage(), parameters);
        final Throwable thrown = record.getThrown();
        logger.logIfEnabled(FQCN, level, null, message, thrown);
    }

    // support for Logger.getFilter()/Logger.setFilter()
    boolean isFiltered(final LogRecord logRecord) {
        final Filter filter = getFilter();
        return filter != null && !filter.isLoggable(logRecord);
    }

    @Override
    public boolean isLoggable(final Level level) {
        return logger.isEnabled(LevelTranslator.toLevel(level));
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public Level getLevel() {
        // The configured level is NOT available through the Log4j API.
        // Some libraries, however, rely on the following assertion:
        //
        // logger.setLevel(level);
        // assert level.equals(logger.getLevel());
        //
        // See https://github.com/apache/logging-log4j2/issues/3119 for more details.
        return super.getLevel();
    }

    @Override
    public void setLevel(final Level newLevel) throws SecurityException {
        LOGGER.warn(MUTATOR_DISABLED, "setLevel", newLevel);
        // Some libraries rely on the following assertion:
        //
        // logger.setLevel(level);
        // assert level.equals(logger.getLevel());
        //
        // See https://github.com/apache/logging-log4j2/issues/3119 for more details.
        doSetLevel(newLevel);
    }

    /**
     * Provides access to {@link Logger#setLevel(java.util.logging.Level)}.
     * <p>
     *   This method should be called by all {@link #setLevel} implementations to check permissions.
     * </p>
     * @see Logger#setLevel(java.util.logging.Level)
     */
    protected void doSetLevel(final Level newLevel) throws SecurityException {
        super.setLevel(newLevel);
    }

    @Override
    public void setUseParentHandlers(boolean useParentHandlers) {
        LOGGER.warn(MUTATOR_DISABLED, "setLevel", useParentHandlers);
        super.setUseParentHandlers(useParentHandlers);
    }

    @Override
    public void addHandler(Handler handler) throws SecurityException {
        LOGGER.warn(MUTATOR_DISABLED, "addHandler", handler);
        super.addHandler(handler);
    }

    @Override
    public void removeHandler(Handler handler) throws SecurityException {
        LOGGER.warn(MUTATOR_DISABLED, "removeHandler", handler);
        super.removeHandler(handler);
    }

    @Override
    public void setResourceBundle(ResourceBundle bundle) {
        LOGGER.warn(
                "Ignoring call to `j.u.l.Logger.setResourceBundle({})`, since `o.a.l.l.jul.LogManager` currently does not support resource bundles.",
                bundle);
        super.setResourceBundle(bundle);
    }

    /**
     * Unsupported operation.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void setParent(final Logger parent) {
        throw new UnsupportedOperationException("Cannot set parent logger");
    }

    @Override
    public void log(final Level level, final String msg) {
        if (getFilter() == null) {
            logger.log(LevelTranslator.toLevel(level), msg);
        } else {
            super.log(level, msg);
        }
    }

    @Override
    public void log(final Level level, final String msg, final Object param1) {
        if (getFilter() == null) {
            logger.log(LevelTranslator.toLevel(level), msg, param1);
        } else {
            super.log(level, msg, param1);
        }
    }

    @Override
    public void log(final Level level, final String msg, final Object[] params) {
        if (getFilter() == null) {
            logger.log(LevelTranslator.toLevel(level), msg, params);
        } else {
            super.log(level, msg, params);
        }
    }

    @Override
    public void log(final Level level, final String msg, final Throwable thrown) {
        if (getFilter() == null) {
            logger.log(LevelTranslator.toLevel(level), msg, thrown);
        } else {
            super.log(level, msg, thrown);
        }
    }

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg) {
        log(level, msg);
    }

    @Override
    public void logp(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final String msg,
            final Object param1) {
        log(level, msg, param1);
    }

    @Override
    public void logp(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final String msg,
            final Object[] params) {
        log(level, msg, params);
    }

    @Override
    public void logp(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final String msg,
            final Throwable thrown) {
        log(level, msg, thrown);
    }

    @Override
    public void logrb(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final String bundleName,
            final String msg) {
        log(level, msg);
    }

    @Override
    public void logrb(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final String bundleName,
            final String msg,
            final Object param1) {
        log(level, msg, param1);
    }

    @Override
    public void logrb(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final String bundleName,
            final String msg,
            final Object[] params) {
        log(level, msg, params);
    }

    @Override
    public void logrb(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final String bundleName,
            final String msg,
            final Throwable thrown) {
        log(level, msg, thrown);
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod) {
        logger.traceEntry();
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod, final Object param1) {
        logger.traceEntry(null, param1);
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod, final Object[] params) {
        logger.traceEntry(null, params);
    }

    @Override
    public void exiting(final String sourceClass, final String sourceMethod) {
        logger.traceExit();
    }

    @Override
    public void exiting(final String sourceClass, final String sourceMethod, final Object result) {
        logger.traceExit(result);
    }

    @Override
    public void throwing(final String sourceClass, final String sourceMethod, final Throwable thrown) {
        logger.throwing(thrown);
    }

    @Override
    public void severe(final String msg) {
        if (getFilter() == null) {
            logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.ERROR, null, msg);
        } else {
            super.severe(msg);
        }
    }

    @Override
    public void warning(final String msg) {
        if (getFilter() == null) {
            logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.WARN, null, msg);
        } else {
            super.warning(msg);
        }
    }

    @Override
    public void info(final String msg) {
        if (getFilter() == null) {
            logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.INFO, null, msg);
        } else {
            super.info(msg);
        }
    }

    @Override
    public void config(final String msg) {
        if (getFilter() == null) {
            logger.logIfEnabled(FQCN, LevelTranslator.CONFIG, null, msg);
        } else {
            super.config(msg);
        }
    }

    @Override
    public void fine(final String msg) {
        if (getFilter() == null) {
            logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.DEBUG, null, msg);
        } else {
            super.fine(msg);
        }
    }

    @Override
    public void finer(final String msg) {
        if (getFilter() == null) {
            logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.TRACE, null, msg);
        } else {
            super.finer(msg);
        }
    }

    @Override
    public void finest(final String msg) {
        if (getFilter() == null) {
            logger.logIfEnabled(FQCN, LevelTranslator.FINEST, null, msg);
        } else {
            super.finest(msg);
        }
    }
}
