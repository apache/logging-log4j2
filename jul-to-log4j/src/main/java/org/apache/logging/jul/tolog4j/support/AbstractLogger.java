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
package org.apache.logging.jul.tolog4j.support;

import static org.apache.logging.log4j.spi.AbstractLogger.ENTRY_MARKER;
import static org.apache.logging.log4j.spi.AbstractLogger.EXIT_MARKER;
import static org.apache.logging.log4j.spi.AbstractLogger.THROWING_MARKER;

import java.util.ResourceBundle;
import java.util.function.Supplier;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.apache.logging.jul.tolog4j.LevelTranslator;
import org.apache.logging.log4j.BridgeAware;
import org.apache.logging.log4j.LogBuilder;
import org.apache.logging.log4j.message.DefaultFlowMessageFactory;
import org.apache.logging.log4j.message.LocalizedMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.ExtendedLogger;

/**
 * Log4j API implementation of the JUL {@link Logger} class.
 * <p>
 *     <strong>Note that this implementation does <em>not</em> use the {@link java.util.logging.Handler} class.</strong>
 *     Instead,
 *     logging is delegated to the underlying Log4j {@link org.apache.logging.log4j.Logger}
 *     which may be implemented in one of many different ways.
 *     Consult the documentation for your Log4j API Provider for more details.
 * </p>
 * <p>
 *     Note that the methods {@link #getParent()} and mutator methods such as {@link #setLevel(java.util.logging.Level)}
 *     must be provided by implementations of this class.
 *     The default {@link org.apache.logging.jul.tolog4j.internal.ApiLogger} implementations just ignores them.
 *     If you need support for these methods, then you'll need to provide your own
 *     {@link org.apache.logging.jul.tolog4j.spi.AbstractLoggerAdapter}.
 * </p>
 *
 * @since 3.0.0
 */
public abstract class AbstractLogger extends Logger {

    private final ExtendedLogger logger;
    private static final String FQCN = AbstractLogger.class.getName();

    protected AbstractLogger(final ExtendedLogger logger) {
        super(logger.getName(), null);
        final Level javaLevel = LevelTranslator.toJavaLevel(logger.getLevel());
        super.setLevel(javaLevel);
        this.logger = logger;
    }

    @Override
    public void log(final LogRecord record) {
        final org.apache.logging.log4j.Level level = LevelTranslator.toLevel(record.getLevel());
        final Object[] parameters = record.getParameters();
        final MessageFactory messageFactory = logger.getMessageFactory();
        final Message message = parameters == null
                ? messageFactory.newMessage(record.getMessage()) /* LOG4J2-1251: not formatted case */
                : messageFactory.newMessage(record.getMessage(), parameters);
        final Throwable thrown = record.getThrown();
        logger.logIfEnabled(FQCN, level, null, message, thrown);
    }

    // <editor-fold desc="Configuration methods">
    // Methods

    @Override
    public abstract void setFilter(Filter newFilter) throws SecurityException;

    @Override
    public abstract void setLevel(Level newLevel) throws SecurityException;

    @Override
    public abstract void addHandler(Handler handler) throws SecurityException;

    @Override
    public abstract void removeHandler(Handler handler) throws SecurityException;

    @Override
    public abstract void setUseParentHandlers(boolean useParentHandlers);

    @Override
    public abstract void setParent(Logger parent);

    @Override
    public Filter getFilter() {
        return null;
    }

    /**
     * Returns the configured level of a logger.
     * <p>
     *     <strong>Implementation note:</strong> this method returns the level <strong>explicitly</strong> configured
     *     in the Log4j API logging implementation and is implementation specific.
     *     The default implementation always returns {@code null}.
     * </p>
     * <p>
     *     To test if a logger is enabled for a specific logging level, i.e. to test its <strong>effective</strong>
     *     level, use {@link Logger#isLoggable(Level)}.
     * </p>
     * @see #isLoggable(Level)
     */
    @Override
    public Level getLevel() {
        return null;
    }

    @Override
    public Handler[] getHandlers() {
        return new Handler[0];
    }

    @Override
    public boolean getUseParentHandlers() {
        return false;
    }

    @Override
    public Logger getParent() {
        return null;
    }

    // </editor-fold>

    // <editor-fold desc="Logging methods">
    // Implementation of methods used for logging

