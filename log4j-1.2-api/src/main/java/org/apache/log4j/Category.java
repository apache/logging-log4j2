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
package org.apache.log4j;

import java.util.Enumeration;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.helpers.NullEnumeration;
import org.apache.log4j.spi.LoggerFactory;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.util.NameUtil;
import org.apache.logging.log4j.message.LocalizedMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.spi.AbstractLoggerAdapter;
import org.apache.logging.log4j.util.Strings;


/**
 * Implementation of the Category class for compatibility, despite it having been deprecated a long, long time ago.
 */
public class Category {

    private static PrivateAdapter adapter = new PrivateAdapter();

    private static final Map<LoggerContext, ConcurrentMap<String, Logger>> CONTEXT_MAP =
        new WeakHashMap<>();

    private static final String FQCN = Category.class.getName();

    /**
     * Resource bundle for localized messages.
     */
    protected ResourceBundle bundle = null;

    private final org.apache.logging.log4j.core.Logger logger;

    /**
     * Constructor used by Logger to specify a LoggerContext.
     * @param context The LoggerContext.
     * @param name The name of the Logger.
     */
    protected Category(final LoggerContext context, final String name) {
        this.logger = context.getLogger(name);
    }

    /**
     * Constructor exposed by Log4j 1.2.
     * @param name The name of the Logger.
     */
    protected Category(final String name) {
        this(PrivateManager.getContext(), name);
    }

    private Category(final org.apache.logging.log4j.core.Logger logger) {
        this.logger = logger;
    }

    public static Category getInstance(final String name) {
        return getInstance(PrivateManager.getContext(), name, adapter);
    }

    static Logger getInstance(final LoggerContext context, final String name) {
        return getInstance(context, name, adapter);
    }

    static Logger getInstance(final LoggerContext context, final String name, final LoggerFactory factory) {
        final ConcurrentMap<String, Logger> loggers = getLoggersMap(context);
        Logger logger = loggers.get(name);
        if (logger != null) {
            return logger;
        }
        logger = factory.makeNewLoggerInstance(name);
        final Logger prev = loggers.putIfAbsent(name, logger);
        return prev == null ? logger : prev;
    }

    static Logger getInstance(final LoggerContext context, final String name, final PrivateAdapter factory) {
        final ConcurrentMap<String, Logger> loggers = getLoggersMap(context);
        Logger logger = loggers.get(name);
        if (logger != null) {
            return logger;
        }
        logger = factory.newLogger(name, context);
        final Logger prev = loggers.putIfAbsent(name, logger);
        return prev == null ? logger : prev;
    }

    public static Category getInstance(@SuppressWarnings("rawtypes") final Class clazz) {
        return getInstance(clazz.getName());
    }

    static Logger getInstance(final LoggerContext context, @SuppressWarnings("rawtypes") final Class clazz) {
        return getInstance(context, clazz.getName());
    }

    public final String getName() {
        return logger.getName();
    }

    org.apache.logging.log4j.core.Logger getLogger() {
        return logger;
    }

    public final Category getParent() {
        final org.apache.logging.log4j.core.Logger parent = logger.getParent();
        if (parent == null) {
            return null;
        }
        final ConcurrentMap<String, Logger> loggers = getLoggersMap(logger.getContext());
        final Logger l = loggers.get(parent.getName());
        return l == null ? new Category(parent) : l;
    }

    public static Category getRoot() {
        return getInstance(Strings.EMPTY);
    }

    static Logger getRoot(final LoggerContext context) {
        return getInstance(context, Strings.EMPTY);
    }

    private static ConcurrentMap<String, Logger> getLoggersMap(final LoggerContext context) {
        synchronized (CONTEXT_MAP) {
            ConcurrentMap<String, Logger> map = CONTEXT_MAP.get(context);
            if (map == null) {
                map = new ConcurrentHashMap<>();
                CONTEXT_MAP.put(context, map);
            }
            return map;
        }
    }

    /**
     Returns all the currently defined categories in the default
     hierarchy as an {@link java.util.Enumeration Enumeration}.

     <p>The root category is <em>not</em> included in the returned
     {@link Enumeration}.
     @return and Enumeration of the Categories.

     @deprecated Please use {@link LogManager#getCurrentLoggers()} instead.
     */
    @SuppressWarnings("rawtypes")
    @Deprecated
    public static Enumeration getCurrentCategories() {
        return LogManager.getCurrentLoggers();
    }

