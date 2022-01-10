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

    static private RepositorySelector repositorySelector;

    private static final boolean LOG4J_CORE_PRESENT;

    static {
        LOG4J_CORE_PRESENT = checkLog4jCore();
        //
        // By default we use a DefaultRepositorySelector which always returns 'hierarchy'.
        Hierarchy hierarchy = new Hierarchy(new RootLogger(Level.DEBUG));
        repositorySelector = new DefaultRepositorySelector(hierarchy);
    }

    private static boolean checkLog4jCore() {
        try {
            return Class.forName("org.apache.logging.log4j.core.LoggerContext") != null;
        } catch (Exception ex) {
            return false;
        }
    }

    public static Logger exists(final String name) {
        return getLoggerRepository().exists(name);
    }

    /**
     * Gets a LoggerContext.
     *
     * @param loader The ClassLoader for the context. If null the context will attempt to determine the appropriate
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
        return Collections.enumeration(
            LogManager.getContext(classLoader).getLoggerRegistry()
                .getLoggers().stream().map(e -> LogManager.getLogger(e.getName(), classLoader))
                .collect(Collectors.toList()));
        // @formatter:on
    }

    public static Logger getLogger(final Class<?> clazz) {
        // Depth 2 gets the call site of this method.
        return getLoggerRepository2().getLogger(clazz.getName(), StackLocatorUtil.getCallerClassLoader(2));
    }

    public static Logger getLogger(final String name) {
        // Depth 2 gets the call site of this method.
        return getLoggerRepository2().getLogger(name, StackLocatorUtil.getCallerClassLoader(2));
    }

    static Logger getLogger(final String name, final ClassLoader classLoader) {
        return getLoggerRepository2().getLogger(name, classLoader);
    }

    public static Logger getLogger(final String name, final LoggerFactory factory) {
        // Depth 2 gets the call site of this method.
        return getLoggerRepository2().getLogger(name, factory, StackLocatorUtil.getCallerClassLoader(2));
    }

    static Logger getLogger(final String name, final LoggerFactory factory, final ClassLoader classLoader) {
        return getLoggerRepository2().getLogger(name, factory, classLoader);
    }

    public static LoggerRepository getLoggerRepository() {
        if (repositorySelector == null) {
            repositorySelector = new DefaultRepositorySelector(new NOPLoggerRepository());
        }
        return repositorySelector.getLoggerRepository();
    }

    static LoggerRepository2 getLoggerRepository2() {
        // TODO Hack
        return (LoggerRepository2) getLoggerRepository();
    }

    public static Logger getRootLogger() {
        return getLoggerRepository2().getRootLogger();
    }

    static boolean isLog4jCorePresent() {
        return LOG4J_CORE_PRESENT;
    }

    static void reconfigure() {
        if (isLog4jCorePresent()) {
            ContextUtil.reconfigure(Hierarchy.getContext());
        }
    }

    /**
     * No-op implementation.
     */
    public static void resetConfiguration() {
        // noop
    }

    /**
     * No-op implementation.
     * 
     * @param selector The RepositorySelector.
     * @param guard prevents calls at the incorrect time.
     * @throws IllegalArgumentException if a parameter is invalid.
     */
    public static void setRepositorySelector(final RepositorySelector selector, final Object guard) throws IllegalArgumentException {
        // noop
    }

    /**
     * No-op implementation.
     */
    public static void shutdown() {
        // noop
    }

}
