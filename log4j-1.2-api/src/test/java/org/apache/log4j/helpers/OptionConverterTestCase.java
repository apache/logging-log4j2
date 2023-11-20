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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfiguratorTest;
import org.apache.log4j.xml.XLevel;
import org.junit.Ignore;

/**
 * Test variable substitution code.
 *
 * @since 1.0
 */
@Ignore("WIP")
public class OptionConverterTestCase extends TestCase {

    Properties props;

    public OptionConverterTestCase(final String name) {
        super(name);
    }

    public void setUp() {
        props = new Properties();
        props.put("TOTO", "wonderful");
        props.put("key1", "value1");
        props.put("key2", "value2");
        // Log4J will NPE without this:
        props.put("line.separator", System.getProperty("line.separator"));
        // Log4J will throw an Error without this:
        props.put("java.home", System.getProperty("java.home"));
        System.setProperties(props);
    }

    public void tearDown() {
        props = null;
        LogManager.resetConfiguration();
    }

    public void varSubstTest1() {
        String r;

        r = OptionConverter.substVars("hello world.", null);
        assertEquals("hello world.", r);

        r = OptionConverter.substVars("hello ${TOTO} world.", null);

        assertEquals("hello wonderful world.", r);
    }

    public void varSubstTest2() {
        String r;

        r = OptionConverter.substVars("Test2 ${key1} mid ${key2} end.", null);
        assertEquals("Test2 value1 mid value2 end.", r);
    }

    public void varSubstTest3() {
        String r;

        r = OptionConverter.substVars("Test3 ${unset} mid ${key1} end.", null);
        assertEquals("Test3  mid value1 end.", r);
    }

    public void varSubstTest4() {
        final String val = "Test4 ${incomplete ";
        try {
            OptionConverter.substVars(val, null);
        } catch (IllegalArgumentException e) {
            final String errorMsg = e.getMessage();
            // System.out.println('['+errorMsg+']');
            assertEquals('"' + val + "\" has no closing brace. Opening brace at position 6.", errorMsg);
        }
    }

    public void varSubstTest5() {
        final Properties props = new Properties();
        props.put("p1", "x1");
        props.put("p2", "${p1}");
        final String res = OptionConverter.substVars("${p2}", props);
        System.out.println("Result is [" + res + "].");
        assertEquals(res, "x1");
    }

    /**
     * Tests configuring Log4J from an InputStream.
     *
     * @since 1.2.17
     */
    public void testInputStream() throws IOException {
        final File file = new File("src/test/resources/log4j1-1.2.17/input/filter1.properties");
        assertTrue(file.exists());
        try (final FileInputStream inputStream = new FileInputStream(file)) {
            OptionConverter.selectAndConfigure(inputStream, null, LogManager.getLoggerRepository());
        }
        new PropertyConfiguratorTest().validateNested();
    }

    public void toLevelTest1() {
        final String val = "INFO";
        final Level p = OptionConverter.toLevel(val, null);
        assertEquals(p, Level.INFO);
    }

    public void toLevelTest2() {
        final String val = "INFO#org.apache.log4j.xml.XLevel";
        final Level p = OptionConverter.toLevel(val, null);
        assertEquals(p, Level.INFO);
    }

    public void toLevelTest3() {
        final String val = "TRACE#org.apache.log4j.xml.XLevel";
        final Level p = OptionConverter.toLevel(val, null);
        assertEquals(p, XLevel.TRACE);
    }

    public void toLevelTest4() {
        final String val = "TR#org.apache.log4j.xml.XLevel";
        final Level p = OptionConverter.toLevel(val, null);
        assertEquals(p, null);
    }

    public void toLevelTest5() {
        final String val = "INFO#org.apache.log4j.xml.TOTO";
        final Level p = OptionConverter.toLevel(val, null);
        assertEquals(p, null);
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite();
        suite.addTest(new OptionConverterTestCase("varSubstTest5"));
        suite.addTest(new OptionConverterTestCase("varSubstTest1"));
        suite.addTest(new OptionConverterTestCase("varSubstTest2"));
        suite.addTest(new OptionConverterTestCase("varSubstTest3"));
        suite.addTest(new OptionConverterTestCase("varSubstTest4"));

        suite.addTest(new OptionConverterTestCase("testInputStream"));

        suite.addTest(new OptionConverterTestCase("toLevelTest1"));
        suite.addTest(new OptionConverterTestCase("toLevelTest2"));
        suite.addTest(new OptionConverterTestCase("toLevelTest3"));
        suite.addTest(new OptionConverterTestCase("toLevelTest4"));
        suite.addTest(new OptionConverterTestCase("toLevelTest5"));
        return suite;
    }
}
