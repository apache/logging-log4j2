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

import com.google.common.base.Strings;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
        final String expected = "{'message':'Test message {}','project':'Log4j'}".replace('\'', '"');
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
    public void testJsonFormatterNestedObjectSupport() {
        final String actualJson = new ObjectMapMessage()
                .with("key1", "val1")
                .with("key2", Collections.singletonMap("key2.1", "val2.1"))
                .with("key3", Arrays.asList(
                        3,
                        (byte) 127,
                        4.5D,
                        4.6F,
                        Arrays.asList(true, false),
                        new BigDecimal(30),
                        Collections.singletonMap("key3.3", "val3.3")))
                .with("key4", new LinkedHashMap<String, Object>() {{
                    put("chars", new char[]{'a', 'b', 'c'});
                    put("booleans", new boolean[]{true, false});
                    put("bytes", new byte[]{1, 2});
                    put("shorts", new short[]{3, 4});
                    put("ints", new int[]{256, 257});
                    put("longs", new long[]{2147483648L, 2147483649L});
                    put("floats", new float[]{1.0F, 1.1F});
                    put("doubles", new double[]{2.0D, 2.1D});
                    put("objects", new Object[]{"foo", "bar"});
                }})
                .getFormattedMessage(new String[]{"JSON"});
        final String expectedJson = ("{" +
                "'key1':'val1'," +
                "'key2':{'key2.1':'val2.1'}," +
                "'key3':[3,127,4.5,4.6,[true,false],30,{'key3.3':'val3.3'}]," +
                "'key4':{" +
                "'chars':['a','b','c']," +
                "'booleans':[true,false]," +
                "'bytes':[1,2]," +
                "'shorts':[3,4]," +
                "'ints':[256,257]," +
                "'longs':[2147483648,2147483649]," +
                "'floats':[1.0,1.1]," +
                "'doubles':[2.0,2.1]," +
                "'objects':['foo','bar']" +
                "}}").replace('\'', '"');
        assertEquals(expectedJson, actualJson);
    }

    @Test
    public void testJsonFormatterInfiniteRecursionPrevention() {
        final List<Object> recursiveValue = Arrays.asList(1, null);
        // noinspection CollectionAddedToSelf
        recursiveValue.set(1, recursiveValue);
        assertThrows(IllegalArgumentException.class, () -> new ObjectMapMessage()
                .with("key", recursiveValue)
                .getFormattedMessage(new String[]{"JSON"}));
    }

    @Test
    public void testJsonFormatterMaxDepthViolation() {
        assertThrows(IllegalArgumentException.class, () -> testJsonFormatterMaxDepth(MapMessageJsonFormatter.MAX_DEPTH - 1));
    }

    @Test
    public void testJsonFormatterMaxDepthConformance() {
        int depth = MapMessageJsonFormatter.MAX_DEPTH - 2;
        String expectedJson = String
                .format("{'key':%s1%s}",
                        Strings.repeat("[", depth),
                        Strings.repeat("]", depth))
                .replace('\'', '"');
        String actualJson = testJsonFormatterMaxDepth(depth);
        assertEquals(expectedJson, actualJson);
    }

    public static String testJsonFormatterMaxDepth(int depth) {
        List<Object> list = new LinkedList<>();
        list.add(1);
        while (--depth > 0) {
            list = new LinkedList<>(Collections.singletonList(list));
        }
        return new ObjectMapMessage()
                .with("key", list)
                .getFormattedMessage(new String[]{"JSON"});
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
        final ObjectMapMessage msg = new ObjectMapMessage().with("key", 1L);
        final String result = msg.getFormattedMessage(new String[]{"JSON"});
        final String expected = "{'key':1}".replace('\'', '"');
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
