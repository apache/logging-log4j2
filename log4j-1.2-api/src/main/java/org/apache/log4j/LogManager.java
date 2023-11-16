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

import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Collectors;
import org.apache.log4j.legacy.core.ContextUtil;
import org.apache.log4j.spi.DefaultRepositorySelector;
import org.apache.log4j.spi.LoggerFactory;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.NOPLoggerRepository;
import org.apache.log4j.spi.RepositorySelector;
import org.apache.log4j.spi.RootLogger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.util.StackLocatorUtil;

/**
 * The main entry point to Log4j 1.
 */
public final class LogManager {

    /**
     * @deprecated This variable is for internal use only. It will become package protected in future versions.
     */
    @Deprecated
    public static final String DEFAULT_CONFIGURATION_FILE = "log4j.properties";

    /**
     * @deprecated This variable is for internal use only. It will become private in future versions.
     */
    @Deprecated
    public static final String DEFAULT_CONFIGURATION_KEY = "log4j.configuration";

    /**
     * @deprecated This variable is for internal use only. It will become private in future versions.
     */
    @Deprecated
    public static final String CONFIGURATOR_CLASS_KEY = "log4j.configuratorClass";

    /**
     * @deprecated This variable is for internal use only. It will become private in future versions.
     */
    @Deprecated
    public static final String DEFAULT_INIT_OVERRIDE_KEY = "log4j.defaultInitOverride";

    static final String DEFAULT_XML_CONFIGURATION_FILE = "log4j.xml";

    private static RepositorySelector repositorySelector;

    private static final boolean LOG4J_CORE_PRESENT;

    static {
        LOG4J_CORE_PRESENT = checkLog4jCore();
        // By default, we use a DefaultRepositorySelector which always returns 'hierarchy'.
        final Hierarchy hierarchy = new Hierarchy(new RootLogger(Level.DEBUG));
        repositorySelector = new DefaultRepositorySelector(hierarchy);
    }

    private static boolean checkLog4jCore() {
        try {
            return Class.forName("org.apache.logging.log4j.core.LoggerContext") != null;
        } catch (final Throwable ex) {
            return false;
        }
    }

    /**
     * Tests if a logger for the given name exists.
     *
     * @param name logger name to test.
     * @return whether a logger for the given name exists.
     */
    public static Logger exists(final String name) {
        return exists(name, StackLocatorUtil.getCallerClassLoader(2));
    }

    static Logger exists(final String name, final ClassLoader classLoader) {
        return getHierarchy().exists(name, classLoader);
    }

    /**
     * Gets a LoggerContext.
     *
     * @param classLoader The ClassLoader for the context. If null the context will attempt to determine the appropriate
     *        ClassLoader.
     * @return a LoggerContext.
     */
    static LoggerContext getContext(final ClassLoader classLoader) {
        return org.apache.logging.log4j.LogManager.getContext(classLoader, false);
    }

    /**
     * Gets an enumeration of the current loggers.
     *
     * @return an enumeration of the current loggers.
     */
    @SuppressWarnings("rawtypes")
    public static Enumeration getCurrentLoggers() {
        return getCurrentLoggers(StackLocatorUtil.getCallerClassLoader(2));
    }

    @SuppressWarnings("rawtypes")
    static Enumeration getCurrentLoggers(final ClassLoader classLoader) {
        // @formatter:off
        return Collections.enumeration(LogManager.getContext(classLoader).getLoggerRegistry().getLoggers().stream()
                .map(e -> LogManager.getLogger(e.getName(), classLoader))
                .collect(Collectors.toList()));
        // @formatter:on
    }

    static Hierarchy getHierarchy() {
        final LoggerRepository loggerRepository = getLoggerRepository();
        return loggerRepository instanceof Hierarchy ? (Hierarchy) loggerRepository : null;
    }

    /**
     * Gets the logger for the given class.
     */
    public static Logger getLogger(final Class<?> clazz) {
        final Hierarchy hierarchy = getHierarchy();
        return hierarchy != null
                ? hierarchy.getLogger(clazz.getName(), StackLocatorUtil.getCallerClassLoader(2))
                : getLoggerRepository().getLogger(clazz.getName());
    }

    /**
     * Gets the logger for the given name.
     */
    public static Logger getLogger(final String name) {
        final Hierarchy hierarchy = getHierarchy();
        return hierarchy != null
                ? hierarchy.getLogger(name, StackLocatorUtil.getCallerClassLoader(2))
                : getLoggerRepository().getLogger(name);
    }

    static Logger getLogger(final String name, final ClassLoader classLoader) {
        final Hierarchy hierarchy = getHierarchy();
        return hierarchy != null
                ? hierarchy.getLogger(name, classLoader)
                : getLoggerRepository().getLogger(name);
    }

    public static Logger getLogger(final String name, final LoggerFactory factory) {
        final Hierarchy hierarchy = getHierarchy();
        return hierarchy != null
                ? hierarchy.getLogger(name, factory, StackLocatorUtil.getCallerClassLoader(2))
                : getLoggerRepository().getLogger(name, factory);
    }

    static Logger getLogger(final String name, final LoggerFactory factory, final ClassLoader classLoader) {
        final Hierarchy hierarchy = getHierarchy();
        return hierarchy != null
                ? hierarchy.getLogger(name, factory, classLoader)
                : getLoggerRepository().getLogger(name, factory);
    }

    public static LoggerRepository getLoggerRepository() {
        if (repositorySelector == null) {
            repositorySelector = new DefaultRepositorySelector(new NOPLoggerRepository());
        }
        return repositorySelector.getLoggerRepository();
    }

    /**
     * Gets the root logger.
     */
    public static Logger getRootLogger() {
        return getRootLogger(StackLocatorUtil.getCallerClassLoader(2));
    }

    static Logger getRootLogger(final ClassLoader classLoader) {
        final Hierarchy hierarchy = getHierarchy();
        return hierarchy != null
                ? hierarchy.getRootLogger(classLoader)
                : getLoggerRepository().getRootLogger();
    }

    static boolean isLog4jCorePresent() {
        return LOG4J_CORE_PRESENT;
    }

    static void reconfigure(final ClassLoader classLoader) {
        if (isLog4jCorePresent()) {
            ContextUtil.reconfigure(LogManager.getContext(classLoader));
        }
    }

    public static void resetConfiguration() {
        resetConfiguration(StackLocatorUtil.getCallerClassLoader(2));
    }

    static void resetConfiguration(final ClassLoader classLoader) {
        final Hierarchy hierarchy = getHierarchy();
        if (hierarchy != null) {
            hierarchy.resetConfiguration(classLoader);
        } else {
            getLoggerRepository().resetConfiguration();
        }
    }

    public static void setRepositorySelector(final RepositorySelector selector, final Object guard)
            throws IllegalArgumentException {
        if (selector == null) {
            throw new IllegalArgumentException("RepositorySelector must be non-null.");
        }
        LogManager.repositorySelector = selector;
    }

    /**
     * Shuts down the current configuration.
     */
    public static void shutdown() {
        shutdown(StackLocatorUtil.getCallerClassLoader(2));
    }

    static void shutdown(final ClassLoader classLoader) {
        final Hierarchy hierarchy = getHierarchy();
        if (hierarchy != null) {
            hierarchy.shutdown(classLoader);
        } else {
            getLoggerRepository().shutdown();
        }
    }
}
