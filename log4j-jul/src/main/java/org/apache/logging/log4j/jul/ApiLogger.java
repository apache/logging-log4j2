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

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.spi.ExtendedLogger;

/**
 * Log4j API implementation of the JUL {@link Logger} class. <strong>Note that this implementation does
 * <em>not</em> use the {@link java.util.logging.Handler} class.</strong> Instead, logging is delegated to the
 * underlying Log4j {@link org.apache.logging.log4j.Logger} which may be implemented in one of many different ways.
 * Consult the documentation for your Log4j Provider for more details.
 * <p>Note that the methods {@link #getParent()} and {@link #setLevel(java.util.logging.Level)} are not supported by
 * this implementation. If you need support for these methods, then you'll need to use log4j-core.</p>
 * <p>Also note that {@link #setParent(java.util.logging.Logger)} is explicitly unsupported. Parent loggers are
 * determined using the syntax of the logger name; not through an arbitrary graph of loggers.</p>
 * 
 * @since 2.1
 */
public class ApiLogger extends Logger {

    private static final String FQCN = java.util.logging.Logger.class.getName();

    private static final String PREFIX = "log4j.jul.";

    /**
     * The {@link org.apache.logging.log4j.ThreadContext} key where the value of {@link java.util.logging.LogRecord#getThreadID()} will be stored.
     */
    public static final String THREAD_ID = PREFIX + "threadID";

    /**
     * The {@link org.apache.logging.log4j.ThreadContext} key where the value of {@link java.util.logging.LogRecord#getSequenceNumber()} will be stored.
     */
    public static final String SEQUENCE_NUMBER = PREFIX + "sequenceNumber";

    /**
     * The {@link org.apache.logging.log4j.ThreadContext} key where the name of the {@link java.util.logging.Level} will be stored. This is particularly useful
     * for custom Level implementations as well as for obtaining the exact Level that was used rather than the
     * equivalent Log4j {@link org.apache.logging.log4j.Level}.
     */
    public static final String LEVEL = PREFIX + "level";

    private final ExtendedLogger logger;

    ApiLogger(final ExtendedLogger logger) {
        super(logger.getName(), null);
        super.setLevel(LevelTranslator.toJavaLevel(logger.getLevel()));
        this.logger = logger;
    }

    @Override
    public void log(final LogRecord record) {
        if (isFiltered(record)) {
            return;
        }
        ThreadContext.put(THREAD_ID, Integer.toString(record.getThreadID()));
        ThreadContext.put(SEQUENCE_NUMBER, Long.toString(record.getSequenceNumber()));
        ThreadContext.put(LEVEL, record.getLevel().getName());
        final org.apache.logging.log4j.Level level = LevelTranslator.toLevel(record.getLevel());
        final Message message = logger.getMessageFactory().newMessage(record.getMessage(), record.getParameters());
        final Throwable thrown = record.getThrown();
        logger.logIfEnabled(FQCN, level, null, message, thrown);
        ThreadContext.remove(THREAD_ID);
        ThreadContext.remove(SEQUENCE_NUMBER);
        ThreadContext.remove(LEVEL);
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

    @Override
    public Logger getParent() {
        throw new UnsupportedOperationException("Cannot get parent logger through log4j-api");
    }

    /**
     * Unsupported operation.
     * @throws UnsupportedOperationException always
     */
    @Override
    public void setParent(final Logger parent) {
        throw new UnsupportedOperationException("Cannot set parent logger");
    }
}
