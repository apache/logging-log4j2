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
package org.apache.logging.log4j.jul;

// note: NO import of Logger, Level, LogManager to prevent conflicts JUL/log4j

import aQute.bnd.annotation.Cardinality;
import aQute.bnd.annotation.spi.ServiceConsumer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ServiceLoader;
import java.util.logging.LogRecord;
import org.apache.logging.jul.tolog4j.LevelTranslator;
import org.apache.logging.log4j.jul.internal.JulLevelPropagator;
import org.apache.logging.log4j.jul.spi.LevelChangePropagator;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.ServiceLoaderUtil;

/**
 * Bridge from JUL to Log4j API
 * <p>
 *     This is an alternative to {@link org.apache.logging.jul.tolog4j.LogManager} (running as complete JUL replacement),
 *     especially useful for webapps running on a container for which the LogManager cannot or should not be used.
 * </p>
 *
 * <p>
 *     Installation/usage:
 * </p>
 * <ul>
 *     <li>
 *         <p>Declaratively inside JUL's {@code logging.properties}):</p>
 *         <pre>
 *     handlers = org.apache.logging.log4j.jul.Log4jBridgeHandler
 *     # Enable log level propagation
 *     # This requires `log4j-jul-propagator` to be present
 *     org.apache.logging.log4j.jul.Log4jBridgeHandler.propagateLevels = true
 *         </pre>
 *         <p><strong>Note:</strong> if your application is running on Tomcat and has a
 *         {@code WEB-INF/classes/logging.properties} class, the configured handlers and log level changes will
 *         only affect you web application.</p>
 *     </li>
 *     <li>
 *         Programmatically by calling {@link #install(boolean, String, boolean)}, e.g., inside a
 *         <a href="https://jakarta.ee/specifications/platform/11/apidocs/jakarta/servlet/servletcontextlistener">ServletContextListener</a>.
 *     </li>
 * </ul>
 *
 * <p>
 *     <strong>Credits</strong>: the idea and concept originate from {@code org.slf4j.bridge.SLF4JBridgeHandler}.
 *     The level propagation idea originates from {@code ch.qos.logback.classic.jul.LevelChangePropagator}.
 *     No source code has been copied.
 * </p>
 * @see <a href="https://logging.apache.org/log4j/3.x/log4j-jul.html">Log4j documentation site</a>
 * @since 2.15.0
 */
@ServiceConsumer(value = LevelChangePropagator.class, cardinality = Cardinality.SINGLE)
public class Log4jBridgeHandler extends java.util.logging.Handler {
    private static final org.apache.logging.log4j.Logger LOGGER = StatusLogger.getLogger();

    // the caller of the logging is java.util.logging.Logger (for location info)
    private static final String FQCN = java.util.logging.Logger.class.getName();
    private static final String UNKNOWN_LOGGER_NAME = "unknown.jul.logger";
    private static final java.util.logging.Formatter julFormatter = new java.util.logging.SimpleFormatter();

    private String julSuffixToAppend = null;
    private LevelChangePropagator levelPropagator;
    private volatile boolean installAsLevelPropagator = false;

    /**
     * Adds a new Log4jBridgeHandler instance to JUL's root logger.
     * <p>
     *     This is a programmatic alternative for a {@code logging.properties} file.
     * </p>
     * @param removeHandlersForRootLogger  If {@code true}, remove all other installed handlers on JUL root level
     * @param suffixToAppend The suffix to append to each JUL logger
     * @param propagateLevels If {@code true}, the logging implementation levels will propagate to JUL.
     */
    public static void install(boolean removeHandlersForRootLogger, String suffixToAppend, boolean propagateLevels) {
        final java.util.logging.Logger rootLogger = getJulRootLogger();
        if (removeHandlersForRootLogger) {
            for (java.util.logging.Handler hdl : rootLogger.getHandlers()) {
                rootLogger.removeHandler(hdl);
            }
        }
        rootLogger.addHandler(new Log4jBridgeHandler(suffixToAppend, propagateLevels));
    }

