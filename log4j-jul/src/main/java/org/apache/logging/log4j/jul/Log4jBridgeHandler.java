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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.LogRecord;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
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
 *    (and typically also:   <code>org.apache.logging.log4j.jul.Log4jBridgeHandler.propagateLevels = true</code> )<br>
 *    Note: in a webapp running on Tomcat, you may create a <code>WEB-INF/classes/logging.properties</code>
 *    file to configure JUL for this webapp only: configured handlers and log levels affect your webapp only!
 *    This file is then the <i>complete</i> JUL configuration, so JUL's defaults (e.g. log level INFO) apply
 *    for stuff not explicitly defined therein.
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
 * <li> Log4jBridgeHandler.<code>propagateLevels</code>   boolean, "true" to automatically propagate log4j log levels to JUL.
 * <li> Log4jBridgeHandler.<code>sysoutDebug</code>   boolean, perform some (developer) debug output to sysout
 * </ul>
 *
 * Log levels are translated with {@link LevelTranslator}, see also
 * <a href="https://logging.apache.org/log4j/2.x/log4j-jul/index.html#Default_Level_Conversions">log4j doc</a>.<br><br>
 *
 * Restrictions:<ul>
 * <li> Manually given source/location info in JUL (e.g. entering(), exiting(), throwing(), logp(), logrb() )
 *    will NOT be considered, i.e. gets lost in log4j logging.
 * <li> Log levels of JUL have to be adjusted according to log4j log levels:
 *      Either by using "propagateLevels" (preferred), or manually by specifying them explicitly,
 *      i.e. logging.properties and log4j2.xml have some redundancies.
 * <li> Only JUL log events that are allowed according to the JUL log level get to this handler and thus to log4j.
 *      This is only relevant and important if you NOT use "propagateLevels".
 *      If you set <code>.level = SEVERE</code> only error logs will be seen by this handler and thus log4j
 *      - even if the corresponding log4j log level is ALL.<br>
 *      On the other side, you should NOT set <code>.level = FINER  or  FINEST</code> if the log4j level is higher.
 *      In this case a lot of JUL log events would be generated, sent via this bridge to log4j and thrown away by the latter.<br>
 *      Note: JUL's default log level (i.e. none specified in logger.properties) is INFO.
 * </ul>
 *
 * (Credits: idea and concept originate from org.slf4j.bridge.SLF4JBridgeHandler;
 *   level propagation idea originates from logback/LevelChangePropagator;
 *   but no source code has been copied)
 */
public class Log4jBridgeHandler extends java.util.logging.Handler implements PropertyChangeListener {
    private static final org.apache.logging.log4j.Logger SLOGGER = StatusLogger.getLogger();

    // the caller of the logging is java.util.logging.Logger (for location info)
    private static final String FQCN = java.util.logging.Logger.class.getName();
    private static final String UNKNOWN_LOGGER_NAME = "unknown.jul.logger";
    private static final java.util.logging.Formatter julFormatter = new java.util.logging.SimpleFormatter();

    private boolean doDebugOutput = false;
    private String julSuffixToAppend = null;
    private volatile boolean installAsLevelPropagator = false;

    /**
     * Adds a new Log4jBridgeHandler instance to JUL's root logger.
     * This is a programmatic alternative to specify
     * <code>handlers = org.apache.logging.log4j.jul.Log4jBridgeHandler</code>
     * and its configuration in logging.properties.<br>
     * @param removeHandlersForRootLogger  true to remove all other installed handlers on JUL root level
     */
    public static void install(boolean removeHandlersForRootLogger, String suffixToAppend, boolean propagateLevels) {
        final java.util.logging.Logger rootLogger = getJulRootLogger();
        if (removeHandlersForRootLogger) {
            for (java.util.logging.Handler hdl : rootLogger.getHandlers()) {
                rootLogger.removeHandler(hdl);
            }
        }
        rootLogger.addHandler(new Log4jBridgeHandler(false, suffixToAppend, propagateLevels));
        // note: filter-level of Handler defaults to ALL, so nothing to do here
    }

