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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map.Entry;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationListener;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;

/**
 * Test the Log4jBridgeHandler.
 * Requires some configurations in the log-config-files, for format/layout
 * see also jul() and log4j():
 * - JUL-config ("logging-test.properties", must be set on JVM-start via "-D..."):
 *   + handlers = org.apache.logging.log4j.jul.Log4jBridgeHandler, java.util.logging.ConsoleHandler
 *   + org.apache.logging.log4j.jul.Log4jBridgeHandler.appendSuffix = _JUL
 *   + java.util.logging.ConsoleHandler.level = ALL
 *   + java.util.logging.SimpleFormatter.format = JUL:  %1$tT.%1$tL %4$s [%3$s: %2$s]  -  %5$s%6$s%n
 *   + .level = FINER
 * - log4j2-config ("log4j2-test.xml"):
 *   + <Root level="TRACE">
 *   + <Appenders> <Console> with target="SYSTEM_ERR", follow="true",
 *      <PatternLayout> "log4j2:  %d{HH:mm:ss.SSS} %5level - [%thread][%logger: %class/%method/%line]  -  %message%n"
 *
 * This test needs to capture syserr because it uses java.util.logging.ConsoleHandler.
 * Also, it performs some outputs to console (sysout and syserr); see also field OUTPUT_CAPTURED.
 *
 * The code also contains evaluation/test code for development time. It is not used for the unit tests
 * but kept here for reference and info. See field DEVTEST.
 */
@FixMethodOrder(org.junit.runners.MethodSorters.NAME_ASCENDING) // is nicer for manually checking console output
public class Log4jBridgeHandlerTest {
    /** Perform developer tests? */
    private static final boolean DEVTEST = false;

    /** Do output the captured logger-output to stdout? */
    private static final boolean OUTPUT_CAPTURED =
            !DEVTEST && Boolean.parseBoolean(System.getProperty("log4j.Log4jBridgeHandlerTest.outputCaptured"));

    /** This classes' simple name = relevant part of logger name. */
    private static final String CSNAME = Log4jBridgeHandlerTest.class.getSimpleName();
    // loggers used in many tests
    private static final java.util.logging.Logger julLog =
            java.util.logging.Logger.getLogger(Log4jBridgeHandlerTest.class.getName());
    private static final org.apache.logging.log4j.Logger log4jLog = org.apache.logging.log4j.LogManager.getLogger();

    // capture sysout/syserr
    // @Rule  public final SystemErrRule systemOutRule = new SystemErrRule().enableLog();
    private static final ByteArrayOutputStream sysoutBytes = new ByteArrayOutputStream(1024);
    private static PrintStream prevSysErrStream;

    @BeforeClass
    public static void beforeClass() {
        // debug output to easily recognize misconfig.:
        // System.out.println("sys-props:\n" + System.getProperties());
        System.out.println("sysout:  logging-cfg-file:  " + System.getProperty("java.util.logging.config.file"));
        if (DEVTEST) devTestBeforeClass(); // call before stderr capturing

        // JUL does not like setting stderr inbetween, so set it once and reset collecting stream
        // for each method; (thus com.github.stefanbirkner:system-rules:SystemErrRule cannot be used)
        System.err.println("vvv--- BEGIN capturing output to stderr ---vvv"
                + "   (do output of captured text to orig. stderr: " + OUTPUT_CAPTURED + ")");
        prevSysErrStream = System.err;
        System.setErr(new PrintStream(sysoutBytes, true));
    }

    @AfterClass
    public static void afterClass() {
        // reset sysout/err to original value
        System.setErr(prevSysErrStream);
        System.err.println("^^^--- END capturing output of stderr ---^^^");
    }

    @Before
    public void beforeTest() {
        // reset sysout collector
        sysoutBytes.reset();
    }

    /** Assert that captured sysout matches given regexp (any text may follow afterwards). */
    private void assertSysoutMatches(String regex) {
        // String logOutput = systemOutRule.getLogWithNormalizedLineSeparator();
        String logOutput = sysoutBytes.toString();
        if (OUTPUT_CAPTURED) prevSysErrStream.print(logOutput);
        logOutput = logOutput.replace("\r\n", "\n");
        regex = regex + "(.|\\n)*"; // allow any text with NL afterwards
        assertTrue("Unmatching output:\n" + logOutput + "\n-- vs: --\n" + regex + "\n----", logOutput.matches(regex));
    }

