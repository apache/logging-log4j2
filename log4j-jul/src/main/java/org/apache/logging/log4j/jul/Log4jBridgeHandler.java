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
package org.apache.logging.log4j.jul;

// note: NO import of Logger, LogManager etc. to prevent conflicts JUL/log4j
import java.util.logging.LogRecord;

import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.status.StatusLogger;


/**
 * Bridge from JUL to log4j2.<br>
 * This is an alternative to log4j.jul.LogManager (running as complete JUL replacement),
 * especially useful for webapps running on a container for which the LogManager cannot or
 * should not be used.<br><br>
 *
 * Installation/usage:<ul>
 * <li> Declaratively inside JUL's <code>logging.properties</code>:<br>
 *    <code>handlers = org.apache.logging.log4j.jul.Log4jBridgeHandler</code><br>
 *    (Note: in a webapp running on Tomcat, you may create a <code>WEB-INF/classes/logging.properties</code>
 *     file to configure JUL for this webapp only: configured handlers and log levels affect your webapp only!
 *     This file is then the <i>complete</i> JUL configuration, so JUL's defaults (e.g. log level INFO) apply
 *     for stuff not explicitly defined therein.)
 * <li> Programmatically by calling <code>install()</code> method,
 *    e.g. inside ServletContextListener static-class-init. or contextInitialized()
 * </ul>
 * Configuration (in JUL's <code>logging.properties</code>):<ul>
 * <li> Log4jBridgeHandler.<code>suffixToAppend</code><br>
 *        String, suffix to append to JUL logger names, to easily recognize bridged log messages.
 *        A dot "." is automatically prepended, so configuration for the basis logger is used<br>
 *        Example:  <code>Log4jBridgeHandler.suffixToAppend = _JUL</code><br>
 *        Useful, for example, if you use JSF because it logs exceptions and throws them afterwards;
 *        you can easily recognize the duplicates with this (or concentrate on the non-JUL-logs).
 * <li> Log4jBridgeHandler.<code>propagateLevels</code>   (this is TODO, but usage is possible without this!) boolean, "true" to automatically propagate log4j log levels to JUL.
 * <li> Log4jBridgeHandler.<code>sysoutDebug</code>   boolean, perform some (developer) debug output to sysout
 * </ul>
 *
 * Log levels are translated with {@link LevelTranslator}, see also
 * <a href="https://logging.apache.org/log4j/2.x/log4j-jul/index.html#Default_Level_Conversions">log4j doc</a>.<br><br>
 *
 * Restrictions:<ul>
 * <li> Manually given source/location info in JUL (e.g. entering(), exiting(), throwing(), logp(), logrb() )
 *    will NOT be considered, i.e. gets lost in log4j logging.
 * <li> Log levels of JUL have to be manually adjusted according to log4j log levels (until "propagateLevels" is implemented).
 *      I.e. logging.properties and log4j2.xml have some redundancies.
 * <li> Only JUL log events that are allowed according to the JUL log level get to this handler and thus to log4j.
 *      If you set <code>.level = SEVERE</code> only error logs will be seen by this handler and thus log4j
 *      - even if the corresponding log4j log level is ALL.<br>
 *      On the other side, you should NOT set <code>.level = FINER  or  FINEST</code> if the log4j level is higher.
 *      In this case a lot of JUL log events would be generated, sent via this bridge to log4j and thrown away by the latter.<br>
 *      Note: JUL's default log level (i.e. none specified in logger.properties) is INFO.
 * </ul>
 *
 * (Credits: idea and concept originate from org.slf4j.bridge.SLF4JBridgeHandler)
 */
public class Log4jBridgeHandler extends java.util.logging.Handler {
    private static final org.apache.logging.log4j.Logger SLOGGER = StatusLogger.getLogger();

    // the caller of the logging is java.util.logging.Logger (for location info)
    private static final String FQCN = java.util.logging.Logger.class.getName();
    private static final String UNKNOWN_LOGGER_NAME = "unknown.jul.logger";
    private static final java.util.logging.Formatter julFormatter = new java.util.logging.SimpleFormatter();

    private boolean debugOutput = false;
    private String suffixToAppend = null;
    private boolean installAsLevelPropagator = false;


