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
package org.apache.logging.log4j.spi;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.message.FlowMessageFactory;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.util.EnvironmentPropertySource;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertyKey;
import org.apache.logging.log4j.util.Unbox;

/**
 * Centralized list of property name constants that can be configured in the Log4j API. These properties may be
 * specified as system properties, environment variables (see {@link EnvironmentPropertySource}), or in a classpath
 * resource file named {@code log4j2.component.properties}.
 *
 * @since 3.0.0
 * @see PropertiesUtil
 */
public enum LoggingSystemProperty implements PropertyKey {

    /**
     * Property to override the maximum size of the StringBuilder instances used in ringbuffer log events to store
     * the contents of reusable messages. After delivering messages above this size to appenders, the StringBuilder
     * is trimmed to this maximum size. The default value is 518 which allows the StringBuilder to resize three times
     * from its initial size.
     */
    // GC.maxReusableMsgSize
    GC_REUSABLE_MESSAGE_MAX_SIZE(PropertyComponent.GC, Constant.MAX_REUSABLE_MSG_SIZE),

    /**
     * Property to ignore the thread context ClassLoader when set to {@code true}.
     *
     * @see LoaderUtil
     */
    // TODO: see if this can be removed
    LOADER_IGNORE_THREAD_CONTEXT_LOADER(PropertyComponent.LOADER, Constant.IGNORE_TCL),

    /**
     * Property to force use of the thread context ClassLoader.
     *
     * @see LoaderUtil
     */
    // TODO: see if this can be removed
    LOADER_FORCE_THREAD_CONTEXT_LOADER(PropertyComponent.LOADER, Constant.FORCE_TCL_ONLY),

    /**
     * Property to set to the fully qualified class name of a custom implementation of {@link LoggerContextFactory}.
     */
    // LoggerContext.factory
    LOGGER_CONTEXT_FACTORY_CLASS(PropertyComponent.LOGGER_CONTEXT, Constant.FACTORY),

    /**
     * Property to override the default {@link MessageFactory} class.
     */
    // Message.messageFactory
    LOGGER_MESSAGE_FACTORY_CLASS(PropertyComponent.MESSAGE, Constant.FACTORY),

    /**
     * Property to override the default {@link FlowMessageFactory} class.
     */
    // Message.flowMessageFactory
    LOGGER_FLOW_MESSAGE_FACTORY_CLASS(PropertyComponent.MESSAGE, Constant.FLOW_MESSAGE_FACTORY),

    /**
     * Property to override the default maximum nesting depth of map messages to format in JSON output. The
     * default value is 8.
     */
    // Message.jsonFormatterMaxDepth
    LOGGER_MAP_MESSAGE_JSON_FORMATTER_MAX_DEPTH(PropertyComponent.MESSAGE, Constant.JSON_FORMATTER_MAX_DEPTH),

    SIMPLE_SHOW_CONTEXT_MAP(PropertyComponent.SIMPLE_LOGGER, Constant.SHOW_CONTEXT_MAP),
    SIMPLE_SHOW_LOG_NAME(PropertyComponent.SIMPLE_LOGGER, Constant.SHOW_LOG_NAME),
    SIMPLE_SHOW_SHORT_LOG_NAME(PropertyComponent.SIMPLE_LOGGER, Constant.SHOW_SHORT_LOG_NAME),
    SIMPLE_SHOW_DATE_TIME(PropertyComponent.SIMPLE_LOGGER, Constant.SHOW_DATE_TIME),
    SIMPLE_DATE_TIME_FORMAT(PropertyComponent.SIMPLE_LOGGER, Constant.DATE_TIME_FORMAT),
    SIMPLE_LOG_FILE(PropertyComponent.SIMPLE_LOGGER, Constant.LOG_FILE),
    SIMPLE_LOG_LEVEL(PropertyComponent.SIMPLE_LOGGER, Constant.LOG_LEVEL),

