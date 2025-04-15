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
package org.apache.logging.log4j.core.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Test;

class EqualsReplacementConverterTest {

    private static final String TEST_MESSAGE = "This is a test";

    @Test
    void testMarkerReplacement() {
        testReplacement("%marker", Strings.EMPTY);
    }

    @Test
    void testMarkerSimpleNameReplacement() {
        testReplacement("%markerSimpleName", Strings.EMPTY);
    }

    @Test
    void testLoggerNameReplacement() {
        testReplacement("%logger", "[" + EqualsReplacementConverterTest.class.getName() + "]");
    }

    @Test
    void testMarkerReplacementWithMessage() {
        testReplacement(TEST_MESSAGE, new String[] {"[%marker]", "[]", "%msg"});
    }

    private void testReplacement(final String tag, final String expectedValue) {
        final String[] options = new String[] {"[" + tag + "]", "[]", expectedValue};
        testReplacement(expectedValue, options);
    }

    private void testReplacement(final String expectedValue, final String[] options) {
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName(EqualsReplacementConverterTest.class.getName()) //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage(TEST_MESSAGE)) //
                .build();
        final StringBuilder sb = new StringBuilder();
        final LoggerContext ctx = LoggerContext.getContext();
        final EqualsReplacementConverter converter =
                EqualsReplacementConverter.newInstance(ctx.getConfiguration(), options);
        assertNotNull(converter);
        converter.format(event, sb);
        assertEquals(expectedValue, sb.toString());
    }

    @Test
    void testParseSubstitutionWithPattern() {
        testParseSubstitution("%msg", TEST_MESSAGE);
    }

    @Test
    void testParseSubstitutionWithoutPattern() {
        final String substitution = "test";
        testParseSubstitution(substitution, substitution);
    }

    @Test
    void testParseSubstitutionEmpty() {
        testParseSubstitution("", "");
    }

    @Test
    void testParseSubstitutionWithWhiteSpaces() {
        testParseSubstitution(" ", " ");
    }

    private void testParseSubstitution(final String substitution, final String expected) {
        final LogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName(EqualsReplacementConverterTest.class.getName())
                .setLevel(Level.DEBUG)
                .setMessage(new SimpleMessage(TEST_MESSAGE))
                .build();
        final LoggerContext ctx = LoggerContext.getContext();
        final EqualsReplacementConverter converter = EqualsReplacementConverter.newInstance(
                ctx.getConfiguration(), new String[] {"[%marker]", "[]", substitution});

        final StringBuilder sb = new StringBuilder();
        assertNotNull(converter);
        converter.parseSubstitution(event, sb);
        final String actual = sb.toString();
        assertEquals(expected, actual);
    }
}