    /** Get regex for a JUL console output. Must match JUL-Console-Formatter! */
    private String jul(
            final java.util.logging.Level lvl,
            final String locationPartRE,
            final String msgPartRE,
            final String exceptionClassAndMsgRE) {
        return "JUL:.*" + lvl.getLocalizedName() + ".*" + CSNAME
                + ".*" + locationPartRE + ".*" + msgPartRE + ".*\n" // use real \n at end here for better error output
                + (exceptionClassAndMsgRE == null ? "" : ".*" + exceptionClassAndMsgRE + ".*\\n(\tat .*\\n)*\\n?");
    }

    /** Get regex for a log4j console output. Must match log4j2-Console-Layout! */
    private String log4j(
            final org.apache.logging.log4j.Level lvl,
            final boolean julBridged,
            final String methodPartRE,
            final String msgPartRE,
            final String exceptionClassAndMsgRE) {
        return "log4j2:.*" + lvl.name() + ".*" + CSNAME + (julBridged ? "\\._JUL" : "")
                + ".*" + CSNAME + "/\\w*" + methodPartRE + "\\w*/.*"
                + msgPartRE + ".*\n" // use real \n at end here for better error output
                + (exceptionClassAndMsgRE == null ? "" : ".*" + exceptionClassAndMsgRE + ".*\\n(\tat .*\\n)*\\n?");
    }

    @Test
    public void test1SimpleLoggings1Jul() {
        julLog.info("Test-'Info'-Log with JUL");
        julLog.fine("Test-'Fine'-Log with JUL");
        julLog.finest("Test-'Finest'-Log with JUL"); // should not be logged because JUL-level is FINER
        julLog.warning("Test-'Warn'-Log with JUL"); // thus add another log afterwards to allow checking
        final String methodRE = "SimpleLoggings1Jul";
        assertSysoutMatches(log4j(org.apache.logging.log4j.Level.INFO, true, methodRE, "'Info'-Log with JUL", null)
                + jul(java.util.logging.Level.INFO, methodRE, "'Info'-Log with JUL", null)
                + log4j(org.apache.logging.log4j.Level.DEBUG, true, methodRE, "'Fine'-Log with JUL", null)
                + jul(java.util.logging.Level.FINE, methodRE, "'Fine'-Log with JUL", null)
                // no finest/trace
                + log4j(org.apache.logging.log4j.Level.WARN, true, methodRE, "'Warn'-Log with JUL", null)
                + jul(java.util.logging.Level.WARNING, methodRE, "'Warn'-Log with JUL", null));
    }

    @Test
    public void test1SimpleLoggings2Log4jDirect() {
        log4jLog.info("Test-'Info'-Log with log4j2");
        log4jLog.debug("Test-'Debug'-Log with log4j2");
        log4jLog.trace("Test-'Trace'-Log with log4j2");
        final String methodRE = "SimpleLoggings2Log4jDirect";
        assertSysoutMatches(log4j(org.apache.logging.log4j.Level.INFO, false, methodRE, "'Info'-Log with log4j2", null)
                + log4j(org.apache.logging.log4j.Level.DEBUG, false, methodRE, "'Debug'-Log with log4j2", null)
                + log4j(org.apache.logging.log4j.Level.TRACE, false, methodRE, "'Trace'-Log with log4j2", null));
    }

    @Test
    public void test2SubMethod() {
        subMethodWithLogs(); // location info is sub method now
        final String methodRE = "subMethodWithLogs";
        assertSysoutMatches(
                log4j(org.apache.logging.log4j.Level.DEBUG, true, methodRE, "'Fine'-Log with JUL in subMethod", null)
                        + jul(java.util.logging.Level.FINE, methodRE, "'Fine'-Log with JUL in subMethod", null)
                        + log4j(
                                org.apache.logging.log4j.Level.INFO,
                                false,
                                methodRE,
                                "'Info'-Log with log4j2 in subMethod",
                                null));
    }

    private void subMethodWithLogs() {
        julLog.fine("Test-'Fine'-Log with JUL in subMethod");
        log4jLog.info("Test-'Info'-Log with log4j2 in subMethod");
    }

