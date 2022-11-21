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
package org.apache.logging.log4j.spi;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.message.FlowMessageFactory;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.util.EnvironmentPropertySource;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.Unbox;

/**
 * Centralized list of property name constants that can be configured in the Log4j API. These properties may be
 * specified as system properties, environment variables (see {@link EnvironmentPropertySource}), or in a classpath
 * resource file named {@code log4j2.component.properties}.
 *
 * @since 3.0.0
 * @see PropertiesUtil
 */
public final class LoggingSystemProperties {
    // TODO: rename properties according to established theme in
    //  https://cwiki.apache.org/confluence/display/LOGGING/Properties+Enhancement

    /**
     * Property to enable TRACE-level debug logging in the Log4j system itself.
     * <p>
     * If property {@value} is either defined empty or its value equals to {@code true} (ignoring case), all internal
     * logging will be printed to the console. The presence of this system property overrides any value set in the
     * configuration's {@code <Configuration status="<level>" ...>} status attribute.
     * </p>
     */
    public static final String SYSTEM_DEBUG = "log4j2.*.System.debug";

    /**
     * Property to override webapp detection. Without this property, the presence of the {@code Servlet} interface
     * (from either {@code javax} or {@code jakarta}) is checked to see if this is a webapp.
     */
    // Web.enableWebApp : calculate | true | false
    public static final String SYSTEM_IS_WEBAPP = "log4j2.isWebapp";

    /**
     * Property to override the use of thread-local values for garbage-free logging.
     *
     * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-1270">LOG4J2-1270</a>
     */
    // GC.enableThreadLocals : calculate | true | false
    public static final String SYSTEM_THREAD_LOCALS_ENABLED = "log4j2.enableThreadlocals";

    /**
     * Property to ignore the thread context ClassLoader when set to {@code true}.
     *
     * @see LoaderUtil
     */
    // TODO: see if this can be removed
    public static final String LOADER_IGNORE_THREAD_CONTEXT_LOADER = "log4j2.ignoreTCL";

    /**
     * Property to force use of the thread context ClassLoader.
     *
     * @see LoaderUtil
     */
    // TODO: see if this can be removed
    public static final String LOADER_FORCE_THREAD_CONTEXT_LOADER = "log4j2.forceTCLOnly";

    /**
     * Property to override the default ringbuffer size used in {@link Unbox}. The default value is 32.
     */
    // GC.unboxRingBufferSize
    public static final String UNBOX_RING_BUFFER_SIZE = "log4j2.unboxRingbufferSize";

    /**
     * Property to set to the fully qualified class name of a custom implementation of {@link LoggerContextFactory}.
     */
    // LoggerContext.factory
    public static final String LOGGER_CONTEXT_FACTORY_CLASS = "log4j2.loggerContextFactory";

    /**
     * Property to override the default {@link MessageFactory} class.
     */
    // Message.messageFactory
    public static final String LOGGER_MESSAGE_FACTORY_CLASS = "log4j2.messageFactory";

    /**
     * Property to override the default {@link FlowMessageFactory} class.
     */
    // Message.flowMessageFactory
    public static final String LOGGER_FLOW_MESSAGE_FACTORY_CLASS = "log4j2.flowMessageFactory";

    /**
     * Property to override the default maximum nesting depth of map messages to format in JSON output. The
     * default value is 8.
     */
    // Message.jsonFormatterMaxDepth
    public static final String LOGGER_MAP_MESSAGE_JSON_FORMATTER_MAX_DEPTH = "log4j2.mapMessageJsonFormatterMaxDepth";

    /**
     * Property to override the maximum size of the StringBuilder instances used in ringbuffer log events to store
     * the contents of reusable messages. After delivering messages above this size to appenders, the StringBuilder
     * is trimmed to this maximum size. The default value is 518 which allows the StringBuilder to resize three times
     * from its initial size.
     */
    // GC.maxReusableMsgSize
    public static final String GC_REUSABLE_MESSAGE_MAX_SIZE = "log4j2.maxReusableMsgSize";