    private static java.util.logging.Logger getJulRootLogger() {
        return java.util.logging.LogManager.getLogManager().getLogger("");
    }

    /** Initialize this handler by reading out JUL configuration. */
    public Log4jBridgeHandler() {
        final java.util.logging.LogManager julLogMgr = java.util.logging.LogManager.getLogManager();
        final String className = this.getClass().getName();
        configure(
                julLogMgr.getProperty(className + ".appendSuffix"),
                Boolean.parseBoolean(julLogMgr.getProperty(className + ".propagateLevels")));
    }

    /** Initialize this handler with given configuration. */
    private Log4jBridgeHandler(String suffixToAppend, boolean propagateLevels) {
        configure(suffixToAppend, propagateLevels);
    }

    /** Perform init. of this handler with given configuration (typical use is for constructor). */
    @SuppressFBWarnings(
            value = "INFORMATION_EXPOSURE_THROUGH_AN_ERROR_MESSAGE",
            justification = "The data is available only in debug mode.")
    private void configure(String suffixToAppend, boolean propagateLevels) {
        if (suffixToAppend != null) {
            suffixToAppend = suffixToAppend.trim(); // remove spaces
            if (suffixToAppend.isEmpty()) {
                suffixToAppend = null;
            } else if (suffixToAppend.charAt(0) != '.') { // always make it a sub-logger
                suffixToAppend = '.' + suffixToAppend;
            }
        }
        this.julSuffixToAppend = suffixToAppend;

        installAsLevelPropagator = propagateLevels;
        if (installAsLevelPropagator) {
            levelPropagator = ServiceLoaderUtil.safeStream(
                            LevelChangePropagator.class,
                            ServiceLoader.load(
                                    LevelChangePropagator.class, getClass().getClassLoader()),
                            LOGGER)
                    .findAny()
                    .orElse(JulLevelPropagator.INSTANCE);
        }

        LOGGER.debug(
                "Log4jBridgeHandler init. with: suffix='{}', lvlProp={}, instance={}",
                suffixToAppend,
                propagateLevels,
                this);
    }

    @Override
    public void close() {
        if (levelPropagator != null) {
            levelPropagator.stop();
        }
    }

    @Override
    public void publish(final LogRecord record) {
        if (record == null) { // silently ignore null records
            return;
        }

        // Only execute synchronized code if we really have to
        if (installAsLevelPropagator) {
            synchronized (this) {
                // Check again to make sure we still have to propagate the levels at this point
                if (installAsLevelPropagator) {
                    levelPropagator.start();
                    installAsLevelPropagator = false;
                }
            }
        }

        final org.apache.logging.log4j.Logger log4jLogger = getLog4jLogger(record);
        final String msg = julFormatter.formatMessage(record);
        final org.apache.logging.log4j.Level log4jLevel = LevelTranslator.toLevel(record.getLevel());
        final Throwable thrown = record.getThrown();
        if (log4jLogger instanceof ExtendedLogger) {
            // relevant for location information
            try {
                ((ExtendedLogger) log4jLogger).logIfEnabled(FQCN, log4jLevel, null, msg, thrown);
            } catch (NoClassDefFoundError e) {
                // sometimes there are problems with log4j.ExtendedStackTraceElement, so try a workaround
                log4jLogger.warn(
                        "Log4jBridgeHandler: ignored exception when calling 'ExtendedLogger': {}", e.toString());
                log4jLogger.log(log4jLevel, msg, thrown);
            }
        } else {
            log4jLogger.log(log4jLevel, msg, thrown);
        }
    }

    @Override
    public void flush() {
        // nothing to do
    }

    /**
     * Return the log4j-Logger instance that will be used for logging.
     * Handles null name case and appends configured suffix.
     */
    private org.apache.logging.log4j.Logger getLog4jLogger(final LogRecord record) {
        String name = record.getLoggerName();
        if (name == null) {
            name = UNKNOWN_LOGGER_NAME;
        } else if (julSuffixToAppend != null) {
            name += julSuffixToAppend;
        }
        return org.apache.logging.log4j.LogManager.getLogger(name);
    }
}