    @Test
    public void test3JulFlow1() {
        // note: manually given source information get lost in log4j!
        julLog.entering("enteringExampleClassParam", "enteringExampleMethodParam");
        final String methodRE = "JulFlow";
        assertSysoutMatches(log4j(org.apache.logging.log4j.Level.TRACE, true, methodRE, "ENTRY", null)
                + jul(
                        java.util.logging.Level.FINER,
                        "enteringExampleClassParam enteringExampleMethodParam",
                        "ENTRY",
                        null));
    }

    @Test
    public void test3JulFlow2() {
        // note: manually given source information get lost in log4j!
        julLog.entering("enteringExampleClassParam", "enteringExampleMethodParam_withParams", new Object[] {
            "with some", "parameters", 42
        });
        final String methodRE = "JulFlow";
        assertSysoutMatches(
                log4j(org.apache.logging.log4j.Level.TRACE, true, methodRE, "ENTRY.*with some.*param.*42", null)
                        + jul(
                                java.util.logging.Level.FINER,
                                "enteringExampleClassParam enteringExampleMethodParam_withParams",
                                "ENTRY.*with some.*param.*42",
                                null));
    }

    @Test
    public void test3JulFlow3() {
        // note: manually given source information get lost in log4j!
        julLog.exiting(
                "exitingExampleClassParam",
                "exitingExampleMethodParam",
                Arrays.asList("array of Strings", "that are the exit", "result"));
        final String methodRE = "JulFlow";
        assertSysoutMatches(log4j(
                        org.apache.logging.log4j.Level.TRACE,
                        true,
                        methodRE,
                        "RETURN.*array of Str.*that are.*result",
                        null)
                + jul(
                        java.util.logging.Level.FINER,
                        "exitingExampleClassParam exitingExampleMethodParam",
                        "RETURN.*array of Str.*that are.*result",
                        null));
    }

    @Test
    public void test3JulFlow4() {
        // note: manually given source information get lost in log4j!
        julLog.throwing(
                "throwingExampleClassParam",
                "throwingExampleMethodParam",
                new IllegalStateException("ONLY TEST for JUL-throwing()"));
        final String methodRE = "JulFlow";
        assertSysoutMatches(log4j(
                        org.apache.logging.log4j.Level.TRACE,
                        true,
                        methodRE,
                        "THROW",
                        "IllegalStateException.*ONLY TEST for JUL-throwing")
                + jul(
                        java.util.logging.Level.FINER,
                        "throwingExampleClassParam throwingExampleMethodParam",
                        "THROW",
                        "IllegalStateException.*ONLY TEST for JUL-throwing"));
    }

    @Test
    public void test4JulSpecials1() {
        julLog.log(
                java.util.logging.Level.WARNING,
                "JUL-Test via log() as warning with exception",
                new java.util.zip.DataFormatException("ONLY TEST for JUL.log()"));
        final String methodRE = "JulSpecials";
        assertSysoutMatches(log4j(
                        org.apache.logging.log4j.Level.WARN,
                        true,
                        methodRE,
                        "JUL-Test via log\\(\\) as warning",
                        "DataFormatException.*ONLY TEST for JUL")
                + jul(
                        java.util.logging.Level.WARNING,
                        methodRE,
                        "JUL-Test via log\\(\\) as warning",
                        "DataFormatException.*ONLY TEST for JUL"));
    }

    @Test
    public void test4JulSpecials2() {
        // test with MessageFormat
        julLog.log(
                java.util.logging.Level.INFO,
                "JUL-Test via log() with parameters (0={0}, 1={1}, 2={2,number,##000.0})",
                new Object[] {"a", "b", 42});
        final String methodRE = "JulSpecials";
        assertSysoutMatches(log4j(
                        org.apache.logging.log4j.Level.INFO,
                        true,
                        methodRE,
                        "JUL-Test via log\\(\\) with parameters \\(0=a, 1=b, 2=042.0\\)",
                        null)
                + jul(
                        java.util.logging.Level.INFO,
                        methodRE,
                        "JUL-Test via log\\(\\) with parameters \\(0=a, 1=b, 2=042.0\\)",
                        null));
    }

    // no test for logrb(ResourceBundle)-case as this is very specific and seldom used (in
    // my opinion); and it does not add any real thing to test here

