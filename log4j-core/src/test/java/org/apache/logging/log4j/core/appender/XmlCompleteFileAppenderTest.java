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
package org.apache.logging.log4j.core.appender;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.categories.Layouts;
import org.apache.logging.log4j.core.CoreLoggerContexts;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.selector.CoreContextSelectors;
import org.apache.logging.log4j.junit.CleanFiles;
import org.apache.logging.log4j.junit.LoggerContextRule;
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
        try (final BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            line1 = reader.readLine();
            line2 = reader.readLine();
            reader.readLine(); // ignore the empty line after the <Events> root
            line3 = reader.readLine();
            line4 = reader.readLine();
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
        final String msg4 = logMsg;
        assertTrue("line4 incorrect: [" + line4 + "], does not contain: [" + msg4 + ']', line4.contains(msg4));

        final String location = "testFlushAtEndOfBatch";
        assertTrue("no location", !line1.contains(location));
    }

    /**
     * Test the indentation of the Events XML.
     * <p>Expected Events XML is as below.</p>
     * <pre>
&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;
&lt;Events xmlns=&quot;http://logging.apache.org/log4j/2.0/events&quot;&gt;

  &lt;Event xmlns=&quot;http://logging.apache.org/log4j/2.0/events&quot; timeMillis=&quot;1460974522088&quot; thread=&quot;main&quot; level=&quot;INFO&quot; loggerName=&quot;com.foo.Bar&quot; endOfBatch=&quot;false&quot; loggerFqcn=&quot;org.apache.logging.log4j.spi.AbstractLogger&quot; threadId=&quot;11&quot; threadPriority=&quot;5&quot;&gt;
    &lt;Message&gt;First Msg tag must be in level 2 after correct indentation&lt;/Message&gt;
  &lt;/Event&gt;

  &lt;Event xmlns=&quot;http://logging.apache.org/log4j/2.0/events&quot; timeMillis=&quot;1460974522089&quot; thread=&quot;main&quot; level=&quot;INFO&quot; loggerName=&quot;com.foo.Bar&quot; endOfBatch=&quot;true&quot; loggerFqcn=&quot;org.apache.logging.log4j.spi.AbstractLogger&quot; threadId=&quot;11&quot; threadPriority=&quot;5&quot;&gt;
    &lt;Message&gt;Second Msg tag must also be in level 2 after correct indentation&lt;/Message&gt;
  &lt;/Event&gt;
&lt;/Events&gt;
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

        final String[] lines = new String[9];

        try (final BufferedReader reader = new BufferedReader(new FileReader(logFile))) {

            int usefulLinesIndex = 0;
            String readLine;
            while((readLine = reader.readLine()) != null) {

                if (!"".equals(readLine.trim())) {
                    lines[usefulLinesIndex] = readLine;
                    usefulLinesIndex++;
                }
            }
        } finally {
            logFile.delete();
        }

        String currentLine = lines[0];
        assertFalse("line1 incorrect: [" + currentLine + "], must have no indentation", currentLine.startsWith(" "));
        // <EVENTS
        currentLine = lines[1];
        assertFalse("line2 incorrect: [" + currentLine + "], must have no indentation", currentLine.startsWith(" "));
        // <EVENT
        currentLine = lines[2];
        assertTrue("line3 incorrect: [" + currentLine + "], must have two-space indentation", currentLine.startsWith("  "));
        assertFalse("line3 incorrect: [" + currentLine + "], must not have more than two-space indentation", currentLine.startsWith("   "));
        // <MSG
        currentLine = lines[3];
        assertTrue("line4 incorrect: [" + currentLine + "], must have four-space indentation", currentLine.startsWith("    "));
        assertFalse("line4 incorrect: [" + currentLine + "], must not have more than four-space indentation", currentLine.startsWith("     "));
        // </EVENT
        currentLine = lines[4];
        assertTrue("line5 incorrect: [" + currentLine + "], must have two-space indentation", currentLine.startsWith("  "));
        assertFalse("line5 incorrect: [" + currentLine + "], must not have more than two-space indentation", currentLine.startsWith("   "));

        // <EVENT
        currentLine = lines[5];
        assertTrue("line6 incorrect: [" + currentLine + "], must have two-space indentation", currentLine.startsWith("  "));
        assertFalse("line6 incorrect: [" + currentLine + "], must not have more than two-space indentation", currentLine.startsWith("   "));
        // <MSG
        currentLine = lines[6];
        assertTrue("line7 incorrect: [" + currentLine + "], must have four-space indentation", currentLine.startsWith("    "));
        assertFalse("line7 incorrect: [" + currentLine + "], must not have more than four-space indentation", currentLine.startsWith("     "));
        // </EVENT
        currentLine = lines[7];
        assertTrue("line8 incorrect: [" + currentLine + "], must have two-space indentation", currentLine.startsWith("  "));
        assertFalse("line8 incorrect: [" + currentLine + "], must not have more than two-space indentation", currentLine.startsWith("   "));
        // </EVENTS
        currentLine = lines[8];
        assertFalse("line9 incorrect: [" + currentLine + "], must have no indentation", currentLine.startsWith(" "));
    }
}
