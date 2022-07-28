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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.legacy.core.ContextUtil;
import org.apache.log4j.or.ObjectRenderer;
import org.apache.log4j.or.RendererMap;
import org.apache.log4j.spi.HierarchyEventListener;
import org.apache.log4j.spi.LoggerFactory;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RendererSupport;
import org.apache.log4j.spi.ThrowableRenderer;
import org.apache.log4j.spi.ThrowableRendererSupport;
import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.spi.AbstractLoggerAdapter;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.util.StackLocatorUtil;

// WARNING This class MUST not have references to the Category or
// WARNING RootCategory classes in its static initialization neither
// WARNING directly nor indirectly.
/**
 * This class is specialized in retrieving loggers by name and also maintaining the logger hierarchy.
 *
 * <p>
 * <em>The casual user does not have to deal with this class directly.</em>
 * </p>
 * <p>
 * The structure of the logger hierarchy is maintained by the {@link #getLogger} method. The hierarchy is such that
 * children link to their parent but parents do not have any pointers to their children. Moreover, loggers can be
 * instantiated in any order, in particular descendant before ancestor.
 * </p>
 * <p>
 * In case a descendant is created before a particular ancestor, then it creates a provision node for the ancestor and
 * adds itself to the provision node. Other descendants of the same ancestor add themselves to the previously created
 * provision node.
 * </p>
 */
public class Hierarchy implements LoggerRepository, RendererSupport, ThrowableRendererSupport {

    private static class PrivateLoggerAdapter extends AbstractLoggerAdapter<Logger> {

        @Override
        protected org.apache.logging.log4j.spi.LoggerContext getContext() {
            return PrivateLogManager.getContext();
        }

        @Override
        protected Logger newLogger(final String name, final org.apache.logging.log4j.spi.LoggerContext context) {
            return new Logger(context, name);
        }
    }

    /**
     * Private LogManager.
     */
    private static class PrivateLogManager extends org.apache.logging.log4j.LogManager {
        private static final String FQCN = Hierarchy.class.getName();

        public static LoggerContext getContext() {
            return getContext(FQCN, false);
        }

        public static org.apache.logging.log4j.Logger getLogger(final String name) {
            return getLogger(FQCN, name);
        }
    }

    private static final PrivateLoggerAdapter LOGGER_ADAPTER = new PrivateLoggerAdapter();

    private static final WeakHashMap<LoggerContext, ConcurrentMap<String, Logger>> CONTEXT_MAP = new WeakHashMap<>();

    static LoggerContext getContext() {
        return PrivateLogManager.getContext();
    }

    private Logger getInstance(final LoggerContext context, final String name) {
        return getInstance(context, name, LOGGER_ADAPTER);
    }

    private Logger getInstance(final LoggerContext context, final String name, final LoggerFactory factory) {
        return getLoggersMap(context).computeIfAbsent(name, k -> {
            final Logger logger = factory.makeNewLoggerInstance(name);
            logger.setHierarchy(this);
            return logger;
        });
    }

    private Logger getInstance(final LoggerContext context, final String name, final PrivateLoggerAdapter factory) {
        return getLoggersMap(context).computeIfAbsent(name, k -> {
            final Logger logger = factory.newLogger(name, context);
            logger.setHierarchy(this);
            return logger;
        });
    }

    static ConcurrentMap<String, Logger> getLoggersMap(final LoggerContext context) {
        synchronized (CONTEXT_MAP) {
            return CONTEXT_MAP.computeIfAbsent(context, k -> new ConcurrentHashMap<>());
        }
    }

    private final LoggerFactory defaultFactory;
    private final Vector listeners;
    Hashtable ht;
    Logger root;
    RendererMap rendererMap;
    int thresholdInt;
    Level threshold;
    boolean emittedNoAppenderWarning;

    boolean emittedNoResourceBundleWarning;

    private ThrowableRenderer throwableRenderer;

    /**
     * Creates a new logger hierarchy.
     *
     * @param root The root of the new hierarchy.
     *
     */
    public Hierarchy(final Logger root) {
        ht = new Hashtable();
        listeners = new Vector(1);
        this.root = root;
        // Enable all level levels by default.
        setThreshold(Level.ALL);
        this.root.setHierarchy(this);
        rendererMap = new RendererMap();
        defaultFactory = new DefaultCategoryFactory();
    }

    @Override
    public void addHierarchyEventListener(final HierarchyEventListener listener) {
        if (listeners.contains(listener)) {
            LogLog.warn("Ignoring attempt to add an existent listener.");
        } else {
            listeners.addElement(listener);
        }
    }

    /**
     * Adds an object renderer for a specific class.
     */
    public void addRenderer(final Class classToRender, final ObjectRenderer or) {
        rendererMap.put(classToRender, or);
    }

    /**
     * This call will clear all logger definitions from the internal hashtable. Invoking this method will irrevocably mess
     * up the logger hierarchy.
     *
     * <p>
     * You should <em>really</em> know what you are doing before invoking this method.
     * </p>
     *
     * @since 0.9.0
     */
    public void clear() {
        // System.out.println("\n\nAbout to clear internal hash table.");
        ht.clear();
        getLoggersMap(getContext()).clear();
    }

