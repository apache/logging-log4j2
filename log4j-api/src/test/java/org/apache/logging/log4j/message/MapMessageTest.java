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
package org.apache.logging.log4j.message;

import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class MapMessageTest {

    @Test
    public void testMap() {
        final String testMsg = "Test message {}";
        final StringMapMessage msg = new StringMapMessage();
        msg.put("message", testMsg);
        msg.put("project", "Log4j");
        final String result = msg.getFormattedMessage();
        final String expected = "message=\"Test message {}\" project=\"Log4j\"";
        assertEquals(expected, result);
    }

    @Test
    public void testBuilder() {
        final String testMsg = "Test message {}";
        final StringMapMessage msg = new StringMapMessage()
                .with("message", testMsg)
                .with("project", "Log4j");
        final String result = msg.getFormattedMessage();
        final String expected = "message=\"Test message {}\" project=\"Log4j\"";
        assertEquals(expected, result);
    }

    @Test
    public void testXML() {
        final String testMsg = "Test message {}";
        final StringMapMessage msg = new StringMapMessage();
        msg.put("message", testMsg);
        msg.put("project", "Log4j");
        final String result = msg.getFormattedMessage(new String[]{"XML"});
        final String expected = "<Map>\n  <Entry key=\"message\">Test message {}</Entry>\n" +
            "  <Entry key=\"project\">Log4j</Entry>\n" +
            "</Map>";
        assertEquals(expected, result);
    }

    @Test
    public void testXMLEscape() {
        final String testMsg = "Test message <foo>";
        final StringMapMessage msg = new StringMapMessage();
        msg.put("message", testMsg);
        final String result = msg.getFormattedMessage(new String[]{"XML"});
        final String expected = "<Map>\n  <Entry key=\"message\">Test message &lt;foo&gt;</Entry>\n" +
                "</Map>";
        assertEquals(expected, result);
    }

    @Test
    public void testJSON() {
        final String testMsg = "Test message {}";
        final StringMapMessage msg = new StringMapMessage();
        msg.put("message", testMsg);
        msg.put("project", "Log4j");
        final String result = msg.getFormattedMessage(new String[]{"JSON"});
        final String expected = "{\"message\":\"Test message {}\", \"project\":\"Log4j\"}";
        assertEquals(expected, result);
    }

    @Test
    public void testJSONEscape() {
        final String testMsg = "Test message \"Hello, World!\"";
        final StringMapMessage msg = new StringMapMessage();
        msg.put("message", testMsg);
        final String result = msg.getFormattedMessage(new String[]{"JSON"});
        final String expected = "{\"message\":\"Test message \\\"Hello, World!\\\"\"}";
        assertEquals(expected, result);
    }

    @Test
    public void testJSONEscapeNewlineAndOtherControlCharacters() {
        final String testMsg = "hello\tworld\r\nhh\bere is it\f";
        final StringMapMessage msg = new StringMapMessage();
        msg.put("one\ntwo", testMsg);
        final String result = msg.getFormattedMessage(new String[]{"JSON"});
        final String expected =
                "{\"one\\ntwo\":\"hello\\tworld\\r\\nhh\\bere is it\\f\"}";
        assertEquals(expected, result);
    }

    @Test
    public void testJava() {
        final String testMsg = "Test message {}";
        final StringMapMessage msg = new StringMapMessage();
        msg.put("message", testMsg);
        msg.put("project", "Log4j");
        final String result = msg.getFormattedMessage(new String[]{"Java"});
        final String expected = "{message=\"Test message {}\", project=\"Log4j\"}";
        assertEquals(expected, result);
    }

    @Test
    public void testMutableByDesign() { // LOG4J2-763
        final StringMapMessage msg = new StringMapMessage();

        // modify parameter before calling msg.getFormattedMessage
        msg.put("key1", "value1");
        msg.put("key2", "value2");
        final String result = msg.getFormattedMessage(new String[]{"Java"});
        final String expected = "{key1=\"value1\", key2=\"value2\"}";
        assertEquals(expected, result);

        // modify parameter after calling msg.getFormattedMessage
        msg.put("key3", "value3");
        final String result2 = msg.getFormattedMessage(new String[]{"Java"});
        final String expected2 = "{key1=\"value1\", key2=\"value2\", key3=\"value3\"}";
        assertEquals(expected2, result2);
    }

    @Test
    public void testGetNonStringValue() {
        final String key = "Key";
        final ObjectMapMessage msg = new ObjectMapMessage()
                .with(key, 1L);
        assertEquals("1", msg.get(key));
    }

    @Test
    public void testRemoveNonStringValue() {
        final String key = "Key";
        final ObjectMapMessage msg = new ObjectMapMessage()
                .with(key, 1L);
        assertEquals("1", msg.remove(key));
    }

    @Test
    public void testJSONFormatNonStringValue() {
        final ObjectMapMessage msg = new ObjectMapMessage()
                .with("key", 1L);
        final String result = msg.getFormattedMessage(new String[]{"JSON"});
        final String expected = "{\"key\":\"1\"}";
        assertEquals(expected, result);
    }

    @Test
    public void testXMLFormatNonStringValue() {
        final ObjectMapMessage msg = new ObjectMapMessage()
                .with("key", 1L);
        final String result = msg.getFormattedMessage(new String[]{"XML"});
        final String expected = "<Map>\n  <Entry key=\"key\">1</Entry>\n</Map>";
        assertEquals(expected, result);
    }

    @Test
    public void testFormatToUsedInOutputXml() {
        final ObjectMapMessage msg = new ObjectMapMessage()
                .with("key", new FormattableTestType());
        final String result = msg.getFormattedMessage(new String[]{"XML"});
        final String expected = "<Map>\n  <Entry key=\"key\">formatTo</Entry>\n</Map>";
        assertEquals(expected, result);
    }

    @Test
    public void testFormatToUsedInOutputJson() {
        final ObjectMapMessage msg = new ObjectMapMessage()
                .with("key", new FormattableTestType());
        final String result = msg.getFormattedMessage(new String[]{"JSON"});
        final String expected = "{\"key\":\"formatTo\"}";
        assertEquals(expected, result);
    }

    @Test
    public void testFormatToUsedInOutputJava() {
        final ObjectMapMessage msg = new ObjectMapMessage()
                .with("key", new FormattableTestType());
        final String result = msg.getFormattedMessage(new String[]{"JAVA"});
        final String expected = "{key=\"formatTo\"}";
        assertEquals(expected, result);
    }

    @Test
    public void testFormatToUsedInOutputDefault() {
        final ObjectMapMessage msg = new ObjectMapMessage()
                .with("key", new FormattableTestType());
        final String result = msg.getFormattedMessage(null);
        final String expected = "key=\"formatTo\"";
        assertEquals(expected, result);
    }

    @Test
    public void testGetUsesDeepToString() {
        final String key = "key";
        final ObjectMapMessage msg = new ObjectMapMessage()
                .with(key, new FormattableTestType());
        final String result = msg.get(key);
        final String expected = "formatTo";
        assertEquals(expected, result);
    }

    @Test
    public void testRemoveUsesDeepToString() {
        final String key = "key";
        final ObjectMapMessage msg = new ObjectMapMessage()
                .with(key, new FormattableTestType());
        final String result = msg.remove(key);
        final String expected = "formatTo";
        assertEquals(expected, result);
    }

    private static final class FormattableTestType implements StringBuilderFormattable {

        @Override
        public String toString() {
            return "toString";
        }

        @Override
        public void formatTo(final StringBuilder buffer) {
            buffer.append("formatTo");
        }
    }
}
