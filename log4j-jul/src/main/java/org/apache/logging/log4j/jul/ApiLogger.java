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

package org.apache.logging.log4j.jul;

import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.spi.ExtendedLogger;

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

    private final WrappedLogger logger;
    private static final String FQCN = ApiLogger.class.getName();

    ApiLogger(final ExtendedLogger logger) {
        super(logger.getName(), null);
        super.setLevel(LevelTranslator.toJavaLevel(logger.getLevel()));
        this.logger = new WrappedLogger(logger);
    }

    @Override
    public void log(final LogRecord record) {
        if (isFiltered(record)) {
            return;
        }
        final org.apache.logging.log4j.Level level = LevelTranslator.toLevel(record.getLevel());
        final Message message = logger.getMessageFactory().newMessage(record.getMessage(), record.getParameters());
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
    public void setLevel(final Level newLevel) throws SecurityException {
        throw new UnsupportedOperationException("Cannot set level through log4j-api");
    }

    /**
     * Provides access to {@link Logger#setLevel(java.util.logging.Level)}. This method should only be used by child
     * classes.
     *
     * @see Logger#setLevel(java.util.logging.Level)
     */
    protected void doSetLevel(final Level newLevel) throws SecurityException {
        super.setLevel(newLevel);
    }

    /**
     * Unsupported operation.
     * @throws UnsupportedOperationException always
     */
    @Override
    public void setParent(final Logger parent) {
        throw new UnsupportedOperationException("Cannot set parent logger");
    }

    @Override
    public void log(final Level level, final String msg) {
        logger.log(LevelTranslator.toLevel(level), msg);
    }

    @Override
    public void log(final Level level, final String msg, final Object param1) {
        logger.log(LevelTranslator.toLevel(level), msg, param1);
    }

    @Override
    public void log(final Level level, final String msg, final Object[] params) {
        logger.log(LevelTranslator.toLevel(level), msg, params);
    }

    @Override
    public void log(final Level level, final String msg, final Throwable thrown) {
        logger.log(LevelTranslator.toLevel(level), msg, thrown);
    }

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg) {
        log(level, msg);
    }

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg,
                     final Object param1) {
        log(level, msg, param1);
    }

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg,
                     final Object[] params) {
        log(level, msg, params);
    }

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg,
                     final Throwable thrown) {
        log(level, msg, thrown);
    }

    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod, final String bundleName,
                      final String msg) {
        log(level, msg);
    }

    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod, final String bundleName,
                      final String msg, final Object param1) {
        log(level, msg, param1);
    }

    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod, final String bundleName,
                      final String msg, final Object[] params) {
        log(level, msg, params);
    }

    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod, final String bundleName,
                      final String msg, final Throwable thrown) {
        log(level, msg, thrown);
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod) {
        logger.entry();
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod, final Object param1) {
        logger.entry(param1);
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod, final Object[] params) {
        logger.entry(params);
    }

    @Override
    public void exiting(final String sourceClass, final String sourceMethod) {
        logger.exit();
    }

    @Override
    public void exiting(final String sourceClass, final String sourceMethod, final Object result) {
        logger.exit(result);
    }

    @Override
    public void throwing(final String sourceClass, final String sourceMethod, final Throwable thrown) {
        logger.throwing(thrown);
    }

    @Override
    public void severe(final String msg) {
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.ERROR, null, msg);
    }

    @Override
    public void warning(final String msg) {
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.WARN, null, msg);
    }

    @Override
    public void info(final String msg) {
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.INFO, null, msg);
    }

    @Override
    public void config(final String msg) {
        logger.logIfEnabled(FQCN, LevelTranslator.CONFIG, null, msg);
    }

    @Override
    public void fine(final String msg) {
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.DEBUG, null, msg);
    }

    @Override
    public void finer(final String msg) {
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.TRACE, null, msg);
    }

    @Override
    public void finest(final String msg) {
        logger.logIfEnabled(FQCN, LevelTranslator.FINEST, null, msg);
    }
}