    @Override
    public boolean isLoggable(final Level level) {
        return logger.isEnabled(LevelTranslator.toLevel(level));
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    private org.apache.logging.log4j.util.Supplier<String> toLog4jSupplier(Supplier<String> msgSupplier) {
        return msgSupplier::get;
    }

    private org.apache.logging.log4j.util.Supplier<Message> toMessageSupplier(Supplier<String> msgSupplier) {
        return () -> logger.getMessageFactory().newMessage(msgSupplier.get());
    }

    private org.apache.logging.log4j.util.Supplier<Message> toMessageSupplier(ResourceBundle bundle, String msg) {
        return () -> new LocalizedMessage(bundle, msg);
    }

    private org.apache.logging.log4j.util.Supplier<Message> toMessageSupplier(
            ResourceBundle bundle, String msg, Object[] params) {
        return () -> new LocalizedMessage(bundle, msg, params);
    }

    private StackTraceElement toLocation(String sourceClass, String sourceMethod) {
        return new StackTraceElement(sourceClass, sourceMethod, null, 0);
    }

    @Override
    public void log(final Level level, final String msg) {
        logger.logIfEnabled(FQCN, LevelTranslator.toLevel(level), null, msg);
    }

    /**
     * @since 3.0.0
     */
    @Override
    public void log(Level level, Supplier<String> msgSupplier) {
        logger.logIfEnabled(FQCN, LevelTranslator.toLevel(level), null, toLog4jSupplier(msgSupplier), null);
    }

    @Override
    public void log(final Level level, final String msg, final Object param1) {
        logger.logIfEnabled(FQCN, LevelTranslator.toLevel(level), null, msg, param1);
    }

    @Override
    public void log(final Level level, final String msg, final Object[] params) {
        logger.logIfEnabled(FQCN, LevelTranslator.toLevel(level), null, msg, params);
    }

    @Override
    public void log(final Level level, final String msg, final Throwable thrown) {
        logger.logIfEnabled(FQCN, LevelTranslator.toLevel(level), null, msg, thrown);
    }

    /**
     * @since 3.0.0
     */
    @Override
    public void log(Level level, Throwable thrown, Supplier<String> msgSupplier) {
        logger.logIfEnabled(FQCN, LevelTranslator.toLevel(level), null, toLog4jSupplier(msgSupplier), thrown);
    }

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg) {
        logger.atLevel(LevelTranslator.toLevel(level))
                .withLocation(toLocation(sourceClass, sourceMethod))
                .log(msg);
    }