    private static java.util.logging.Logger getJulRootLogger() {
        return java.util.logging.LogManager.getLogManager().getLogger("");
    }

    /** Initialize this handler by reading out JUL configuration. */
    public Log4jBridgeHandler() {
        final java.util.logging.LogManager julLogMgr = java.util.logging.LogManager.getLogManager();
        final String className = this.getClass().getName();
        init(
                Boolean.parseBoolean(julLogMgr.getProperty(className + ".sysoutDebug")),
                julLogMgr.getProperty(className + ".appendSuffix"),
                Boolean.parseBoolean(julLogMgr.getProperty(className + ".propagateLevels")));
    }

    /** Initialize this handler with given configuration. */
    public Log4jBridgeHandler(boolean debugOutput, String suffixToAppend, boolean propagateLevels) {
        init(debugOutput, suffixToAppend, propagateLevels);
    }

    /** Perform init. of this handler with given configuration (typical use is for constructor). */
    @SuppressFBWarnings(
            value = "INFORMATION_EXPOSURE_THROUGH_AN_ERROR_MESSAGE",
            justification = "The data is available only in debug mode.")
    protected void init(boolean debugOutput, String suffixToAppend, boolean propagateLevels) {
        this.doDebugOutput = debugOutput;
        if (debugOutput) {
            new Exception("DIAGNOSTIC ONLY (sysout):  Log4jBridgeHandler instance created (" + this + ")")
                    .printStackTrace(System.out); // is no error thus no syserr
        }

        if (suffixToAppend != null) {
            suffixToAppend = suffixToAppend.trim(); // remove spaces
            if (suffixToAppend.isEmpty()) {
                suffixToAppend = null;
            } else if (suffixToAppend.charAt(0) != '.') { // always make it a sub-logger
                suffixToAppend = '.' + suffixToAppend;
            }
        }
        this.julSuffixToAppend = suffixToAppend;

        this.installAsLevelPropagator = propagateLevels;

        SLOGGER.debug(
                "Log4jBridgeHandler init. with: suffix='{}', lvlProp={}, instance={}",
                suffixToAppend,
                propagateLevels,
                this);
    }

    @Override
    public void close() {
        // cleanup and remove listener and JUL logger references
        julLoggerRefs = null;
        LoggerContext.getContext(false).removePropertyChangeListener(this);
        if (doDebugOutput) {
            System.out.println("sysout:  Log4jBridgeHandler close(): " + this);
        }
    }