    /**
     * Property that can be configured with the maximum number of status data entries to keep queued. Once the limit is
     * reached, older entries will be removed as new entries are added. The default value is 200.
     */
    // StatusLogger.entries
    STATUS_MAX_ENTRIES(PropertyComponent.STATUS_LOGGER, Constant.ENTRIES),

    /**
     * Property that can be configured with the {@link Level} name to use as the default level for
     * {@link StatusListener}s. The default value is {@link Level#WARN}.
     */
    // StatusLogger.statusLoggerLevel
    STATUS_DEFAULT_LISTENER_LEVEL(PropertyComponent.STATUS_LOGGER, Constant.LISTENER_LEVEL),

    /**
     * Property that can be configured with a date-time format string to use as the format for timestamps
     * in the status logger output. See {@link java.text.SimpleDateFormat} for supported formats.
     */
    // StatusLogger.dateFormat
    STATUS_DATE_FORMAT(PropertyComponent.STATUS_LOGGER, Constant.DATE_TIME_FORMAT),

    /**
     * Property to enable TRACE-level debug logging in the Log4j system itself.
     * <p>
     * If the property is either defined empty or its value equals to {@code true} (ignoring case), all internal
     * logging will be printed to the console. The presence of this system property overrides any value set in the
     * configuration's {@code <Configuration status="<level>" ...>} status attribute.
     * </p>
     */
    STATUS_LOGGER_DEBUG(PropertyComponent.STATUS_LOGGER, Constant.DEBUG),

    /**
     * Property to control whether {@link ThreadContext} stores map data. If set to {@code true}, then the
     * thread context map will be disabled.
     */
    // ThreadContext.enableMap
    THREAD_CONTEXT_MAP_ENABLED(PropertyComponent.THREAD_CONTEXT, Constant.ENABLE_MAP),

    /**
     * Property to control whether {@link ThreadContext} stores stack data. If set to {@code true}, then the
     * thread context stack will be disabled.
     */
    // ThreadContext.enableStack
    THREAD_CONTEXT_STACK_ENABLED(PropertyComponent.THREAD_CONTEXT, Constant.ENABLE_STACK),

    /**
     * Property to control whether {@link ThreadContext} stores any data. If set to {@code true}, then the
     * thread context map and stack will be disabled.
     */
    // ThreadContext.enable
    THREAD_CONTEXT_ENABLE(PropertyComponent.THREAD_CONTEXT, Constant.ENABLE),

    /**
     * Property to control whether {@link ThreadContextMap} uses {@link InheritableThreadLocal} when {@code true} or
     * {@link ThreadLocal} otherwise for holding map data.
     */
    // ThreadContext.mapInheritable
    THREAD_CONTEXT_MAP_INHERITABLE(PropertyComponent.THREAD_CONTEXT, Constant.MAP_INHERITABLE),

    /**
     * Property to override the default {@link ThreadContextMap} class. Note that implementation classes should
     * also implement {@link ReadOnlyThreadContextMap} if they should be accessible to applications via
     * {@link ThreadContext#getThreadContextMap()}.
     */
    // TODO: replace with LoggingSystem overrides
    THREAD_CONTEXT_MAP_CLASS(PropertyComponent.THREAD_CONTEXT, Constant.MAP_CLASS),

    /**
     * Property to override the initial capacity of the thread context map. The default value is 16.
     */
    // ThreadContext.initialCapacity
    THREAD_CONTEXT_INITIAL_CAPACITY(PropertyComponent.THREAD_CONTEXT, Constant.INITIAL_CAPACITY),

