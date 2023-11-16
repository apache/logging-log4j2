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
package org.apache.log4j;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.apache.log4j.bridge.AppenderAdapter;
import org.apache.log4j.bridge.AppenderWrapper;
import org.apache.log4j.bridge.LogEventWrapper;
import org.apache.log4j.helpers.AppenderAttachableImpl;
import org.apache.log4j.helpers.NullEnumeration;
import org.apache.log4j.legacy.core.CategoryUtil;
import org.apache.log4j.spi.AppenderAttachable;
import org.apache.log4j.spi.HierarchyEventListener;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.message.LocalizedMessage;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.apache.logging.log4j.util.Strings;

/**
 * Implementation of the Category class for compatibility, despite it having been deprecated a long, long time ago.
 */
public class Category implements AppenderAttachable {

    private static final String FQCN = Category.class.getName();

    /**
     * Tests if the named category exists (in the default hierarchy).
     *
     * @param name The name to test.
     * @return Whether the name exists.
     *
     * @deprecated Please use {@link LogManager#exists(String)} instead.
     * @since 0.8.5
     */
    @Deprecated
    public static Logger exists(final String name) {
        return LogManager.exists(name, StackLocatorUtil.getCallerClassLoader(2));
    }

    /**
     * Returns all the currently defined categories in the default hierarchy as an {@link java.util.Enumeration
     * Enumeration}.
     *
     * <p>
     * The root category is <em>not</em> included in the returned {@link Enumeration}.
     * </p>
     *
     * @return and Enumeration of the Categories.
     *
     * @deprecated Please use {@link LogManager#getCurrentLoggers()} instead.
     */
    @SuppressWarnings("rawtypes")
    @Deprecated
    public static Enumeration getCurrentCategories() {
        return LogManager.getCurrentLoggers(StackLocatorUtil.getCallerClassLoader(2));
    }

    /**
     * Gets the default LoggerRepository instance.
     *
     * @return the default LoggerRepository instance.
     * @deprecated Please use {@link LogManager#getLoggerRepository()} instead.
     * @since 1.0
     */
    @Deprecated
    public static LoggerRepository getDefaultHierarchy() {
        return LogManager.getLoggerRepository();
    }

    public static Category getInstance(@SuppressWarnings("rawtypes") final Class clazz) {
        return LogManager.getLogger(clazz.getName(), StackLocatorUtil.getCallerClassLoader(2));
    }

    public static Category getInstance(final String name) {
        return LogManager.getLogger(name, StackLocatorUtil.getCallerClassLoader(2));
    }

    public static Category getRoot() {
        return LogManager.getRootLogger(StackLocatorUtil.getCallerClassLoader(2));
    }

    private static String getSubName(final String name) {
        if (Strings.isEmpty(name)) {
            return null;
        }
        final int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(0, i) : Strings.EMPTY;
    }

    /**
     * Shuts down the current configuration.
     */
    public static void shutdown() {
        // Depth 2 gets the call site of this method.
        LogManager.shutdown(StackLocatorUtil.getCallerClassLoader(2));
    }

    /**
     * The name of this category.
     */
    protected String name;

    /**
     * Additivity is set to true by default, that is children inherit the appenders of their ancestors by default. If this
     * variable is set to <code>false</code> then the appenders found in the ancestors of this category are not used.
     * However, the children of this category will inherit its appenders, unless the children have their additivity flag set
     * to <code>false</code> too. See the user manual for more details.
     */
    protected boolean additive = true;

    /**
     * The assigned level of this category. The <code>level</code> variable need not be assigned a value in which case it is
     * inherited form the hierarchy.
     */
    protected volatile Level level;

    /**
     * The parent of this category. All categories have at least one ancestor which is the root category.
     */
    protected volatile Category parent;

    /**
     * Resource bundle for localized messages.
     */
    protected ResourceBundle bundle;

    private final org.apache.logging.log4j.Logger logger;

    /** Categories need to know what Hierarchy they are in. */
    protected LoggerRepository repository;

    AppenderAttachableImpl aai;

    /**
     * Constructor used by Logger to specify a LoggerContext.
     *
     * @param context The LoggerContext.
     * @param name The name of the Logger.
     */
    protected Category(final LoggerContext context, final String name) {
        this.name = name;
        this.logger = context.getLogger(name);
    }

    Category(final org.apache.logging.log4j.Logger logger) {
        this.logger = logger;
    }

    /**
     * Constructor exposed by Log4j 1.2.
     *
     * @param name The name of the Logger.
     */
    protected Category(final String name) {
        this(Hierarchy.getContext(), name);
    }

