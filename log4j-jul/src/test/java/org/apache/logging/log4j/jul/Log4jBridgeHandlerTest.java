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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the Log4jBridgeHandler.
 * <p>
 *     Requires some configurations:
 * </p>
 * <ul>
 *     <li>
 *         The {@code java.util.logging.config.file} system property must be set to the location of the
 *         {@code Log4jBridgeHandlerTest-jul.properties} resource.
 *     </li>
 *     <li>
 *         The Log4j Core configuration file must be set to the location of the
 *         {@code Log4jBridgeHandlerTest-log4j2.xml} resource.
 *     </li>
 * </ul>
 */
class Log4jBridgeHandlerTest {

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

    @BeforeAll
    static void beforeClass() {
        prevSysErrStream = System.err;
        System.setErr(new PrintStream(sysoutBytes, true));
    }

    @AfterAll
    static void afterClass() {
        // reset sysout/err to original value
        System.setErr(prevSysErrStream);
    }

    @BeforeEach
    void beforeTest() {
        // reset sysout collector
        sysoutBytes.reset();
    }

    /** Assert that captured sysout matches given regexp (any text may follow afterwards). */
    private void assertSysoutMatches(String regex) {
        // String logOutput = systemOutRule.getLogWithNormalizedLineSeparator();
        String logOutput = sysoutBytes.toString();
        logOutput = logOutput.replace("\r\n", "\n");
        regex = regex + "(.|\\n)*"; // allow any text with NL afterwards
        Assertions.assertThat(logOutput).matches(regex);
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
    void test1SimpleLoggings1Jul() {
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
    void test1SimpleLoggings2Log4jDirect() {
        log4jLog.info("Test-'Info'-Log with log4j2");
        log4jLog.debug("Test-'Debug'-Log with log4j2");
        log4jLog.trace("Test-'Trace'-Log with log4j2");
        final String methodRE = "SimpleLoggings2Log4jDirect";
        assertSysoutMatches(log4j(org.apache.logging.log4j.Level.INFO, false, methodRE, "'Info'-Log with log4j2", null)
                + log4j(org.apache.logging.log4j.Level.DEBUG, false, methodRE, "'Debug'-Log with log4j2", null)
                + log4j(org.apache.logging.log4j.Level.TRACE, false, methodRE, "'Trace'-Log with log4j2", null));
    }

    @Test
    void test2SubMethod() {
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
    void test3JulFlow1() {
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
    void test3JulFlow2() {
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
    void test3JulFlow3() {
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
    void test3JulFlow4() {
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
    void test4JulSpecials1() {
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
    void test4JulSpecials2() {
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
}
