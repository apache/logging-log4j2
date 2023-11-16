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
package org.apache.logging.log4j.core.appender;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.selector.CoreContextSelectors;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.apache.logging.log4j.core.test.categories.Layouts;
import org.apache.logging.log4j.core.test.junit.CleanFiles;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests a "complete" XML file a.k.a. a well-formed XML file.
 */
@RunWith(Parameterized.class)
@Category(Layouts.Xml.class)
public class XmlCompleteFileAppenderTest {

    public XmlCompleteFileAppenderTest(final Class<ContextSelector> contextSelector) {
        this.loggerContextRule = new LoggerContextRule("XmlCompleteFileAppenderTest.xml", contextSelector);
        this.cleanFiles = new CleanFiles(logFile);
        this.ruleChain = RuleChain.outerRule(cleanFiles).around(loggerContextRule);
    }

    @Parameters(name = "{0}")
    public static Class<?>[] getParameters() {
        return CoreContextSelectors.CLASSES;
    }

    private final File logFile = new File("target", "XmlCompleteFileAppenderTest.log");
    private final LoggerContextRule loggerContextRule;
    private final CleanFiles cleanFiles;

    @Rule
    public RuleChain ruleChain;

    @Test
    public void testFlushAtEndOfBatch() throws Exception {
        final Logger logger = this.loggerContextRule.getLogger("com.foo.Bar");
        final String logMsg = "Message flushed with immediate flush=false";
        logger.info(logMsg);
        CoreLoggerContexts.stopLoggerContext(false, logFile); // stop async thread

        String line1;
        String line2;
        String line3;
        String line4;
        String line5;
        try (final BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            line1 = reader.readLine();
            line2 = reader.readLine();
            reader.readLine(); // ignore the empty line after the <Events> root
            line3 = reader.readLine();
            line4 = reader.readLine();
            line5 = reader.readLine();
        } finally {
            logFile.delete();
        }
        assertNotNull("line1", line1);
        final String msg1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        assertTrue("line1 incorrect: [" + line1 + "], does not contain: [" + msg1 + ']', line1.equals(msg1));

        assertNotNull("line2", line2);
        final String msg2 = "<Events xmlns=\"http://logging.apache.org/log4j/2.0/events\">";
        assertTrue("line2 incorrect: [" + line2 + "], does not contain: [" + msg2 + ']', line2.equals(msg2));

        assertNotNull("line3", line3);
        final String msg3 = "<Event ";
        assertTrue("line3 incorrect: [" + line3 + "], does not contain: [" + msg3 + ']', line3.contains(msg3));

        assertNotNull("line4", line4);
        final String msg4 = "<Instant epochSecond=";
        assertTrue("line4 incorrect: [" + line4 + "], does not contain: [" + msg4 + ']', line4.contains(msg4));

        assertNotNull("line5", line5);
        final String msg5 = logMsg;
        assertTrue("line5 incorrect: [" + line5 + "], does not contain: [" + msg5 + ']', line5.contains(msg5));

        final String location = "testFlushAtEndOfBatch";
        assertTrue("no location", !line1.contains(location));
    }

    /**
     * Test the indentation of the Events XML.
     * <p>Expected Events XML is as below.</p>
     * <pre>
     * &lt;?xml version="1.0" encoding="UTF-8"?>
     * &lt;Events xmlns="http://logging.apache.org/log4j/2.0/events">
     *
     * &lt;Event xmlns="http://logging.apache.org/log4j/2.0/events" thread="main" level="INFO" loggerName="com.foo.Bar" endOfBatch="true" loggerFqcn="org.apache.logging.log4j.spi.AbstractLogger" threadId="12" threadPriority="5">
     * &lt;Instant epochSecond="1515889414" nanoOfSecond="144000000" epochMillisecond="1515889414144" nanoOfMillisecond="0"/>
     * &lt;Message>First Msg tag must be in level 2 after correct indentation&lt;/Message>
     * &lt;/Event>
     *
     * &lt;Event xmlns="http://logging.apache.org/log4j/2.0/events" thread="main" level="INFO" loggerName="com.foo.Bar" endOfBatch="true" loggerFqcn="org.apache.logging.log4j.spi.AbstractLogger" threadId="12" threadPriority="5">
     * &lt;Instant epochSecond="1515889414" nanoOfSecond="144000000" epochMillisecond="1515889414144" nanoOfMillisecond="0"/>
     * &lt;Message>Second Msg tag must also be in level 2 after correct indentation&lt;/Message>
     * &lt;/Event>
     * &lt;/Events>
     * </pre>
     * @throws Exception
     */
    @Test
    public void testChildElementsAreCorrectlyIndented() throws Exception {
        final Logger logger = this.loggerContextRule.getLogger("com.foo.Bar");
        final String firstLogMsg = "First Msg tag must be in level 2 after correct indentation";
        logger.info(firstLogMsg);
        final String secondLogMsg = "Second Msg tag must also be in level 2 after correct indentation";
        logger.info(secondLogMsg);
        CoreLoggerContexts.stopLoggerContext(false, logFile); // stop async thread

        final int[] indentations = {
            0, // "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            0, // "<Events xmlns=\"http://logging.apache.org/log4j/2.0/events\">\n"
            -1, // empty
            2, // "  <Event xmlns=\"http://logging.apache.org/log4j/2.0/events\" thread=\"main\" level=\"INFO\"
            // loggerName=\"com.foo.Bar\" endOfBatch=\"true\"
            // loggerFqcn=\"org.apache.logging.log4j.spi.AbstractLogger\" threadId=\"12\" threadPriority=\"5\">\n"
            4, // "    <Instant epochSecond=\"1515889414\" nanoOfSecond=\"144000000\" epochMillisecond=\"1515889414144\"
            // nanoOfMillisecond=\"0\"/>\n"
            4, // "    <Message>First Msg tag must be in level 2 after correct indentation</Message>\n" +
            2, // "  </Event>\n"
            -1, // empty
            2, // "  <Event xmlns=\"http://logging.apache.org/log4j/2.0/events\" thread=\"main\" level=\"INFO\"
            // loggerName=\"com.foo.Bar\" endOfBatch=\"true\"
            // loggerFqcn=\"org.apache.logging.log4j.spi.AbstractLogger\" threadId=\"12\" threadPriority=\"5\">\n" +
            4, // "    <Instant epochSecond=\"1515889414\" nanoOfSecond=\"144000000\" epochMillisecond=\"1515889414144\"
            // nanoOfMillisecond=\"0\"/>\n" +
            4, // "    <Message>Second Msg tag must also be in level 2 after correct indentation</Message>\n" +
            2, // "  </Event>\n" +
            0, // "</Events>\n";
        };
        final List<String> lines1 = Files.readAllLines(logFile.toPath(), Charset.forName("UTF-8"));

        assertEquals("number of lines", indentations.length, lines1.size());
        for (int i = 0; i < indentations.length; i++) {
            final String line = lines1.get(i);
            if (line.trim().isEmpty()) {
                assertEquals(-1, indentations[i]);
            } else {
                final String padding = "        ".substring(0, indentations[i]);
                assertTrue(
                        "Expected " + indentations[i] + " leading spaces but got: " + line, line.startsWith(padding));
            }
        }
    }
}