    private void assertLogLevel(final String loggerName, final java.util.logging.Level julLevel) {
        final java.util.logging.Logger lg =
                java.util.logging.LogManager.getLogManager().getLogger(loggerName);
        assertEquals("Logger '" + loggerName + "'", julLevel, (lg == null ? null : lg.getLevel()));
    }

    @Test
    public void test5LevelPropFromConfigFile() {
        // JUL levels are set from config files and the initial propagation
        assertLogLevel("", java.util.logging.Level.FINE);
        assertLogLevel("log4j.Log4jBridgeHandlerTest.propagate1", java.util.logging.Level.FINE);
        assertLogLevel("log4j.Log4jBridgeHandlerTest.propagate1.nested1", java.util.logging.Level.FINER);
        assertLogLevel("log4j.Log4jBridgeHandlerTest.propagate1.nested2.deeplyNested", java.util.logging.Level.WARNING);
        assertLogLevel("log4j.Log4jBridgeHandlerTest.propagate2", java.util.logging.Level.ALL);
        assertLogLevel("log4j.Log4jBridgeHandlerTest.propagate2.nested.deeplyNested", java.util.logging.Level.INFO);
        // these are set in logging.properties but not in log4j2.xml:
        assertLogLevel("log4j.Log4jBridgeHandlerTest.propagate2.nested", null);
        assertLogLevel("javax.mail", null);
        // these should not exist:
        assertLogLevel("log4j.Log4jBridgeHandlerTest", null);
        assertLogLevel("log4j.Log4jBridgeHandlerTest.propagate1.nested", null);
        assertLogLevel("log4j.Log4jBridgeHandlerTest.propagate1.nested1.deeplyNested", null);
    }

    @Test
    public void test5LevelPropSetLevel() {
        String name = "log4j.test.new_logger_level_set";
        Configurator.setLevel(name, org.apache.logging.log4j.Level.DEBUG);
        assertLogLevel(name, java.util.logging.Level.FINE);
        test5LevelPropFromConfigFile(); // the rest should be untouched!

        name = "log4j.Log4jBridgeHandlerTest.propagate1.nested1";
        Configurator.setLevel(name, org.apache.logging.log4j.Level.WARN);
        assertLogLevel(name, java.util.logging.Level.WARNING);
        // the others around should be untouched
        assertLogLevel("log4j.Log4jBridgeHandlerTest.propagate1", java.util.logging.Level.FINE);
        assertLogLevel("log4j.Log4jBridgeHandlerTest.propagate1.nested2.deeplyNested", java.util.logging.Level.WARNING);

        // note: no need to check for the other set[Root]Level() methods, because they all call
        // loggerContext.updateLoggers() which calls firePropertyChangeEvent()
    }

    @Test
    public void test5LevelPropGC() {
        // this test will fail if you comment out "julLoggerRefs.add(julLog);" in propagateLogLevels()
        test5LevelPropFromConfigFile(); // at start, all should be fine
        final java.util.logging.Logger julLogRef =
                java.util.logging.Logger.getLogger("log4j.Log4jBridgeHandlerTest.propagate1.nested1");
        System.gc(); // a single call is sufficient
        System.out.println("sysout:  test5LevelPropGC() still has reference to JUL-logger: " + julLogRef.getName()
                + " / " + julLogRef);
        try {
            test5LevelPropFromConfigFile(); // even after GC the not referenced loggers should still be there
        } catch (Throwable t) {
            debugPrintJulLoggers("After GC");
            // => JUL root logger, above explicitly referenced logger and its parent ("...propagate1")
            //    and the global referenced julLog ("...jul.Log4jBridgeHandlerTest") are still there, the
            //    others are null-references now
            throw t;
        }
    }