    public static final String SIMPLE_SHOW_CONTEXT_MAP = "SimpleLogger.showContextMap";
    public static final String SIMPLE_SHOW_LOG_NAME = "SimpleLogger.showLogName";
    public static final String SIMPLE_SHOW_SHORT_LOG_NAME = "SimpleLogger.showShortLogName";
    public static final String SIMPLE_SHOW_DATE_TIME = "SimpleLogger.showDateTime";
    public static final String SIMPLE_DATE_TIME_FORMAT = "SimpleLogger.dateTimeFormat";
    public static final String SIMPLE_LOG_FILE = "SimpleLogger.logFile";
    public static final String SIMPLE_LOG_LEVEL = "SimpleLogger.logLevel";
    public static final String SIMPLE_LOGGER_LOG_LEVEL = "SimpleLogger.%s.level";

    /**
     * Property that can be configured with the maximum number of status data entries to keep queued. Once the limit is
     * reached, older entries will be removed as new entries are added. The default value is 200.
     */
    // StatusLogger.entries
    public static final String STATUS_MAX_ENTRIES = "log4j2.statusEntries";

    /**
     * Property that can be configured with the {@link Level} name to use as the default level for
     * {@link StatusListener}s. The default value is {@link Level#WARN}.
     */
    // StatusLogger.statusLoggerLevel
    public static final String STATUS_DEFAULT_LISTENER_LEVEL = "log4j2.statusLoggerLevel";

    /**
     * Property that can be configured with a date-time format string to use as the format for timestamps
     * in the status logger output. See {@link java.text.SimpleDateFormat} for supported formats.
     */
    // StatusLogger.dateFormat
    public static final String STATUS_DATE_FORMAT = "log4j2.statusLoggerDateFormat";

    /**
     * Property to control whether {@link ThreadContext} stores map data. If set to {@code true}, then the
     * thread context map will be disabled.
     */
    // ThreadContext.enableMap
    public static final String THREAD_CONTEXT_MAP_DISABLED = "log4j2.disableThreadContextMap";

    /**
     * Property to control whether {@link ThreadContext} stores stack data. If set to {@code true}, then the
     * thread context stack will be disabled.
     */
    // ThreadContext.enableStack
    public static final String THREAD_CONTEXT_STACK_DISABLED = "log4j2.disableThreadContextStack";

    /**
     * Property to control whether {@link ThreadContext} stores any data. If set to {@code true}, then the
     * thread context map and stack will be disabled.
     */
    // ThreadContext.enabled
    public static final String THREAD_CONTEXT_DISABLED = "log4j2.disableThreadContext";

    /**
     * Property to control whether {@link ThreadContextMap} uses {@link InheritableThreadLocal} when {@code true} or
     * {@link ThreadLocal} otherwise for holding map data.
     */
    // ThreadContext.inheritable
    public static final String THREAD_CONTEXT_MAP_INHERITABLE = "log4j2.isThreadContextMapInheritable";

    /**
     * Property to override the default {@link ThreadContextMap} class. Note that implementation classes should
     * also implement {@link ReadOnlyThreadContextMap} if they should be accessible to applications via
     * {@link ThreadContext#getThreadContextMap()}.
     */
    // TODO: replace with LoggingSystem overrides
    public static final String THREAD_CONTEXT_MAP_CLASS = "log4j2.threadContextMap";

    /**
     * Property to override the initial capacity of the thread context map. The default value is 16.
     */
    // ThreadContext.initialCapacity
    public static final String THREAD_CONTEXT_INITIAL_CAPACITY = "log4j2.threadContextInitialCapacity";

    /**
     * Property to override whether to use a garbage-free implementation of {@link ThreadContextMap}.
     */
    // ThreadContext.garbageFree
    public static final String THREAD_CONTEXT_GARBAGE_FREE_ENABLED = "log4j2.garbagefreeThreadContextMap";

    private LoggingSystemProperties() {
        throw new UnsupportedOperationException("Utility class");
    }
}
