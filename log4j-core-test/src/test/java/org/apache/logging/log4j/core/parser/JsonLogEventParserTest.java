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
package org.apache.logging.log4j.core.parser;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JsonLogEventParserTest extends LogEventParserTest {

    private JsonLogEventParser parser;

    private static final String JSON = "{\n" + "  \"timeMillis\" : 1493121664118,\n"
            + "  \"instant\":{\"epochSecond\":1493121664,\"nanoOfSecond\":118000000},\n"
            + "  \"thread\" : \"main\",\n"
            + "  \"threadId\" : 1,\n"
            + "  \"threadPriority\" : 5,\n"
            + "  \"level\" : \"INFO\",\n"
            + "  \"loggerName\" : \"HelloWorld\",\n"
            + "  \"marker\" : {\n"
            + "    \"name\" : \"child\",\n"
            + "    \"parents\" : [ {\n"
            + "      \"name\" : \"parent\",\n"
            + "      \"parents\" : [ {\n"
            + "        \"name\" : \"grandparent\"\n"
            + "      } ]\n"
            + "    } ]\n"
            + "  },\n"
            + "  \"message\" : \"Hello, world!\",\n"
            + "  \"thrown\" : {\n"
            + "    \"commonElementCount\" : 0,\n"
            + "    \"message\" : \"error message\",\n"
            + "    \"name\" : \"java.lang.RuntimeException\",\n"
            + "    \"extendedStackTrace\" : [ {\n"
            + "      \"class\" : \"logtest.Main\",\n"
            + "      \"method\" : \"main\",\n"
            + "      \"file\" : \"Main.java\",\n"
            + "      \"line\" : 29,\n"
            + "      \"exact\" : true,\n"
            + "      \"location\" : \"classes/\",\n"
            + "      \"version\" : \"?\"\n"
            + "    } ]\n"
            + "  },\n"
            + "  \"contextStack\" : [ \"one\", \"two\" ],\n"
            + "  \"loggerFqcn\" : \"org.apache.logging.log4j.spi.AbstractLogger\",\n"
            + "  \"endOfBatch\" : false,\n"
            + "  \"contextMap\" : {\n"
            + "    \"bar\" : \"BAR\",\n"
            + "    \"foo\" : \"FOO\"\n"
            + "  },\n"
            + "  \"source\" : {\n"
            + "    \"class\" : \"logtest.Main\",\n"
            + "    \"method\" : \"main\",\n"
            + "    \"file\" : \"Main.java\",\n"
            + "    \"line\" : 29\n"
            + "  }\n"
            + "}";

    @BeforeEach
    void setup() {
        parser = new JsonLogEventParser();
    }

    @Test
    void testString() throws ParseException {
        final LogEvent logEvent = parser.parseFrom(JSON);
        assertLogEvent(logEvent);
    }

    @Test
    void testStringEmpty() {
        assertThrows(ParseException.class, () -> parser.parseFrom(""));
    }

    @Test
    void testStringInvalidJson() {
        assertThrows(ParseException.class, () -> parser.parseFrom("foobar"));
    }

    @Test
    void testStringJsonArray() {
        assertThrows(ParseException.class, () -> parser.parseFrom("[]"));
    }

    @Test
    void testEmptyObject() throws ParseException {
        parser.parseFrom("{}");
    }

    @Test
    void testStringWrongPropertyType() {
        assertThrows(ParseException.class, () -> parser.parseFrom("{\"threadId\":\"foobar\"}"));
    }

    @Test
    void testStringIgnoreInvalidProperty() throws ParseException {
        parser.parseFrom("{\"foo\":\"bar\"}");
    }

    @Test
    void testByteArray() throws ParseException {
        final LogEvent logEvent = parser.parseFrom(JSON.getBytes(StandardCharsets.UTF_8));
        assertLogEvent(logEvent);
    }

    @Test
    void testByteArrayOffsetLength() throws ParseException {
        final byte[] bytes = ("abc" + JSON + "def").getBytes(StandardCharsets.UTF_8);
        final LogEvent logEvent = parser.parseFrom(bytes, 3, bytes.length - 6);
        assertLogEvent(logEvent);
    }
}