    /** Print all available JUL loggers to stdout. */
    private static void debugPrintJulLoggers(final String infoStr) {
        final java.util.logging.LogManager julMgr = java.util.logging.LogManager.getLogManager();
        System.out.println("sysout:  " + infoStr + " - for " + julMgr);
        final java.util.List<String> txt = new java.util.ArrayList<>();
        int n = 1;
        for (Enumeration<String> en = julMgr.getLoggerNames(); en.hasMoreElements(); ) {
            final String ln = en.nextElement();
            final java.util.logging.Logger lg = julMgr.getLogger(ln);
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
    }

    ////////////////
    ////////////////   INTERNAL DEVELOPER TESTS follow
    ////////////////   (these are NOT neccessary for unit test but source is kept here for reference and info)

    static {
        if (DEVTEST) {
            System.out.println("sysout:  static init. BEGIN");

            // get log4j context impl. (requires log4j-core!)
            // note:  "LogManager.getContext();"  does not work, it returns another instance!?!
            final LoggerContext context = LoggerContext.getContext(false); // this matches Configurator.setLevel() impl.
            final Configuration cfg = context.getConfiguration();
            // print real loggers (=> is empty when using LogManager.getContext()!?! only contains already instantiated
            // loggers)
            System.out.println("LogCtx " + context + " '" + context.getName() + "',  loc = "
                    + context.getConfigLocation() + ",  cfg = " + cfg + " = " + System.identityHashCode(cfg));
            for (org.apache.logging.log4j.Logger lg : context.getLoggers()) {
                System.out.println("- Logger '" + lg.getName() + "',  lvl = " + lg.getLevel());
            }
            // print logger configurations (=> all loggers with level are present here)
            System.out.println("Loggers in Cfg:");
            for (Entry<String, LoggerConfig> entry : cfg.getLoggers().entrySet()) {
                final LoggerConfig lcfg = entry.getValue();
                System.out.println(
                        "- '" + entry.getKey() + "' = '" + lcfg.getName() + "' / " + lcfg.getLevel() + "; " + lcfg);
            }

            // print JUL loggers (=> is completely init. here, even if first JUL log and BridgeHandler-creation happens
            // later)
            debugPrintJulLoggers("in static-class-init");
            /* java.util.logging.LogManager julMgr = java.util.logging.LogManager.getLogManager();
            System.out.println("\nJUL-Loggers for " + julMgr);
            for (Enumeration<String> en = julMgr.getLoggerNames();  en.hasMoreElements(); ) {
                String ln = en.nextElement();
                java.util.logging.Logger lg = julMgr.getLogger(ln);
                if (lg.getLevel() == null) {
                    System.out.println("-    (null-Level Logger '" + ln + "')");
                } else {
                    System.out.println("- Logger '" + ln + "',  lvl = " + lg.getLevel());
                }
            } */

            // changing of log4j config. is to be done via log4j.core.config.Configurator,
            // e.g. setLevel(loggerName, newLevel)
            // Note: the (internal) log4j.core.Logger has a setLevel() but this should not be used.
            final CfgListener listener = new CfgListener();
            cfg.addListener(listener); // => onChange() is never called: not on start, not on setLevel
            context.addPropertyChangeListener(listener);

            System.out.println("sysout:  static init. END");
        } // if
    }

    private static void devTestBeforeClass() {
        log4jLog.info("Dummy-Start-Log in beforeClass()"); // force init. of log4j (needed?? does not harm)
        @SuppressWarnings("resource")
        final LoggerContext context = LoggerContext.getContext(
                false); // this matches Configurator.setLevel() impl. (instead of "LogManager.getContext();")
        System.out.println("beforeClass():  LogCtx " + context + " '" + context.getName() + "',  loc = "
                + context.getConfigLocation() + ",  cfg = " + context.getConfiguration());
        for (org.apache.logging.log4j.Logger lg : context.getLoggers()) {
            System.out.println("- Logger '" + lg.getName() + "',  lvl = " + lg.getLevel());
        }

        // force level change
        System.out.println("sysout:  now calling log4j-setLevel()");
        Configurator.setLevel("log4jTest.Dummy_set_in_devTestBeforeClass", org.apache.logging.log4j.Level.DEBUG);
    }

    private static class CfgListener implements ConfigurationListener, PropertyChangeListener {
        public CfgListener() {
            System.out.println("sysout:  CfgListener created: " + this);
        }

        @Override
        public void onChange(final Reconfigurable reconfigurable) { // from ConfigurationListener
            System.out.println("sysout:  CfgListener.CfgLi-onChange(): " + reconfigurable + " = "
                    + System.identityHashCode(reconfigurable));
        }

        @Override
        public void propertyChange(final PropertyChangeEvent evt) { // from PropertyChangeListener
            System.out.println("sysout:  CfgListener.PropChLi-propertyChange(): " + evt);
        }
    }
}
