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
package org.apache.logging.log4j.jackson.xml.parser;

import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.parser.AbstractLogEventParserTest;
import org.apache.logging.log4j.core.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

public class XmlLogEventParserTest extends AbstractLogEventParserTest {

    private static final String XML = "<Event xmlns=\"http://logging.apache.org/log4j/2.0/events\"\n" +
            "       timeMillis=\"1493121664118\"\n" +
            "       level=\"INFO\"\n" +
            "       loggerName=\"HelloWorld\"\n" +
            "       endOfBatch=\"false\"\n" +
            "       thread=\"main\"\n" +
            "       loggerFqcn=\"org.apache.logging.log4j.spi.AbstractLogger\"\n" +
            "       threadId=\"1\"\n" +
            "       threadPriority=\"5\">\n" +
            "  <Instant epochSecond=\"1493121664\" nanoOfSecond=\"118000000\"/>\n" +
            "  <Marker name=\"child\">\n" +
            "    <Parents>\n" +
            "      <Marker name=\"parent\">\n" +
            "        <Parents>\n" +
            "          <Marker name=\"grandparent\"/>\n" +
            "        </Parents>\n" +
            "      </Marker>\n" +
            "    </Parents>\n" +
            "  </Marker>\n" +
            "  <Message>Hello, world!</Message>\n" +
            "  <ContextMap>\n" +
            "    <item key=\"bar\" value=\"BAR\"/>\n" +
            "    <item key=\"foo\" value=\"FOO\"/>\n" +
            "  </ContextMap>\n" +
            "  <ContextStack>\n" +
            "    <ContextStackItem>one</ContextStackItem>\n" +
            "    <ContextStackItem>two</ContextStackItem>\n" +
            "  </ContextStack>\n" +
            "  <Source\n" +
            "      class=\"logtest.Main\"\n" +
            "      method=\"main\"\n" +
            "      file=\"Main.java\"\n" +
            "      line=\"29\"/>\n" +
            "  <Thrown commonElementCount=\"0\" message=\"error message\" name=\"java.lang.RuntimeException\">\n" +
            "    <ExtendedStackTrace>\n" +
            "      <ExtendedStackTraceItem\n" +
            "          class=\"logtest.Main\"\n" +
            "          method=\"main\"\n" +
            "          file=\"Main.java\"\n" +
            "          line=\"29\"\n" +
            "          exact=\"true\"\n" +
            "          location=\"classes/\"\n" +
            "          version=\"?\"/>\n" +
            "    </ExtendedStackTrace>\n" +
            "  </Thrown>\n" +
            "</Event>";

    private XmlLogEventParser parser;

    @Before
    public void setup() {
        parser = new XmlLogEventParser();
    }

    @Test
    public void testByteArray() throws ParseException {
        final LogEvent logEvent = parser.parseFrom(XML.getBytes(StandardCharsets.UTF_8));
        assertLogEvent(logEvent);
    }

    @Test
    public void testByteArrayOffsetLength() throws ParseException {
        final byte[] bytes = ("abc" + XML + "def").getBytes(StandardCharsets.UTF_8);
        final LogEvent logEvent = parser.parseFrom(bytes, 3, bytes.length - 6);
        assertLogEvent(logEvent);
    }

    @Test
    public void testEmptyObject() throws ParseException {
        parser.parseFrom("<Event></Event>");
    }

    @Test
    public void testString() throws ParseException {
        final LogEvent logEvent = parser.parseFrom(XML);
        assertLogEvent(logEvent);
    }

    @Test(expected = ParseException.class)
    public void testStringEmpty() throws ParseException {
        parser.parseFrom("");
    }

    @Test
    public void testStringIgnoreInvalidProperty() throws ParseException {
        parser.parseFrom("<Event><foo>bar</foo></Event>");
    }

    @Test(expected = ParseException.class)
    public void testStringInvalidXml() throws ParseException {
        parser.parseFrom("foobar");
    }

    @Test(expected = ParseException.class)
    public void testStringWrongPropertyType() throws ParseException {
        parser.parseFrom("<Event><Instant epochSecond=\"bar\">foobar</Instant></Event>");
    }

    @Test
    public void testTimeMillisIgnored() throws ParseException {
        parser.parseFrom("<Event><timeMillis>foobar</timeMillis></Event>");
    }

}
