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
package org.apache.log4j.helpers;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.util.Compare;
import org.junit.Ignore;

/**
 * Test case for helpers/PatternParser.java. Tests the various conversion patterns supported by PatternParser. This test
 * class tests PatternParser via the PatternLayout class which uses it.
 */
@Ignore("WIP")
public class PatternParserTestCase extends TestCase {

    static String OUTPUT_FILE = "target/PatternParser";
    static String WITNESS_FILE = "target/witness/PatternParser";

    static String msgPattern = "%m%n";

    public static Test suite() {
        final TestSuite suite = new TestSuite();
        suite.addTest(new PatternParserTestCase("mdcPattern"));
        return suite;
    }

    Logger root;

    Logger logger;

    public PatternParserTestCase(final String name) {
        super(name);
    }

    /**
     * Test case for MDC conversion pattern.
     */
    public void mdcPattern() throws Exception {

        final String mdcMsgPattern1 = "%m : %X%n";
        final String mdcMsgPattern2 = "%m : %X{key1}%n";
        final String mdcMsgPattern3 = "%m : %X{key2}%n";
        final String mdcMsgPattern4 = "%m : %X{key3}%n";
        final String mdcMsgPattern5 = "%m : %X{key1},%X{key2},%X{key3}%n";

        // set up appender
        final PatternLayout layout = new PatternLayout(msgPattern);
        final Appender appender = new FileAppender(layout, OUTPUT_FILE + "_mdc", false);

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setLevel(Level.DEBUG);

        // output starting message
        root.debug("starting mdc pattern test");

        layout.setConversionPattern(mdcMsgPattern1);
        root.debug("empty mdc, no key specified in pattern");

        layout.setConversionPattern(mdcMsgPattern2);
        root.debug("empty mdc, key1 in pattern");

        layout.setConversionPattern(mdcMsgPattern3);
        root.debug("empty mdc, key2 in pattern");

        layout.setConversionPattern(mdcMsgPattern4);
        root.debug("empty mdc, key3 in pattern");

        layout.setConversionPattern(mdcMsgPattern5);
        root.debug("empty mdc, key1, key2, and key3 in pattern");

        MDC.put("key1", "value1");
        MDC.put("key2", "value2");

        layout.setConversionPattern(mdcMsgPattern1);
        root.debug("filled mdc, no key specified in pattern");

        layout.setConversionPattern(mdcMsgPattern2);
        root.debug("filled mdc, key1 in pattern");

        layout.setConversionPattern(mdcMsgPattern3);
        root.debug("filled mdc, key2 in pattern");

        layout.setConversionPattern(mdcMsgPattern4);
        root.debug("filled mdc, key3 in pattern");

        layout.setConversionPattern(mdcMsgPattern5);
        root.debug("filled mdc, key1, key2, and key3 in pattern");

        MDC.remove("key1");
        MDC.remove("key2");

        layout.setConversionPattern(msgPattern);
        root.debug("finished mdc pattern test");

        assertTrue(Compare.compare(OUTPUT_FILE + "_mdc", WITNESS_FILE + "_mdc"));
    }

    public void setUp() {
        root = Logger.getRootLogger();
        root.removeAllAppenders();
    }

    public void tearDown() {
        root.getLoggerRepository().resetConfiguration();
    }
}