    @Override
    public void emitNoAppenderWarning(final Category cat) {
        // No appenders in hierarchy, warn user only once.
        if (!this.emittedNoAppenderWarning) {
            LogLog.warn("No appenders could be found for logger (" + cat.getName() + ").");
            LogLog.warn("Please initialize the log4j system properly.");
            LogLog.warn("See http://logging.apache.org/log4j/1.2/faq.html#noconfig for more info.");
            this.emittedNoAppenderWarning = true;
        }
    }

    /**
     * Tests if the named logger exists in the hierarchy. If so return its reference, otherwise returns <code>null</code>.
     *
     * @param name The name of the logger to search for.
     *
     */
    @Override
    public Logger exists(final String name) {
        return exists(name, getContext());
    }

    Logger exists(final String name, final ClassLoader classLoader) {
        return exists(name, getContext(classLoader));
    }

    Logger exists(final String name, final LoggerContext loggerContext) {
        if (!loggerContext.hasLogger(name)) {
            return null;
        }
        return Logger.getLogger(name);
    }

    @Override
    public void fireAddAppenderEvent(final Category logger, final Appender appender) {
        if (listeners != null) {
            final int size = listeners.size();
            HierarchyEventListener listener;
            for (int i = 0; i < size; i++) {
                listener = (HierarchyEventListener) listeners.elementAt(i);
                listener.addAppenderEvent(logger, appender);
            }
        }
    }

    void fireRemoveAppenderEvent(final Category logger, final Appender appender) {
        if (listeners != null) {
            final int size = listeners.size();
            HierarchyEventListener listener;
            for (int i = 0; i < size; i++) {
                listener = (HierarchyEventListener) listeners.elementAt(i);
                listener.removeAppenderEvent(logger, appender);
            }
        }
    }

    LoggerContext getContext(final ClassLoader classLoader) {
        return LogManager.getContext(classLoader);
    }

    /**
     * @deprecated Please use {@link #getCurrentLoggers} instead.
     */
    @Deprecated
    @Override
    public Enumeration getCurrentCategories() {
        return getCurrentLoggers();
    }

    /**
     * Gets all the currently defined categories in this hierarchy as an {@link java.util.Enumeration Enumeration}.
     *
     * <p>
     * The root logger is <em>not</em> included in the returned {@link Enumeration}.
     * </p>
     */
    @Override
    public Enumeration getCurrentLoggers() {
        // The accumlation in v is necessary because not all elements in
        // ht are Logger objects as there might be some ProvisionNodes
        // as well.
        //        final Vector v = new Vector(ht.size());
        //
        //        final Enumeration elems = ht.elements();
        //        while (elems.hasMoreElements()) {
        //            final Object o = elems.nextElement();
        //            if (o instanceof Logger) {
        //                v.addElement(o);
        //            }
        //        }
        //        return v.elements();

        return LogManager.getCurrentLoggers(StackLocatorUtil.getCallerClassLoader(2));
    }

    /**
     * Gets a new logger instance named as the first parameter using the default factory.
     *
     * <p>
     * If a logger of that name already exists, then it will be returned. Otherwise, a new logger will be instantiated and
     * then linked with its existing ancestors as well as children.
     * </p>
     *
     * @param name The name of the logger to retrieve.
     *
     */
    @Override
    public Logger getLogger(final String name) {
        return getInstance(getContext(), name);
    }

    Logger getLogger(final String name, final ClassLoader classLoader) {
        return getInstance(getContext(classLoader), name);
    }

    /**
     * Gets a new logger instance named as the first parameter using <code>factory</code>.
     *
     * <p>
     * If a logger of that name already exists, then it will be returned. Otherwise, a new logger will be instantiated by
     * the <code>factory</code> parameter and linked with its existing ancestors as well as children.
     * </p>
     *
     * @param name The name of the logger to retrieve.
     * @param factory The factory that will make the new logger instance.
     *
     */
    @Override
    public Logger getLogger(final String name, final LoggerFactory factory) {
        return getInstance(getContext(), name, factory);
    }

    Logger getLogger(final String name, final LoggerFactory factory, final ClassLoader classLoader) {
        return getInstance(getContext(classLoader), name, factory);
    }

    /**
     * Gets the renderer map for this hierarchy.
     */
    @Override
    public RendererMap getRendererMap() {
        return rendererMap;
    }

    /**
     * Gets the root of this hierarchy.
     *
     * @since 0.9.0
     */
    @Override
    public Logger getRootLogger() {
        return getInstance(getContext(), org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME);
    }

    Logger getRootLogger(final ClassLoader classLoader) {
        return getInstance(getContext(classLoader), org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME);
    }