    /**
     * Adds a Log4jBridgeHandler instance to JUL's root logger.
     * This is a programmatic alternative to specify "handlers = org.apache.logging.log4j.jul.Log4jBridgeHandler"
     * in logging.properties.
     * This handler will redirect JUL logging to log4j2.
     * However, only logs enabled in JUL will be redirected. For example, if a log
     * statement invoking a JUL logger is disabled, then the corresponding non-event
     * will <em>not</em> reach Log4jBridgeHandler and cannot be redirected.
     *
     * @param removeHandlersForRootLogger  remove all other installed handlers on JUL root level
     */
    public static void install(boolean removeHandlersForRootLogger) {
        java.util.logging.Logger rootLogger = getJulRootLogger();
        if (removeHandlersForRootLogger) {
            for (java.util.logging.Handler hdl : rootLogger.getHandlers()) {
                rootLogger.removeHandler(hdl);
            }
        }
        rootLogger.addHandler(new Log4jBridgeHandler());
        // note: filter-level of Handler defaults to ALL, so nothing to do here
    }

    private static java.util.logging.Logger getJulRootLogger() {
        return java.util.logging.LogManager.getLogManager().getLogger("");
    }


    /**
     * Initialize this handler. Read out configuration.
     */
    public Log4jBridgeHandler() {
        final java.util.logging.LogManager julLogMgr = java.util.logging.LogManager.getLogManager();
        final String className = this.getClass().getName();
        debugOutput = Boolean.parseBoolean(julLogMgr.getProperty(className + ".sysoutDebug"));
        if (debugOutput) {
            new Exception("DIAGNOSTIC ONLY (sysout):  Log4jBridgeHandler instance created (" + this + ")")
                    .printStackTrace(System.out);		// is no error thus no syserr
        }

        suffixToAppend = julLogMgr.getProperty(className + ".appendSuffix");
        if (suffixToAppend != null) {
            suffixToAppend = suffixToAppend.trim();		// remove spaces
            if (suffixToAppend.isEmpty()) {
                suffixToAppend = null;
            } else if (suffixToAppend.charAt(0) != '.') {		// always make it a sub-logger
                suffixToAppend = '.' + suffixToAppend;
            }
        }
        installAsLevelPropagator = Boolean.parseBoolean(julLogMgr.getProperty(className + ".propagateLevels"));
        // TODO really do install
        if (installAsLevelPropagator) {
        	SLOGGER.warn("Log4jBridgeHandler.propagateLevels is currently NOT implemented."); // Call Log4jBridgeHandler.initJulLogLevels() !");
        }

        SLOGGER.debug("Log4jBridgeHandler init. with: suffix='{}', lP={}",
        		suffixToAppend, installAsLevelPropagator);
    }


    @Override
    public void close() {
        if (debugOutput) {
            System.out.println("sysout:  Log4jBridgeHandler close(): " + this);
        }
    }


    @Override
    public void publish(LogRecord record) {
        // silently ignore null records
        if (record == null) {
            return;
        }

        org.apache.logging.log4j.Logger log4jLogger = getLog4jLogger(record);
        String msg = julFormatter.formatMessage(record);		// use JUL's implementation to get real msg
        /* log4j allows nulls:
        if (msg == null) {
            // JUL allows nulls, but other log system may not
            msg = "<null log msg>";
        } */
        org.apache.logging.log4j.Level log4jLevel = LevelTranslator.toLevel(record.getLevel());
        Throwable thrown = record.getThrown();
        if (log4jLogger instanceof ExtendedLogger) {
            // relevant for location information
            try {
                ((ExtendedLogger) log4jLogger).logIfEnabled(FQCN, log4jLevel, null, msg, thrown);
            } catch (NoClassDefFoundError e) {
                // sometimes there are problems with log4j.ExtendedStackTraceElement, so try a workaround
                log4jLogger.warn("Log4jBridgeHandler: ignored exception when calling 'ExtendedLogger': {}", e.toString());
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
     * Return the Logger instance that will be used for logging.
     * Handles null name case and appends configured suffix.
     */
    protected org.apache.logging.log4j.Logger getLog4jLogger(LogRecord record) {
        String name = record.getLoggerName();
        if (name == null) {
            name = UNKNOWN_LOGGER_NAME;
        } else if (suffixToAppend != null) {
            name += suffixToAppend;
        }
        return org.apache.logging.log4j.LogManager.getLogger(name);
    }

}
