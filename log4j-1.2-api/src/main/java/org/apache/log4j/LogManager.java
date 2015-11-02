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

import org.apache.log4j.helpers.NullEnumeration;
import org.apache.log4j.spi.HierarchyEventListener;
import org.apache.log4j.spi.LoggerFactory;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RepositorySelector;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.util.Strings;

/**
 *
 */
public final class LogManager {

    /**
     * @deprecated This variable is for internal use only. It will
     * become package protected in future versions.
     * */
    @Deprecated
    public static final String DEFAULT_CONFIGURATION_FILE = "log4j.properties";

    /**
     * @deprecated This variable is for internal use only. It will
     * become private in future versions.
     * */
    @Deprecated
    public static final String DEFAULT_CONFIGURATION_KEY = "log4j.configuration";

    /**
     * @deprecated This variable is for internal use only. It will
     * become private in future versions.
     * */
    @Deprecated
    public static final String CONFIGURATOR_CLASS_KEY = "log4j.configuratorClass";

    /**
     * @deprecated This variable is for internal use only. It will
     * become private in future versions.
     */
    @Deprecated
    public static final String DEFAULT_INIT_OVERRIDE_KEY = "log4j.defaultInitOverride";

    static final String DEFAULT_XML_CONFIGURATION_FILE = "log4j.xml";

    private static final LoggerRepository REPOSITORY = new Repository();

    private LogManager() {
    }

    public static Logger getRootLogger() {
        return Category.getInstance(PrivateManager.getContext(), Strings.EMPTY);
    }

    public static Logger getLogger(final String name) {
        return Category.getInstance(PrivateManager.getContext(), name);
    }

    public static Logger getLogger(final Class<?> clazz) {
        return Category.getInstance(PrivateManager.getContext(), clazz.getName());
    }

    public static Logger getLogger(final String name, final LoggerFactory factory) {
        return Category.getInstance(PrivateManager.getContext(), name);
    }

    public static Logger exists(final String name) {
        final LoggerContext ctx = PrivateManager.getContext();
        if (!ctx.hasLogger(name)) {
            return null;
        }
        return Logger.getLogger(name);
    }

    @SuppressWarnings("rawtypes")
    public static Enumeration getCurrentLoggers() {
        return NullEnumeration.getInstance();
    }

    static void reconfigure() {
        final LoggerContext ctx = PrivateManager.getContext();
        ctx.reconfigure();
    }

    /**
     * No-op implementation.
     */
    public static void shutdown() {
    }

    /**
     * No-op implementation.
     */
    public static void resetConfiguration() {
    }

    /**
     * No-op implementation.
     * @param selector The RepositorySelector.
     * @param guard prevents calls at the incorrect time.
     * @throws IllegalArgumentException if a parameter is invalid.
     */
    public static void setRepositorySelector(final RepositorySelector selector, final Object guard)
        throws IllegalArgumentException {
    }

    public static LoggerRepository getLoggerRepository() {
        return REPOSITORY;
    }

    /**
     * The Repository.
     */
    private static class Repository implements LoggerRepository {
        @Override
        public void addHierarchyEventListener(final HierarchyEventListener listener) {

        }

        @Override
        public boolean isDisabled(final int level) {
            return false;
        }

        @Override
        public void setThreshold(final Level level) {

        }

        @Override
        public void setThreshold(final String val) {

        }

        @Override
        public void emitNoAppenderWarning(final Category cat) {

        }

        @Override
        public Level getThreshold() {
            return Level.OFF;
        }

        @Override
        public Logger getLogger(final String name) {
            return Category.getInstance(PrivateManager.getContext(), name);
        }

        @Override
        public Logger getLogger(final String name, final LoggerFactory factory) {
            return Category.getInstance(PrivateManager.getContext(), name);
        }

        @Override
        public Logger getRootLogger() {
            return Category.getRoot(PrivateManager.getContext());
        }

        @Override
        public Logger exists(final String name) {
            return LogManager.exists(name);
        }

        @Override
        public void shutdown() {
        }

        @Override
        @SuppressWarnings("rawtypes")
        public Enumeration getCurrentLoggers() {
            return NullEnumeration.getInstance();
        }

        @Override
        @SuppressWarnings("rawtypes")
        public Enumeration getCurrentCategories() {
            return NullEnumeration.getInstance();
        }

        @Override
        public void fireAddAppenderEvent(final Category logger, final Appender appender) {
        }

        @Override
        public void resetConfiguration() {
        }
    }

    /**
     * Internal LogManager.
     */
    private static class PrivateManager extends org.apache.logging.log4j.LogManager {
        private static final String FQCN = LogManager.class.getName();

        public static LoggerContext getContext() {
            return (LoggerContext) getContext(FQCN, false);
        }

        public static org.apache.logging.log4j.Logger getLogger(final String name) {
            return getLogger(FQCN, name);
        }
    }
}
