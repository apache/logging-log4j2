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
package org.apache.logging.log4j.jdk;

import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.util.Assert;
import org.apache.logging.log4j.message.Message;

/**
 * Log4j implementation of the JUL {@link java.util.logging.Logger} class. <strong>Note that this implementation does
 * <em>not</em> use the {@link java.util.logging.Handler} class.</strong> Instead, logging is delegated to the
 * underlying Log4j {@link org.apache.logging.log4j.core.Logger} which uses
 * {@link org.apache.logging.log4j.core.Appender Appenders} instead.
 *
 * @since 2.1
 */
public class Logger extends java.util.logging.Logger {

    private static final String FQCN = java.util.logging.Logger.class.getName();

    private static final String PREFIX = "log4j.jul.";

    /**
     * The {@link ThreadContext} key where the value of {@link LogRecord#getThreadID()} will be stored.
     */
    public static final String THREAD_ID = PREFIX + "threadID";

    /**
     * The {@link ThreadContext} key where the value of {@link LogRecord#getSequenceNumber()} will be stored.
     */
    public static final String SEQUENCE_NUMBER = PREFIX + "sequenceNumber";

    /**
     * The {@link ThreadContext} key where the name of the {@link Level} will be stored. This is particularly useful
     * for custom Level implementations as well as for obtaining the exact Level that was used rather than the
     * equivalent Log4j {@link org.apache.logging.log4j.Level}.
     */
    public static final String LEVEL = PREFIX + "level";

    private final org.apache.logging.log4j.core.Logger logger;

    /**
     * Constructs a Logger using a Log4j {@link org.apache.logging.log4j.core.Logger}.
     *
     * @param logger the underlying Logger to base this Logger on
     */
    Logger(final org.apache.logging.log4j.core.Logger logger) {
        super(Assert.requireNonNull(logger, "No Logger provided").getName(), null);
        super.setLevel(Levels.toJavaLevel(logger.getLevel()));
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
        final org.apache.logging.log4j.Level level = Levels.toLevel(record.getLevel());
        final Message message = logger.getMessageFactory().newMessage(record.getMessage(), record.getParameters());
        final Throwable thrown = record.getThrown();
        // TODO: may want to use LoggerConfig.log(LogEvent) with a LogRecord/LogEvent hybrid
        logger.logIfEnabled(FQCN, level, null, message, thrown);
        // TODO: support handlers?
        ThreadContext.remove(THREAD_ID);
        ThreadContext.remove(SEQUENCE_NUMBER);
        ThreadContext.remove(LEVEL);
    }

    // support for Logger.getFilter()/Logger.setFilter()
    private boolean isFiltered(final LogRecord logRecord) {
        final Filter filter = getFilter();
        return filter != null && !filter.isLoggable(logRecord);
    }

    @Override
    public void setLevel(final Level level) throws SecurityException {
        logger.setLevel(Levels.toLevel(level));
        super.setLevel(level);
    }

    @Override
    public boolean isLoggable(final Level level) {
        return logger.isEnabled(Levels.toLevel(level));
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    /**
     * Marks the underlying {@link org.apache.logging.log4j.core.Logger} as additive.
     *
     * @param additive {@code true} if this Logger should be additive
     * @see org.apache.logging.log4j.core.Logger#setAdditive(boolean)
     */
    @Override
    public synchronized void setUseParentHandlers(final boolean additive) {
        logger.setAdditive(additive);
    }

    /**
     * Indicates if the underlying {@link org.apache.logging.log4j.core.Logger} is additive. <strong>Note that the
     * Log4j version of JDK Loggers do <em>not</em> use Handlers.</strong>
     *
     * @return {@code true} if this Logger is additive, or {@code false} otherwise
     * @see org.apache.logging.log4j.core.Logger#isAdditive()
     */
    @Override
    public synchronized boolean getUseParentHandlers() {
        return logger.isAdditive();
    }

    @Override
    public java.util.logging.Logger getParent() {
        final org.apache.logging.log4j.core.Logger parent = logger.getParent();
        return parent == null ? null : java.util.logging.Logger.getLogger(parent.getName());
    }
}
