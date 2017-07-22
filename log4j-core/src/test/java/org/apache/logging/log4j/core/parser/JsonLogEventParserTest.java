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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JsonLogEventParserTest {

    private JsonLogEventParser parser;

    private static final String JSON = "{\n" +
            "  \"timeMillis\" : 1493121664118,\n" +
            "  \"thread\" : \"main\",\n" +
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
            "  \"endOfBatch\" : false,\n" +
            "  \"loggerFqcn\" : \"org.apache.logging.log4j.spi.AbstractLogger\",\n" +
            "  \"contextMap\" : {\n" +
            "    \"bar\" : \"BAR\",\n" +
            "    \"foo\" : \"FOO\"\n" +
            "  },\n" +
            "  \"threadId\" : 1,\n" +
            "  \"threadPriority\" : 5,\n" +
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
        LogEvent logEvent = parser.parseFrom(JSON);
        assertNotNull(logEvent);
        assertEquals("HelloWorld", logEvent.getLoggerName());
        // TODO assert more here
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
    public void testStringInvalidProperty() throws ParseException {
        parser.parseFrom("{\"foo\":\"bar\"}");
    }

}