    /**
     * Add <code>newAppender</code> to the list of appenders of this Category instance.
     * <p>
     * If <code>newAppender</code> is already in the list of appenders, then it won't be added again.
     * </p>
     */
    @Override
    public void addAppender(final Appender appender) {
        if (appender != null) {
            if (LogManager.isLog4jCorePresent()) {
                CategoryUtil.addAppender(logger, AppenderAdapter.adapt(appender));
            } else {
                synchronized (this) {
                    if (aai == null) {
                        aai = new AppenderAttachableImpl();
                    }
                    aai.addAppender(appender);
                }
            }
            repository.fireAddAppenderEvent(this, appender);
        }
    }

    /**
     * If <code>assertion</code> parameter is {@code false}, then logs <code>msg</code> as an {@link #error(Object) error}
     * statement.
     *
     * <p>
     * The <code>assert</code> method has been renamed to <code>assertLog</code> because <code>assert</code> is a language
     * reserved word in JDK 1.4.
     * </p>
     *
     * @param assertion The assertion.
     * @param msg The message to print if <code>assertion</code> is false.
     *
     * @since 1.2
     */
    public void assertLog(final boolean assertion, final String msg) {
        if (!assertion) {
            this.error(msg);
        }
    }

    /**
     * Call the appenders in the hierrachy starting at <code>this</code>. If no appenders could be found, emit a warning.
     * <p>
     * This method calls all the appenders inherited from the hierarchy circumventing any evaluation of whether to log or
     * not to log the particular log request.
     * </p>
     *
     * @param event the event to log.
     */
    public void callAppenders(final LoggingEvent event) {
        if (LogManager.isLog4jCorePresent()) {
            CategoryUtil.log(logger, new LogEventWrapper(event));
            return;
        }
        int writes = 0;
        for (Category c = this; c != null; c = c.parent) {
            // Protected against simultaneous call to addAppender, removeAppender,...
            synchronized (c) {
                if (c.aai != null) {
                    writes += c.aai.appendLoopOnAppenders(event);
                }
                if (!c.additive) {
                    break;
                }
            }
        }
        if (writes == 0) {
            repository.emitNoAppenderWarning(this);
        }
    }