    /**
     * Property to override whether to use a garbage-free implementation of {@link ThreadContextMap}.
     */
    // ThreadContext.garbageFree
    THREAD_CONTEXT_GARBAGE_FREE_ENABLED(PropertyComponent.THREAD_CONTEXT, Constant.GARBAGE_FREE),
    /**
     * Property to override the default ringbuffer size used in {@link Unbox}. The default value is 32.
     */
    // GC.unboxRingBufferSize
    UNBOX_RING_BUFFER_SIZE(PropertyComponent.GC, Constant.UNBOX_RING_BUFFER_SIZE),
    /**
     * Property to override webapp detection. Without this property, the presence of the {@code Servlet} interface
     * (from either {@code javax} or {@code jakarta}) is checked to see if this is a webapp.
     */
    // Web.isWebApp : calculate | true | false
    IS_WEBAPP(PropertyComponent.WEB, Constant.IS_WEB_APP),
    /**
     * Property to override the default recycler factory.
     */
    RECYCLER_FACTORY(PropertyComponent.RECYCLER, Constant.RECYCLER_FACTORY),
    /**
     * Property to override the default recycler capacity.
     */
    RECYCLER_CAPACITY(PropertyComponent.RECYCLER, Constant.RECYCLER_CAPACITY);

    public static final String SIMPLE_LOGGER_LOG_LEVEL = "SimpleLogger.%s.level";
    public static final String SYSTEM_PROPERTY_PREFIX = "log4j2.*.";

    private final PropertyComponent component;
    private final String name;

    private final String key;
    private final String systemKey;

    LoggingSystemProperty(final PropertyComponent component, final String name) {
        this.component = component;
        this.name = name;
        this.key = component.getName() + Constant.DELIM + name;
        this.systemKey = SYSTEM_PROPERTY_PREFIX + this.key;
    }

    public static LoggingSystemProperty findKey(final String component, final String name) {
        final String toFind = component + Constant.DELIM + name;
        for (LoggingSystemProperty property : values()) {
            if (property.key.equals(toFind)) {
                return property;
            }
        }
        return null;
    }