    /**
     * Gets a {@link Level} representation of the <code>enable</code> state.
     *
     * @since 1.2
     */
    @Override
    public Level getThreshold() {
        return threshold;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ThrowableRenderer getThrowableRenderer() {
        return throwableRenderer;
    }

    /**
     * This method will return <code>true</code> if this repository is disabled for <code>level</code> object passed as
     * parameter and <code>false</code> otherwise. See also the {@link #setThreshold(Level) threshold} emthod.
     */
    @Override
    public boolean isDisabled(final int level) {
        return thresholdInt > level;
    }

    /**
     * @deprecated Deprecated with no replacement.
     */
    @Deprecated
    public void overrideAsNeeded(final String override) {
        LogLog.warn("The Hiearchy.overrideAsNeeded method has been deprecated.");
    }

    /**
     * Resets all values contained in this hierarchy instance to their default. This removes all appenders from all
     * categories, sets the level of all non-root categories to <code>null</code>, sets their additivity flag to
     * <code>true</code> and sets the level of the root logger to {@link Level#DEBUG DEBUG}. Moreover, message disabling is
     * set its default "off" value.
     *
     * <p>
     * Existing categories are not removed. They are just reset.
     * </p>
     *
     * <p>
     * This method should be used sparingly and with care as it will block all logging until it is completed.
     * </p>
     *
     * @since 0.8.5
     */
    @Override
    public void resetConfiguration() {
        resetConfiguration(getContext());
    }

    void resetConfiguration(final ClassLoader classLoader) {
        resetConfiguration(getContext(classLoader));
    }

    void resetConfiguration(final LoggerContext loggerContext) {
        getLoggersMap(loggerContext).clear();

        getRootLogger().setLevel(Level.DEBUG);
        root.setResourceBundle(null);
        setThreshold(Level.ALL);

        // the synchronization is needed to prevent JDK 1.2.x hashtable
        // surprises
        synchronized (ht) {
            shutdown(); // nested locks are OK

            final Enumeration cats = getCurrentLoggers();
            while (cats.hasMoreElements()) {
                final Logger c = (Logger) cats.nextElement();
                c.setLevel(null);
                c.setAdditivity(true);
                c.setResourceBundle(null);
            }
        }
        rendererMap.clear();
        throwableRenderer = null;
    }

    /**
     * Does nothing.
     *
     * @deprecated Deprecated with no replacement.
     */
    @Deprecated
    public void setDisableOverride(final String override) {
        LogLog.warn("The Hiearchy.setDisableOverride method has been deprecated.");
    }

    /**
     * Used by subclasses to add a renderer to the hierarchy passed as parameter.
     */
    @Override
    public void setRenderer(final Class renderedClass, final ObjectRenderer renderer) {
        rendererMap.put(renderedClass, renderer);
    }

    /**
     * Enable logging for logging requests with level <code>l</code> or higher. By default all levels are enabled.
     *
     * @param level The minimum level for which logging requests are sent to their appenders.
     */
    @Override
    public void setThreshold(final Level level) {
        if (level != null) {
            thresholdInt = level.level;
            threshold = level;
        }
    }

    /**
     * The string form of {@link #setThreshold(Level)}.
     */
    @Override
    public void setThreshold(final String levelStr) {
        final Level level = OptionConverter.toLevel(levelStr, null);
        if (level != null) {
            setThreshold(level);
        } else {
            LogLog.warn("Could not convert [" + levelStr + "] to Level.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setThrowableRenderer(final ThrowableRenderer throwableRenderer) {
        this.throwableRenderer = throwableRenderer;
    }

    /**
     * Shutting down a hierarchy will <em>safely</em> close and remove all appenders in all categories including the root
     * logger.
     *
     * <p>
     * Some appenders such as {@link org.apache.log4j.net.SocketAppender} and {@link AsyncAppender} need to be closed before
     * the application exists. Otherwise, pending logging events might be lost.
     * </p>
     * <p>
     * The <code>shutdown</code> method is careful to close nested appenders before closing regular appenders. This is
     * allows configurations where a regular appender is attached to a logger and again to a nested appender.
     * </p>
     *
     * @since 1.0
     */
    @Override
    public void shutdown() {
        shutdown(getContext());
    }

    public void shutdown(final ClassLoader classLoader) {
        shutdown(org.apache.logging.log4j.LogManager.getContext(classLoader, false));
    }

    void shutdown(final LoggerContext context) {
        //      final Logger root = getRootLogger();
        //      // begin by closing nested appenders
        //      root.closeNestedAppenders();
        //
        //      synchronized (ht) {
        //          Enumeration cats = this.getCurrentLoggers();
        //          while (cats.hasMoreElements()) {
        //              final Logger c = (Logger) cats.nextElement();
        //              c.closeNestedAppenders();
        //          }
        //
        //          // then, remove all appenders
        //          root.removeAllAppenders();
        //          cats = this.getCurrentLoggers();
        //          while (cats.hasMoreElements()) {
        //              final Logger c = (Logger) cats.nextElement();
        //              c.removeAllAppenders();
        //          }
        //      }
        getLoggersMap(context).clear();
        if (LogManager.isLog4jCorePresent()) {
            ContextUtil.shutdown(context);
        }
    }
}