    @Override
    public void publish(final LogRecord record) {
        if (record == null) { // silently ignore null records
            return;
        }

        // Only execute synchronized code if we really have to
        if (this.installAsLevelPropagator) {
            synchronized (this) {
                // Check again to make sure we still have to propagate  the levels at this point
                if (this.installAsLevelPropagator) {
                    @SuppressWarnings("resource")
                    final // no need to close the AutoCloseable ctx here
                    LoggerContext context = LoggerContext.getContext(false);
                    context.addPropertyChangeListener(this);
                    propagateLogLevels(context.getConfiguration());
                    // note: java.util.logging.LogManager.addPropertyChangeListener() could also
                    // be set here, but a call of JUL.readConfiguration() will be done on purpose
                    this.installAsLevelPropagator = false;
                }
            }
        }

        final org.apache.logging.log4j.Logger log4jLogger = getLog4jLogger(record);
        final String msg = julFormatter.formatMessage(record); // use JUL's implementation to get real msg
        /* log4j allows nulls:
        if (msg == null) {
            // JUL allows nulls, but other log system may not
            msg = "<null log msg>";
        } */
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

    /////  log level propagation code

    @Override
    // impl. for PropertyChangeListener
    public void propertyChange(final PropertyChangeEvent evt) {
        SLOGGER.debug("Log4jBridgeHandler.propertyChange(): {}", evt);
        if (LoggerContext.PROPERTY_CONFIG.equals(evt.getPropertyName()) && evt.getNewValue() instanceof Configuration) {
            propagateLogLevels((Configuration) evt.getNewValue());
        }
    }

    /** Save "hard" references to configured JUL loggers. (is lazy init.) */
    private Set<java.util.logging.Logger> julLoggerRefs;
    /** Perform developer tests? (Should be unused/outcommented for real code) */
    // private static final boolean DEVTEST = false;

    private void propagateLogLevels(final Configuration config) {
        SLOGGER.debug("Log4jBridgeHandler.propagateLogLevels(): {}", config);
        // clear or init. saved JUL logger references
        // JUL loggers have to be explicitly referenced because JUL internally uses
        // weak references so not instantiated loggers may be garbage collected
        // and their level config gets lost then.
        if (julLoggerRefs == null) {
            julLoggerRefs = new HashSet<>();
        } else {
            julLoggerRefs.clear();
        }

        // if (DEVTEST)  debugPrintJulLoggers("Start of propagation");
        // walk through all log4j configured loggers and set JUL level accordingly
        final Map<String, LoggerConfig> log4jLoggers = config.getLoggers();
        // java.util.List<String> outTxt = new java.util.ArrayList<>();    // DEVTEST / DEV-DEBUG ONLY
        for (LoggerConfig lcfg : log4jLoggers.values()) {
            final java.util.logging.Logger julLog =
                    java.util.logging.Logger.getLogger(lcfg.getName()); // this also fits for root = ""
            final java.util.logging.Level julLevel =
                    LevelTranslator.toJavaLevel(lcfg.getLevel()); // lcfg.getLevel() never returns null
            julLog.setLevel(julLevel);
            julLoggerRefs.add(julLog); // save an explicit reference to prevent GC
            // if (DEVTEST)  outTxt.add("propagating '" + lcfg.getName() + "' / " + lcfg.getLevel() + "  ->  " +
            // julLevel);
        } // for
        // if (DEVTEST)  java.util.Collections.sort(outTxt, String.CASE_INSENSITIVE_ORDER);
        // if (DEVTEST)  for (String s : outTxt)  System.out.println("+ " + s);
        // if (DEVTEST)  debugPrintJulLoggers("After propagation");

        // cleanup JUL: reset all log levels not explicitly given by log4j
        // This has to happen after propagation because JUL creates and inits. the loggers lazily
        // so a nested logger might be created during the propagation-for-loop above and gets
        // its JUL-configured level not until then.
        final java.util.logging.LogManager julMgr = java.util.logging.LogManager.getLogManager();
        for (Enumeration<String> en = julMgr.getLoggerNames(); en.hasMoreElements(); ) {
            final java.util.logging.Logger julLog = julMgr.getLogger(en.nextElement());
            if (julLog != null
                    && julLog.getLevel() != null
                    && !"".equals(julLog.getName())
                    && !log4jLoggers.containsKey(julLog.getName())) {
                julLog.setLevel(null);
            }
        } // for
        // if (DEVTEST)  debugPrintJulLoggers("After JUL cleanup");
    }

    /* DEV-DEBUG ONLY  (comment out for release) *xx/
    private void debugPrintJulLoggers(String infoStr) {
        if (!DEVTEST)  return;
        java.util.logging.LogManager julMgr = java.util.logging.LogManager.getLogManager();
        System.out.println("sysout:  " + infoStr + " - for " + julMgr);
        java.util.List<String> txt = new java.util.ArrayList<>();
        int n = 1;
        for (Enumeration<String> en = julMgr.getLoggerNames();  en.hasMoreElements(); ) {
            String ln = en.nextElement();
            java.util.logging.Logger lg = julMgr.getLogger(ln);
            if (lg == null) {
                txt.add("(!null-Logger '" + ln + "')  #" + n);
            } else if (lg.getLevel() == null) {
                txt.add("(null-Level Logger '" + ln + "')  #" + n);
            } else {
                txt.add("Logger '" + ln + "',  lvl = " + lg.getLevel() + "  #" + n);
            }
            n++;
        } // for
        java.util.Collections.sort(txt, String.CASE_INSENSITIVE_ORDER);
        for (String s : txt) {
            System.out.println("  - " + s);
        }
    } /**/

}