    @Override
    public String getComponent() {
        return component.getName();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getSystemKey() {
        return systemKey;
    }

    @Override
    public String toString() {
        return getKey();
    }

    public static class Constant {
        private static final String DELIM = ".";
        /**
         * Property to enable TRACE-level debug logging in the Log4j system itself.
         * <p>
         * If property {@value} is either defined empty or its value equals to {@code true} (ignoring case), all internal
         * logging will be printed to the console. The presence of this system property overrides any value set in the
         * configuration's {@code <Configuration status="<level>" ...>} status attribute.
         * </p>
         */
        static final String DEBUG = "debug";

        public static String SYSTEM_DEBUG = SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.SYSTEM + DELIM + DEBUG;

        /**
         * Property to override webapp detection. Without this property, the presence of the {@code Servlet} interface
         * (from either {@code javax} or {@code jakarta}) is checked to see if this is a webapp.
         */
        // Web.enableWebApp : calculate | true | false
        static final String IS_WEBAPP = "isWebApp";

        public static final String WEB_IS_WEBAPP =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.WEB + DELIM + IS_WEBAPP;

        /**
         * Property to override the use of thread-local values for garbage-free logging.
         *
         * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-1270">LOG4J2-1270</a>
         */
        // ThreadContext.enabled : calculate | true | false
        static final String ENABLE = "enable";

        /**
         * Property to ignore the thread context ClassLoader when set to {@code true}.
         *
         * @see LoaderUtil
         */
        // TODO: see if this can be removed
        static final String IGNORE_TCL = "ignoreTCL";

        public static final String LOADER_IGNORE_THREAD_CONTEXT_LOADER =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.LOADER + DELIM + IGNORE_TCL;

        /**
         * Property to force use of the thread context ClassLoader.
         *
         * @see LoaderUtil
         */
        // TODO: see if this can be removed
        static final String FORCE_TCL_ONLY = "forceTCLOnly";

        public static final String LOADER_FORCE_THREAD_CONTEXT_LOADER =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.LOADER + DELIM + FORCE_TCL_ONLY;

        /**
         * Property to override the default ringbuffer size used in {@link Unbox}. The default value is 32.
         */
        // GC.unboxRingBufferSize
        static final String UNBOX_RING_BUFFER_SIZE = "unboxRingBufferSize";

        public static final String GC_UNBOX_RING_BUFFER_SIZE =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.GC + DELIM + UNBOX_RING_BUFFER_SIZE;

        /**
         * Property to set to the fully qualified class name of a custom implementation of {@link LoggerContextFactory}.
         */
        // LoggerContext.factory
        static final String FACTORY = "factory";

        public static final String LOGGER_CONTEXT_FACTORY_CLASS =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.LOGGER_CONTEXT + DELIM + FACTORY;

        /**
         * Property to override the default {@link MessageFactory} class.
         */
        // Message.messageFactory
        public static final String LOGGER_MESSAGE_FACTORY_CLASS =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.MESSAGE + DELIM + FACTORY;

        /**
         * Property to override the default {@link FlowMessageFactory} class.
         */
        // Message.flowMessageFactory
        static final String FLOW_MESSAGE_FACTORY = "flowMessageFactory";

        public static final String LOGGER_FLOW_MESSAGE_FACTORY_CLASS =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.MESSAGE + DELIM + FLOW_MESSAGE_FACTORY;

        /**
         * Property to override the default maximum nesting depth of map messages to format in JSON output. The
         * default value is 8.
         */
        // Message.jsonFormatterMaxDepth
        static final String JSON_FORMATTER_MAX_DEPTH = "jsonFormatterMaxDepth";

        public static final String LOGGER_MAP_MESSAGE_JSON_FORMATTER_MAX_DEPTH =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.MESSAGE + DELIM + JSON_FORMATTER_MAX_DEPTH;

        /**
         * Property to override the maximum size of the StringBuilder instances used in ringbuffer log events to store
         * the contents of reusable messages. After delivering messages above this size to appenders, the StringBuilder
         * is trimmed to this maximum size. The default value is 518 which allows the StringBuilder to resize three times
         * from its initial size.
         */
        // GC.maxReusableMsgSize
        static final String MAX_REUSABLE_MSG_SIZE = "maxReusableMsgSize";

        public static final String GC_REUSABLE_MESSAGE_MAX_SIZE =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.GC + DELIM + MAX_REUSABLE_MSG_SIZE;

        static final String SHOW_CONTEXT_MAP = "showContextMap";
        public static final String SIMPLE_SHOW_CONTEXT_MAP =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.SIMPLE_LOGGER + DELIM + SHOW_CONTEXT_MAP;
        static final String SHOW_LOG_NAME = "showLogName";
        public static final String SIMPLE_SHOW_LOG_NAME =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.SIMPLE_LOGGER + DELIM + SHOW_LOG_NAME;
        static final String SHOW_SHORT_LOG_NAME = "showShortLogName";
        public static final String SIMPLE_SHOW_SHORT_LOG_NAME =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.SIMPLE_LOGGER + DELIM + SHOW_SHORT_LOG_NAME;
        static final String SHOW_DATE_TIME = "showDateTime";
        public static final String SIMPLE_SHOW_DATE_TIME =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.SIMPLE_LOGGER + DELIM + SHOW_DATE_TIME;
        static final String DATE_TIME_FORMAT = "dateTimeFormat";
        public static final String SIMPLE_DATE_TIME_FORMAT =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.SIMPLE_LOGGER + DELIM + DATE_TIME_FORMAT;
        static final String LOG_FILE = "logFile";
        public static final String SIMPLE_LOG_FILE =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.SIMPLE_LOGGER + DELIM + LOG_FILE;
        static final String LOG_LEVEL = "logLevel";
        public static final String SIMPLE_LOG_LEVEL =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.SIMPLE_LOGGER + DELIM + LOG_LEVEL;
        public static final String SIMPLE_LOGGER_LOG_LEVEL = "SimpleLogger.%s.level";

        /**
         * Property that can be configured with the maximum number of status data entries to keep queued. Once the limit is
         * reached, older entries will be removed as new entries are added. The default value is 200.
         */
        // StatusLogger.entries
        static final String ENTRIES = "entries";

        public static final String STATUS_MAX_ENTRIES =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.STATUS_LOGGER + DELIM + ENTRIES;

        /**
         * Property that can be configured with the {@link Level} name to use as the default level for
         * {@link StatusListener}s. The default value is {@link Level#WARN}.
         */
        // StatusLogger.listenerLevel
        static final String LISTENER_LEVEL = "listenerLevel";

        public static final String STATUS_DEFAULT_LISTENER_LEVEL =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.STATUS_LOGGER + DELIM + LISTENER_LEVEL;

        /**
         * Property that can be configured with a date-time format string to use as the format for timestamps
         * in the status logger output. See {@link java.text.SimpleDateFormat} for supported formats.
         */
        // StatusLogger.dateFormat
        public static final String STATUS_DATE_FORMAT =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.STATUS_LOGGER + DELIM + DATE_TIME_FORMAT;

        /**
         * Property to control whether {@link ThreadContext} stores map data. If set to {@code true}, then the
         * thread context map will be disabled.
         */
        // ThreadContext.enableMap
        static final String ENABLE_MAP = "enableMap";

        public static final String THREAD_CONTEXT_MAP_ENABLED =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.THREAD_CONTEXT + DELIM + ENABLE_MAP;

        /**
         * Property to control whether {@link ThreadContext} stores stack data. If set to {@code true}, then the
         * thread context stack will be disabled.
         */
        // ThreadContext.enableStack
        static final String ENABLE_STACK = "enableStack";

        public static final String THREAD_CONTEXT_STACK_ENABLED =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.THREAD_CONTEXT + DELIM + ENABLE_STACK;

        /**
         * Property to control whether {@link ThreadContext} stores any data. If set to {@code true}, then the
         * thread context map and stack will be disabled.
         */
        // ThreadContext.enable
        public static final String THREAD_CONTEXT_ENABLED =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.THREAD_CONTEXT + DELIM + ENABLE;

        /**
         * Property to control whether {@link ThreadContextMap} uses {@link InheritableThreadLocal} when {@code true} or
         * {@link ThreadLocal} otherwise for holding map data.
         */
        // ThreadContext.mapInheritable
        static final String MAP_INHERITABLE = "mapInheritable";

        public static final String THREAD_CONTEXT_MAP_INHERITABLE =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.THREAD_CONTEXT + DELIM + MAP_INHERITABLE;

        /**
         * Property to override the default {@link ThreadContextMap} class. Note that implementation classes should
         * also implement {@link ReadOnlyThreadContextMap} if they should be accessible to applications via
         * {@link ThreadContext#getThreadContextMap()}.
         */
        static final String MAP_CLASS = "mapClass";

        public static final String THREAD_CONTEXT_MAP_CLASS =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.THREAD_CONTEXT + DELIM + MAP_CLASS;

        /**
         * Property to override the initial capacity of the thread context map. The default value is 16.
         */
        // ThreadContext.initialCapacity
        static final String INITIAL_CAPACITY = "initialCapcity";

        public static final String THREAD_CONTEXT_INITIAL_CAPACITY =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.THREAD_CONTEXT + DELIM + INITIAL_CAPACITY;

        /**
         * Property to override whether to use a garbage-free implementation of {@link ThreadContextMap}.
         */
        // ThreadContext.garbageFree
        static final String GARBAGE_FREE = "garbageFree";

        public static final String THREAD_CONTEXT_GARBAGE_FREE_ENABLED =
                SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.THREAD_CONTEXT + DELIM + GARBAGE_FREE;

        static final String IS_WEB_APP = "isWebApp";
        public static final String WEB =
                LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.WEB + DELIM + IS_WEB_APP;

        static final String RECYCLER_FACTORY = "factory";
        public static final String RECYCLER_FACTORY_PROPERTY = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.RECYCLER
                + DELIM
                + RECYCLER_FACTORY;

        static final String RECYCLER_CAPACITY = "capacity";
        public static final String RECYCLER_CAPACITY_PROPERTY = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.RECYCLER
                + DELIM
                + RECYCLER_CAPACITY;
    }
}