    public final Level getEffectiveLevel() {
        switch (logger.getLevel().getStandardLevel()) {
        case ALL:
            return Level.ALL;
        case TRACE:
            return Level.TRACE;
        case DEBUG:
            return Level.DEBUG;
        case INFO:
            return Level.INFO;
        case WARN:
            return Level.WARN;
        case ERROR:
            return Level.ERROR;
        case FATAL:
            return Level.FATAL;
        case OFF:
            return Level.OFF;
        default:
            // TODO Should this be an IllegalStateException?
            return Level.OFF;
        }
    }

    public final Priority getChainedPriority() {
        return getEffectiveLevel();
    }

    public final Level getLevel() {
        return getEffectiveLevel();
    }

    public void setLevel(final Level level) {
        logger.setLevel(org.apache.logging.log4j.Level.toLevel(level.levelStr));
    }

    public final Level getPriority() {
        return getEffectiveLevel();
    }

    public void setPriority(final Priority priority) {
        logger.setLevel(org.apache.logging.log4j.Level.toLevel(priority.levelStr));
    }

    public void debug(final Object message) {
        maybeLog(FQCN, org.apache.logging.log4j.Level.DEBUG, message, null);
    }

    public void debug(final Object message, final Throwable t) {
        maybeLog(FQCN, org.apache.logging.log4j.Level.DEBUG, message, t);
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public void error(final Object message) {
        maybeLog(FQCN, org.apache.logging.log4j.Level.ERROR, message, null);
    }

    public void error(final Object message, final Throwable t) {
        maybeLog(FQCN, org.apache.logging.log4j.Level.ERROR, message, t);
    }

    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    public void warn(final Object message) {
        maybeLog(FQCN, org.apache.logging.log4j.Level.WARN, message, null);
    }

    public void warn(final Object message, final Throwable t) {
        maybeLog(FQCN, org.apache.logging.log4j.Level.WARN, message, t);
    }

    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    public void fatal(final Object message) {
        maybeLog(FQCN, org.apache.logging.log4j.Level.FATAL, message, null);
    }

    public void fatal(final Object message, final Throwable t) {
        maybeLog(FQCN, org.apache.logging.log4j.Level.FATAL, message, t);
    }

    public boolean isFatalEnabled() {
        return logger.isFatalEnabled();
    }

    public void info(final Object message) {
        maybeLog(FQCN, org.apache.logging.log4j.Level.INFO, message, null);
    }

    public void info(final Object message, final Throwable t) {
        maybeLog(FQCN, org.apache.logging.log4j.Level.INFO, message, t);
    }

    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    public void trace(final Object message) {
        maybeLog(FQCN, org.apache.logging.log4j.Level.TRACE, message, null);
    }

    public void trace(final Object message, final Throwable t) {
        maybeLog(FQCN, org.apache.logging.log4j.Level.TRACE, message, t);
    }

    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    public boolean isEnabledFor(final Priority level) {
        final org.apache.logging.log4j.Level lvl = org.apache.logging.log4j.Level.toLevel(level.toString());
        return isEnabledFor(lvl);
    }

    /**
     * No-op implementation.
     * @param appender The Appender to add.
     */
    public void addAppender(final Appender appender) {
    }

    /**
     * No-op implementation.
     * @param event The logging event.
     */
    public void callAppenders(final LoggingEvent event) {
    }

    @SuppressWarnings("rawtypes")
    public Enumeration getAllAppenders() {
        return NullEnumeration.getInstance();
    }

    /**
     * No-op implementation.
     * @param name The name of the Appender.
     * @return null.
     */
    public Appender getAppender(final String name) {
        return null;
    }

    /**
     Is the appender passed as parameter attached to this category?
     * @param appender The Appender to add.
     * @return true if the appender is attached.
     */
    public boolean isAttached(final Appender appender) {
        return false;
    }

    /**
     * No-op implementation.
     */
    public void removeAllAppenders() {
    }

    /**
     * No-op implementation.
     * @param appender The Appender to remove.
     */
    public void removeAppender(final Appender appender) {
    }

    /**
     * No-op implementation.
     * @param name The Appender to remove.
     */
    public void removeAppender(final String name) {
    }

    /**
     * No-op implementation.
     */
    public static void shutdown() {
    }


    public void forcedLog(final String fqcn, final Priority level, final Object message, final Throwable t) {
        final org.apache.logging.log4j.Level lvl = org.apache.logging.log4j.Level.toLevel(level.toString());
        final Message msg = message instanceof Message ? (Message) message : new ObjectMessage(message);
        logger.logMessage(fqcn, lvl, null, msg, t);
    }

    public boolean exists(final String name) {
        return PrivateManager.getContext().hasLogger(name);
    }

    public boolean getAdditivity() {
        return logger.isAdditive();
    }

    public void setAdditivity(final boolean additivity) {
        logger.setAdditive(additivity);
    }

    public void setResourceBundle(final ResourceBundle bundle) {
        this.bundle = bundle;
    }

    public ResourceBundle getResourceBundle() {
        if (bundle != null) {
            return bundle;
        }
        String name = logger.getName();
        final ConcurrentMap<String, Logger> loggers = getLoggersMap(logger.getContext());
        while ((name = NameUtil.getSubName(name)) != null) {
            final Logger subLogger = loggers.get(name);
            if (subLogger != null) {
				final ResourceBundle rb = subLogger.bundle;
                if (rb != null) {
                    return rb;
                }
            }
        }
        return null;
    }

    /**
     If <code>assertion</code> parameter is {@code false}, then
     logs <code>msg</code> as an {@link #error(Object) error} statement.

     <p>The <code>assert</code> method has been renamed to
     <code>assertLog</code> because <code>assert</code> is a language
     reserved word in JDK 1.4.

     @param assertion The assertion.
     @param msg The message to print if <code>assertion</code> is
     false.

     @since 1.2
     */
    public void assertLog(final boolean assertion, final String msg) {
        if (!assertion) {
            this.error(msg);
        }
    }

    public void l7dlog(final Priority priority, final String key, final Throwable t) {
        if (isEnabledFor(priority)) {
            final Message msg = new LocalizedMessage(bundle, key, null);
            forcedLog(FQCN, priority, msg, t);
        }
    }

    public void l7dlog(final Priority priority, final String key, final Object[] params, final Throwable t) {
        if (isEnabledFor(priority)) {
            final Message msg = new LocalizedMessage(bundle, key, params);
            forcedLog(FQCN, priority, msg, t);
        }
    }

    public void log(final Priority priority, final Object message, final Throwable t) {
        if (isEnabledFor(priority)) {
            final Message msg = new ObjectMessage(message);
            forcedLog(FQCN, priority, msg, t);
        }
    }

    public void log(final Priority priority, final Object message) {
        if (isEnabledFor(priority)) {
            final Message msg = new ObjectMessage(message);
            forcedLog(FQCN, priority, msg, null);
        }
    }

    public void log(final String fqcn, final Priority priority, final Object message, final Throwable t) {
        if (isEnabledFor(priority)) {
            final Message msg = new ObjectMessage(message);
            forcedLog(fqcn, priority, msg, t);
        }
    }

    private void maybeLog(final String fqcn, final org.apache.logging.log4j.Level level,
            final Object message, final Throwable throwable) {
        if (logger.isEnabled(level, null, message, throwable)) {
            logger.logMessage(FQCN, level, null, new ObjectMessage(message), throwable);
        }
    }

    private static class PrivateAdapter extends AbstractLoggerAdapter<Logger> {

        @Override
        protected Logger newLogger(final String name, final org.apache.logging.log4j.spi.LoggerContext context) {
            return new Logger((LoggerContext) context, name);
        }

        @Override
        protected org.apache.logging.log4j.spi.LoggerContext getContext() {
            return PrivateManager.getContext();
        }
    }

    /**
     * Private LogManager.
     */
    private static class PrivateManager extends org.apache.logging.log4j.LogManager {
        private static final String FQCN = Category.class.getName();

        public static LoggerContext getContext() {
            return (LoggerContext) getContext(FQCN, false);
        }

        public static org.apache.logging.log4j.Logger getLogger(final String name) {
            return getLogger(FQCN, name);
        }
    }

    private boolean isEnabledFor(final org.apache.logging.log4j.Level level) {
        return logger.isEnabled(level, null, null);
    }

}