    /**
     * Closes all attached appenders implementing the AppenderAttachable interface.
     *
     * @since 1.0
     */
    synchronized void closeNestedAppenders() {
        final Enumeration enumeration = this.getAllAppenders();
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                final Appender a = (Appender) enumeration.nextElement();
                if (a instanceof AppenderAttachable) {
                    a.close();
                }
            }
        }
    }

    public void debug(final Object message) {
        maybeLog(FQCN, org.apache.logging.log4j.Level.DEBUG, message, null);
    }

    public void debug(final Object message, final Throwable t) {
        maybeLog(FQCN, org.apache.logging.log4j.Level.DEBUG, message, t);
    }

    public void error(final Object message) {
        maybeLog(FQCN, org.apache.logging.log4j.Level.ERROR, message, null);
    }

    public void error(final Object message, final Throwable t) {
        maybeLog(FQCN, org.apache.logging.log4j.Level.ERROR, message, t);
    }

    public void fatal(final Object message) {
        maybeLog(FQCN, org.apache.logging.log4j.Level.FATAL, message, null);
    }

    public void fatal(final Object message, final Throwable t) {
        maybeLog(FQCN, org.apache.logging.log4j.Level.FATAL, message, t);
    }

    /**
     * LoggerRepository forgot the fireRemoveAppenderEvent method, if using the stock Hierarchy implementation, then call
     * its fireRemove. Custom repositories can implement HierarchyEventListener if they want remove notifications.
     *
     * @param appender appender, may be null.
     */
    private void fireRemoveAppenderEvent(final Appender appender) {
        if (appender != null) {
            if (repository instanceof Hierarchy) {
                ((Hierarchy) repository).fireRemoveAppenderEvent(this, appender);
            } else if (repository instanceof HierarchyEventListener) {
                ((HierarchyEventListener) repository).removeAppenderEvent(this, appender);
            }
        }
    }

    private static Message createMessage(final Object message) {
        if (message instanceof String) {
            return new SimpleMessage((String) message);
        }
        if (message instanceof CharSequence) {
            return new SimpleMessage((CharSequence) message);
        }
        if (message instanceof Map) {
            return new MapMessage<>((Map<String, ?>) message);
        }
        if (message instanceof Message) {
            return (Message) message;
        }
        return new ObjectMessage(message);
    }

    public void forcedLog(final String fqcn, final Priority level, final Object message, final Throwable t) {
        final org.apache.logging.log4j.Level lvl = level.getVersion2Level();
        final Message msg = createMessage(message);
        if (logger instanceof ExtendedLogger) {
            ((ExtendedLogger) logger).logMessage(fqcn, lvl, null, msg, t);
        } else {
            logger.log(lvl, msg, t);
        }
    }

    public boolean getAdditivity() {
        return LogManager.isLog4jCorePresent() ? CategoryUtil.isAdditive(logger) : false;
    }

    /**
     * Get all the Log4j 1.x appenders contained in this category as an
     * {@link Enumeration}. Log4j 2.x appenders are omitted.
     *
     * @return Enumeration An enumeration of the appenders in this category.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Enumeration getAllAppenders() {
        if (LogManager.isLog4jCorePresent()) {
            final Collection<org.apache.logging.log4j.core.Appender> appenders =
                    CategoryUtil.getAppenders(logger).values();
            return Collections.enumeration(appenders.stream()
                    // omit native Log4j 2.x appenders
                    .filter(AppenderAdapter.Adapter.class::isInstance)
                    .map(AppenderWrapper::adapt)
                    .collect(Collectors.toSet()));
        }
        return aai == null ? NullEnumeration.getInstance() : aai.getAllAppenders();
    }

    /**
     * Look for the appender named as <code>name</code>.
     * <p>
     * Return the appender with that name if in the list. Return <code>null</code> otherwise.
     * </p>
     */
    @Override
    public Appender getAppender(final String name) {
        if (LogManager.isLog4jCorePresent()) {
            return AppenderWrapper.adapt(CategoryUtil.getAppenders(logger).get(name));
        }
        return aai != null ? aai.getAppender(name) : null;
    }

    public Priority getChainedPriority() {
        return getEffectiveLevel();
    }

    public Level getEffectiveLevel() {
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
            default:
                // TODO Should this be an IllegalStateException?
                return Level.OFF;
        }
    }

    /**
     * Gets the {@link LoggerRepository} where this <code>Category</code> instance is attached.
     *
     * @deprecated Please use {@link #getLoggerRepository()} instead.
     * @since 1.1
     */
    @Deprecated
    public LoggerRepository getHierarchy() {
        return repository;
    }

    public final Level getLevel() {
        return getEffectiveLevel();
    }

    private String getLevelStr(final Priority priority) {
        return priority == null ? null : priority.levelStr;
    }

    org.apache.logging.log4j.Logger getLogger() {
        return logger;
    }

    /**
     * Gets the {@link LoggerRepository} where this <code>Category</code> is attached.
     *
     * @since 1.2
     */
    public LoggerRepository getLoggerRepository() {
        return repository;
    }

    public final String getName() {
        return logger.getName();
    }

    public final Category getParent() {
        if (!LogManager.isLog4jCorePresent()) {
            return null;
        }
        final org.apache.logging.log4j.Logger parent = CategoryUtil.getParent(logger);
        final LoggerContext loggerContext = CategoryUtil.getLoggerContext(logger);
        if (parent == null || loggerContext == null) {
            return null;
        }
        final ConcurrentMap<String, Logger> loggers = Hierarchy.getLoggersMap(loggerContext);
        Category parentLogger = loggers.get(parent.getName());
        if (parentLogger == null) {
            parentLogger = new Category(parent);
            parentLogger.setHierarchy(getLoggerRepository());
        }
        return parentLogger;
    }

    public final Level getPriority() {
        return getEffectiveLevel();
    }

    public ResourceBundle getResourceBundle() {
        if (bundle != null) {
            return bundle;
        }
        String name = logger.getName();
        if (LogManager.isLog4jCorePresent()) {
            final LoggerContext ctx = CategoryUtil.getLoggerContext(logger);
            if (ctx != null) {
                final ConcurrentMap<String, Logger> loggers = Hierarchy.getLoggersMap(ctx);
                while ((name = getSubName(name)) != null) {
                    final Logger subLogger = loggers.get(name);
                    if (subLogger != null) {
                        final ResourceBundle rb = subLogger.bundle;
                        if (rb != null) {
                            return rb;
                        }
                    }
                }
            }
        }
        return null;
    }

    public void info(final Object message) {
        maybeLog(FQCN, org.apache.logging.log4j.Level.INFO, message, null);
    }

    public void info(final Object message, final Throwable t) {
        maybeLog(FQCN, org.apache.logging.log4j.Level.INFO, message, t);
    }

    /**
     * Is the appender passed as parameter attached to this category?
     *
     * @param appender The Appender to add.
     * @return true if the appender is attached.
     */
    @Override
    public boolean isAttached(final Appender appender) {
        return aai == null ? false : aai.isAttached(appender);
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    private boolean isEnabledFor(final org.apache.logging.log4j.Level level) {
        return logger.isEnabled(level);
    }

    public boolean isEnabledFor(final Priority level) {
        return isEnabledFor(level.getVersion2Level());
    }

    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    public boolean isFatalEnabled() {
        return logger.isFatalEnabled();
    }

    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    public void l7dlog(final Priority priority, final String key, final Object[] params, final Throwable t) {
        if (isEnabledFor(priority)) {
            final Message msg = new LocalizedMessage(bundle, key, params);
            forcedLog(FQCN, priority, msg, t);
        }
    }

    public void l7dlog(final Priority priority, final String key, final Throwable t) {
        if (isEnabledFor(priority)) {
            final Message msg = new LocalizedMessage(bundle, key, null);
            forcedLog(FQCN, priority, msg, t);
        }
    }

    public void log(final Priority priority, final Object message) {
        if (isEnabledFor(priority)) {
            forcedLog(FQCN, priority, message, null);
        }
    }

    public void log(final Priority priority, final Object message, final Throwable t) {
        if (isEnabledFor(priority)) {
            forcedLog(FQCN, priority, message, t);
        }
    }

    public void log(final String fqcn, final Priority priority, final Object message, final Throwable t) {
        if (isEnabledFor(priority)) {
            forcedLog(fqcn, priority, message, t);
        }
    }

    void maybeLog(
            final String fqcn,
            final org.apache.logging.log4j.Level level,
            final Object message,
            final Throwable throwable) {
        if (logger.isEnabled(level)) {
            final Message msg = createMessage(message);
            if (logger instanceof ExtendedLogger) {
                ((ExtendedLogger) logger).logMessage(fqcn, level, null, msg, throwable);
            } else {
                logger.log(level, msg, throwable);
            }
        }
    }

    /**
     * Removes all previously added appenders from this Category instance.
     * <p>
     * This is useful when re-reading configuration information.
     * </p>
     */
    @Override
    public void removeAllAppenders() {
        if (aai != null) {
            final Vector appenders = new Vector();
            for (final Enumeration iter = aai.getAllAppenders(); iter != null && iter.hasMoreElements(); ) {
                appenders.add(iter.nextElement());
            }
            aai.removeAllAppenders();
            for (final Object appender : appenders) {
                fireRemoveAppenderEvent((Appender) appender);
            }
            aai = null;
        }
    }

    /**
     * Removes the appender passed as parameter form the list of appenders.
     *
     * @param appender The Appender to remove.
     * @since 0.8.2
     */
    @Override
    public void removeAppender(final Appender appender) {
        if (appender == null || aai == null) {
            return;
        }
        final boolean wasAttached = aai.isAttached(appender);
        aai.removeAppender(appender);
        if (wasAttached) {
            fireRemoveAppenderEvent(appender);
        }
    }

    /**
     * Removes the appender with the name passed as parameter form the list of appenders.
     *
     * @param name The Appender to remove.
     * @since 0.8.2
     */
    @Override
    public void removeAppender(final String name) {
        if (name == null || aai == null) {
            return;
        }
        final Appender appender = aai.getAppender(name);
        aai.removeAppender(name);
        if (appender != null) {
            fireRemoveAppenderEvent(appender);
        }
    }

    public void setAdditivity(final boolean additivity) {
        if (LogManager.isLog4jCorePresent()) {
            CategoryUtil.setAdditivity(logger, additivity);
        }
    }

    /**
     * Only the Hiearchy class can set the hiearchy of a category. Default package access is MANDATORY here.
     */
    final void setHierarchy(final LoggerRepository repository) {
        this.repository = repository;
    }

    public void setLevel(final Level level) {
        setLevel(level != null ? level.getVersion2Level() : null);
    }

    private void setLevel(final org.apache.logging.log4j.Level level) {
        if (LogManager.isLog4jCorePresent()) {
            CategoryUtil.setLevel(logger, level);
        }
    }

    public void setPriority(final Priority priority) {
        setLevel(priority != null ? priority.getVersion2Level() : null);
    }

    public void setResourceBundle(final ResourceBundle bundle) {
        this.bundle = bundle;
    }

    public void warn(final Object message) {
        maybeLog(FQCN, org.apache.logging.log4j.Level.WARN, message, null);
    }

    public void warn(final Object message, final Throwable t) {
        maybeLog(FQCN, org.apache.logging.log4j.Level.WARN, message, t);
    }
}
