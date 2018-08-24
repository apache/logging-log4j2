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

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.logging.Level;

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
 * @author Thies Wellpott (twapache@online.de)
 */
@FixMethodOrder(org.junit.runners.MethodSorters.NAME_ASCENDING)		// is nicer for manually checking console output
public class Log4jBridgeHandlerTest {
    /** Do output the captured logger-output to stdout? */
    private static final boolean OUTPUT_CAPTURED = Boolean.parseBoolean(
            System.getProperty("log4j.Log4jBridgeHandlerTest.outputCaptured"));

    /** This classes' simple name = relevant part of logger name. */
    private static final String CSNAME = Log4jBridgeHandlerTest.class.getSimpleName();
    // loggers used in many tests
    private static final java.util.logging.Logger julLog = java.util.logging.Logger.getLogger(Log4jBridgeHandlerTest.class.getName());
    private static final org.apache.logging.log4j.Logger log4jLog = org.apache.logging.log4j.LogManager.getLogger();

    // capture sysout/syserr
    //@Rule  public final SystemErrRule systemOutRule = new SystemErrRule().enableLog();
    private static final ByteArrayOutputStream sysoutBytes = new ByteArrayOutputStream(1024);
    private static PrintStream prevSysErrStream;


    @BeforeClass
    public static void beforeClass() {
        // debug output to easily recognize misconfig.:
        //System.out.println("sys-props:\n" + System.getProperties());
        System.out.println("sysout:  logging-cfg-file:  " + System.getProperty("java.util.logging.config.file"));

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



    /** Assert that sysout matches given regexp (any text may follow afterwards). */
    private void assertSysoutMatches(String regex) {
        //String logOutput = systemOutRule.getLogWithNormalizedLineSeparator();
        String logOutput = sysoutBytes.toString();
        if (OUTPUT_CAPTURED)  prevSysErrStream.print(logOutput);
        logOutput = logOutput.replace("\r\n", "\n");
        regex = regex + "(.|\\n)*";			// allow any text with NL afterwards
        assertTrue("Unmatching output:\n" + logOutput + "\n-- vs: --\n" + regex + "\n----", logOutput.matches(regex));
    }

    /** Get regex for a JUL console output. Must match JUL-Console-Formatter! */
    private String jul(java.util.logging.Level lvl, String locationPartRE,
            String msgPartRE, String exceptionClassAndMsgRE) {
        return "JUL:.*" + lvl.getLocalizedName() + ".*" + CSNAME
                + ".*" + locationPartRE + ".*" + msgPartRE + ".*\n"	// use real \n at end here for better error output
                + (exceptionClassAndMsgRE == null  ?  ""
                        :  ".*" + exceptionClassAndMsgRE + ".*\\n(\tat .*\\n)*\\n?");
    }

    /** Get regex for a log4j console output. Must match log4j2-Console-Layout! */
    private String log4j(org.apache.logging.log4j.Level lvl, boolean julBridged,
            String methodPartRE, String msgPartRE, String exceptionClassAndMsgRE) {
        return "log4j2:.*" + lvl.name() + ".*" + CSNAME + (julBridged ? "\\._JUL" : "")
                + ".*" + CSNAME + "/\\w*" + methodPartRE + "\\w*/.*"
                + msgPartRE + ".*\n"		// use real \n at end here for better error output
                + (exceptionClassAndMsgRE == null  ?  ""
                    :  ".*" + exceptionClassAndMsgRE + ".*\\n(\tat .*\\n)*\\n?");
    }



    @Test
    public void test1SimpleLoggings1Jul() {
        julLog.info("Test-'Info'-Log with JUL");
        julLog.fine("Test-'Fine'-Log with JUL");
        julLog.finest("Test-'Finest'-Log with JUL");		// should not be logged because JUL-level is FINER
        julLog.warning("Test-'Warn'-Log with JUL");		// thus add another log afterwards to allow checking
        String methodRE = "SimpleLoggings";
        assertSysoutMatches(
                log4j(org.apache.logging.log4j.Level.INFO, true, methodRE, "'Info'-Log with JUL", null)
                + jul(java.util.logging.Level.INFO, methodRE, "'Info'-Log with JUL", null)
                + log4j(org.apache.logging.log4j.Level.DEBUG, true, methodRE, "'Fine'-Log with JUL", null)
                + jul(java.util.logging.Level.FINE, methodRE, "'Fine'-Log with JUL", null)
                // no finest/trace
                + log4j(org.apache.logging.log4j.Level.WARN, true, methodRE, "'Warn'-Log with JUL", null)
                + jul(java.util.logging.Level.WARNING, methodRE, "'Warn'-Log with JUL", null)
                );
    }

    @Test
    public void test1SimpleLoggings2Log4jDirect() {
        log4jLog.info("Test-'Info'-Log with log4j2");
        log4jLog.debug("Test-'Debug'-Log with log4j2");
        log4jLog.trace("Test-'Trace'-Log with log4j2");
        String methodRE = "SimpleLoggings";
        assertSysoutMatches(
                log4j(org.apache.logging.log4j.Level.INFO, false, methodRE, "'Info'-Log with log4j2", null)
                + log4j(org.apache.logging.log4j.Level.DEBUG, false, methodRE, "'Debug'-Log with log4j2", null)
                + log4j(org.apache.logging.log4j.Level.TRACE, false, methodRE, "'Trace'-Log with log4j2", null)
                );
    }


    @Test
    public void test2SubMethod() {
        subMethodWithLogs();					// location info is sub method now
        String methodRE = "subMethodWithLogs";
        assertSysoutMatches(
                log4j(org.apache.logging.log4j.Level.DEBUG, true, methodRE, "'Fine'-Log with JUL in subMethod", null)
                + jul(java.util.logging.Level.FINE, methodRE, "'Fine'-Log with JUL in subMethod", null)
                + log4j(org.apache.logging.log4j.Level.INFO, false, methodRE, "'Info'-Log with log4j2 in subMethod", null)
                );
    }
    private void subMethodWithLogs() {
        julLog.fine("Test-'Fine'-Log with JUL in subMethod");
        log4jLog.info("Test-'Info'-Log with log4j2 in subMethod");
    }


    @Test
    public void test3JulFlow1() {
        // note: manually given source information get lost in log4j!
        julLog.entering("enteringExampleClassParam", "enteringExampleMethodParam");
        String methodRE = "JulFlow";
        assertSysoutMatches(
                log4j(org.apache.logging.log4j.Level.TRACE, true, methodRE, "ENTRY", null)
                + jul(java.util.logging.Level.FINER, "enteringExampleClassParam enteringExampleMethodParam", "ENTRY", null)
                );
    }

    @Test
    public void test3JulFlow2() {
        // note: manually given source information get lost in log4j!
        julLog.entering("enteringExampleClassParam", "enteringExampleMethodParam_withParams",
                new Object[] {"with some", "parameters", 42} );
        String methodRE = "JulFlow";
        assertSysoutMatches(
                log4j(org.apache.logging.log4j.Level.TRACE, true, methodRE,
                        "ENTRY.*with some.*param.*42", null)
                + jul(java.util.logging.Level.FINER, "enteringExampleClassParam enteringExampleMethodParam_withParams",
                        "ENTRY.*with some.*param.*42", null)
                );
    }

    @Test
    public void test3JulFlow3() {
        // note: manually given source information get lost in log4j!
        julLog.exiting("exitingExampleClassParam", "exitingExampleMethodParam",
                Arrays.asList("array of Strings", "that are the exit", "result"));
        String methodRE = "JulFlow";
        assertSysoutMatches(
                log4j(org.apache.logging.log4j.Level.TRACE, true, methodRE,
                        "RETURN.*array of Str.*that are.*result", null)
                + jul(java.util.logging.Level.FINER, "exitingExampleClassParam exitingExampleMethodParam",
                        "RETURN.*array of Str.*that are.*result", null)
                );
    }

    @Test
    public void test3JulFlow4() {
        // note: manually given source information get lost in log4j!
        julLog.throwing("throwingExampleClassParam", "throwingExampleMethodParam",
                new IllegalStateException("ONLY TEST for JUL-throwing()"));
        String methodRE = "JulFlow";
        assertSysoutMatches(
                log4j(org.apache.logging.log4j.Level.TRACE, true, methodRE,
                        "THROW",  "IllegalStateException.*ONLY TEST for JUL-throwing")
                + jul(java.util.logging.Level.FINER, "throwingExampleClassParam throwingExampleMethodParam",
                        "THROW",  "IllegalStateException.*ONLY TEST for JUL-throwing")
                );
    }



    @Test
    public void test4JulSpecials1() {
        julLog.log(Level.WARNING, "JUL-Test via log() as warning with exception",
                new java.util.zip.DataFormatException("ONLY TEST for JUL.log()"));
        String methodRE = "JulSpecials";
        assertSysoutMatches(
                log4j(org.apache.logging.log4j.Level.WARN, true, methodRE,
                        "JUL-Test via log\\(\\) as warning",  "DataFormatException.*ONLY TEST for JUL")
                + jul(java.util.logging.Level.WARNING, methodRE, "JUL-Test via log\\(\\) as warning",
                        "DataFormatException.*ONLY TEST for JUL")
                );
    }

    @Test
    public void test4JulSpecials2() {
        // test with MessageFormat
        julLog.log(Level.INFO, "JUL-Test via log() with parameters (0={0}, 1={1}, 2={2,number,##000.0})",
                new Object[] {"a", "b", 42} );
        String methodRE = "JulSpecials";
        assertSysoutMatches(
                log4j(org.apache.logging.log4j.Level.INFO, true, methodRE,
                        "JUL-Test via log\\(\\) with parameters \\(0=a, 1=b, 2=042.0\\)", null)
                + jul(java.util.logging.Level.INFO, methodRE,
                        "JUL-Test via log\\(\\) with parameters \\(0=a, 1=b, 2=042.0\\)", null)
                );
    }

    // no test for logrb(ResourceBundle)-case as this is very specific and seldom used (in
    // my opinion); and it does not add any real thing to test here

}