    /**
     * @since 3.0.0
     */
    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, Supplier<String> msgSupplier) {
        logger.atLevel(LevelTranslator.toLevel(level))
                .withLocation(toLocation(sourceClass, sourceMethod))
                .log(toMessageSupplier(msgSupplier));
    }

    @Override
    public void logp(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final String msg,
            final Object param1) {
        logger.atLevel(LevelTranslator.toLevel(level))
                .withLocation(toLocation(sourceClass, sourceMethod))
                .log(msg, param1);
    }

    @Override
    public void logp(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final String msg,
            final Object[] params) {
        logger.atLevel(LevelTranslator.toLevel(level))
                .withLocation(toLocation(sourceClass, sourceMethod))
                .log(msg, params);
    }

    @Override
    public void logp(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final String msg,
            final Throwable thrown) {
        logger.atLevel(LevelTranslator.toLevel(level))
                .withLocation(toLocation(sourceClass, sourceMethod))
                .withThrowable(thrown)
                .log(msg);
    }

    /**
     * @since 3.0.0
     */
    @Override
    public void logp(
            Level level, String sourceClass, String sourceMethod, Throwable thrown, Supplier<String> msgSupplier) {
        logger.atLevel(LevelTranslator.toLevel(level))
                .withLocation(toLocation(sourceClass, sourceMethod))
                .withThrowable(thrown)
                .log(toMessageSupplier(msgSupplier));
    }

    /**
     * @since 3.0.0
     */
    @Override
    public void logrb(
            Level level, String sourceClass, String sourceMethod, ResourceBundle bundle, String msg, Object... params) {
        logger.atLevel(LevelTranslator.toLevel(level))
                .withLocation(toLocation(sourceClass, sourceMethod))
                .log(toMessageSupplier(bundle, msg, params));
    }

    @Override
    public void logrb(
            Level level, String sourceClass, String sourceMethod, ResourceBundle bundle, String msg, Throwable thrown) {
        logger.atLevel(LevelTranslator.toLevel(level))
                .withLocation(toLocation(sourceClass, sourceMethod))
                .withThrowable(thrown)
                .log(toMessageSupplier(bundle, msg));
    }

    /**
     * @since 3.0.0
     */
    @Override
    public void logrb(Level level, ResourceBundle bundle, String msg, Object... params) {
        logger.logIfEnabled(FQCN, LevelTranslator.toLevel(level), null, toMessageSupplier(bundle, msg, params), null);
    }

    /**
     * @since 3.0.0
     */
    @Override
    public void logrb(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
        LogBuilder builder = logger.atLevel(LevelTranslator.toLevel(level)).withThrowable(thrown);
        if (builder instanceof BridgeAware bridgeAware) {
            bridgeAware.setEntryPoint(FQCN);
        }
        builder.log(toMessageSupplier(bundle, msg));
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod) {
        logger.atTrace()
                .withLocation(toLocation(sourceClass, sourceMethod))
                .withMarker(ENTRY_MARKER)
                .log(DefaultFlowMessageFactory.INSTANCE.newEntryMessage(null, (Object[]) null));
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod, final Object param1) {
        logger.atTrace()
                .withLocation(toLocation(sourceClass, sourceMethod))
                .withMarker(ENTRY_MARKER)
                .log(DefaultFlowMessageFactory.INSTANCE.newEntryMessage(null, param1));
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod, final Object[] params) {
        logger.atTrace()
                .withLocation(toLocation(sourceClass, sourceMethod))
                .withMarker(ENTRY_MARKER)
                .log(DefaultFlowMessageFactory.INSTANCE.newEntryMessage(null, params));
    }

    @Override
    public void exiting(final String sourceClass, final String sourceMethod) {
        logger.atTrace()
                .withLocation(toLocation(sourceClass, sourceMethod))
                .withMarker(EXIT_MARKER)
                .log(DefaultFlowMessageFactory.INSTANCE.newExitMessage(null, (Object) null));
    }

    @Override
    public void exiting(final String sourceClass, final String sourceMethod, final Object result) {
        logger.atTrace()
                .withLocation(toLocation(sourceClass, sourceMethod))
                .withMarker(EXIT_MARKER)
                .log(DefaultFlowMessageFactory.INSTANCE.newExitMessage(null, result));
    }

    @Override
    public void throwing(final String sourceClass, final String sourceMethod, final Throwable thrown) {
        logger.atTrace()
                .withLocation(toLocation(sourceClass, sourceMethod))
                .withMarker(THROWING_MARKER)
                .withThrowable(thrown)
                .log("Throwing");
    }

    @Override
    public void severe(final String msg) {
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.ERROR, null, msg);
    }

    /**
     * @since 3.0.0
     */
    @Override
    public void severe(Supplier<String> msgSupplier) {
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.ERROR, null, toLog4jSupplier(msgSupplier), null);
    }

    @Override
    public void warning(final String msg) {
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.WARN, null, msg);
    }

    @Override
    public void warning(Supplier<String> msgSupplier) {
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.WARN, null, toLog4jSupplier(msgSupplier), null);
    }

    @Override
    public void info(final String msg) {
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.INFO, null, msg);
    }

    @Override
    public void info(Supplier<String> msgSupplier) {
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.INFO, null, toLog4jSupplier(msgSupplier), null);
    }

    @Override
    public void config(final String msg) {
        logger.logIfEnabled(FQCN, LevelTranslator.CONFIG, null, msg);
    }

    @Override
    public void config(Supplier<String> msgSupplier) {
        logger.logIfEnabled(FQCN, LevelTranslator.CONFIG, null, toLog4jSupplier(msgSupplier), null);
    }

    @Override
    public void fine(final String msg) {
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.DEBUG, null, msg);
    }

    @Override
    public void fine(Supplier<String> msgSupplier) {
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.DEBUG, null, toLog4jSupplier(msgSupplier), null);
    }

    @Override
    public void finer(final String msg) {
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.TRACE, null, msg);
    }

    @Override
    public void finer(Supplier<String> msgSupplier) {
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.TRACE, null, toLog4jSupplier(msgSupplier), null);
    }

    @Override
    public void finest(final String msg) {
        logger.logIfEnabled(FQCN, LevelTranslator.FINEST, null, msg);
    }

    @Override
    public void finest(Supplier<String> msgSupplier) {
        logger.logIfEnabled(FQCN, LevelTranslator.FINEST, null, toLog4jSupplier(msgSupplier), null);
    }
    // </editor-fold>
}
