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
package org.apache.logging.log4j.core.parser;

import org.apache.logging.log4j.core.LogEvent;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class JsonLogEventParserTest extends LogEventParserTest {

    private JsonLogEventParser parser;

    private static final String JSON = "{\n" +
            "  \"timeMillis\" : 1493121664118,\n" +
            "  \"instant\":{\"epochSecond\":1493121664,\"nanoOfSecond\":118000000},\n" +
            "  \"thread\" : \"main\",\n" +
            "  \"threadId\" : 1,\n" +
            "  \"threadPriority\" : 5,\n" +
            "  \"level\" : \"INFO\",\n" +
            "  \"loggerName\" : \"HelloWorld\",\n" +
            "  \"marker\" : {\n" +
            "    \"name\" : \"child\",\n" +
            "    \"parents\" : [ {\n" +
            "      \"name\" : \"parent\",\n" +
            "      \"parents\" : [ {\n" +
            "        \"name\" : \"grandparent\"\n" +
            "      } ]\n" +
            "    } ]\n" +
            "  },\n" +
            "  \"message\" : \"Hello, world!\",\n" +
            "  \"thrown\" : {\n" +
            "    \"commonElementCount\" : 0,\n" +
            "    \"message\" : \"error message\",\n" +
            "    \"name\" : \"java.lang.RuntimeException\",\n" +
            "    \"extendedStackTrace\" : [ {\n" +
            "      \"class\" : \"logtest.Main\",\n" +
            "      \"method\" : \"main\",\n" +
            "      \"file\" : \"Main.java\",\n" +
            "      \"line\" : 29,\n" +
            "      \"exact\" : true,\n" +
            "      \"location\" : \"classes/\",\n" +
            "      \"version\" : \"?\"\n" +
            "    } ]\n" +
            "  },\n" +
            "  \"contextStack\" : [ \"one\", \"two\" ],\n" +
            "  \"loggerFqcn\" : \"org.apache.logging.log4j.spi.AbstractLogger\",\n" +
            "  \"endOfBatch\" : false,\n" +
            "  \"contextMap\" : {\n" +
            "    \"bar\" : \"BAR\",\n" +
            "    \"foo\" : \"FOO\"\n" +
            "  },\n" +
            "  \"source\" : {\n" +
            "    \"class\" : \"logtest.Main\",\n" +
            "    \"method\" : \"main\",\n" +
            "    \"file\" : \"Main.java\",\n" +
            "    \"line\" : 29\n" +
            "  }\n" +
            "}";

    @Before
    public void setup() {
        parser = new JsonLogEventParser();
    }

    @Test
    public void testString() throws ParseException {
        final LogEvent logEvent = parser.parseFrom(JSON);
        assertLogEvent(logEvent);
    }

    @Test(expected = ParseException.class)
    public void testStringEmpty() throws ParseException {
        parser.parseFrom("");
    }

    @Test(expected = ParseException.class)
    public void testStringInvalidJson() throws ParseException {
        parser.parseFrom("foobar");
    }

    @Test(expected = ParseException.class)
    public void testStringJsonArray() throws ParseException {
        parser.parseFrom("[]");
    }

    @Test
    public void testEmptyObject() throws ParseException {
        parser.parseFrom("{}");
    }

    @Test(expected = ParseException.class)
    public void testStringWrongPropertyType() throws ParseException {
        parser.parseFrom("{\"threadId\":\"foobar\"}");
    }

    @Test
    public void testStringIgnoreInvalidProperty() throws ParseException {
        parser.parseFrom("{\"foo\":\"bar\"}");
    }

    @Test
    public void testByteArray() throws ParseException {
        final LogEvent logEvent = parser.parseFrom(JSON.getBytes(StandardCharsets.UTF_8));
        assertLogEvent(logEvent);
    }

    @Test
    public void testByteArrayOffsetLength() throws ParseException {
        final byte[] bytes = ("abc" + JSON + "def").getBytes(StandardCharsets.UTF_8);
        final LogEvent logEvent = parser.parseFrom(bytes, 3, bytes.length - 6);
        assertLogEvent(logEvent);
    }

}
