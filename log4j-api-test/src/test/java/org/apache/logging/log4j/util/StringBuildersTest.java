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
package org.apache.logging.log4j.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the StringBuilders class.
 */
public class StringBuildersTest {
    @Test
    public void trimToMaxSize() {
        final StringBuilder sb = new StringBuilder();
        final char[] value = new char[4 * 1024];
        sb.append(value);

        assertTrue(sb.length() > Constants.MAX_REUSABLE_MESSAGE_SIZE, "needs trimming");
        StringBuilders.trimToMaxSize(sb, Constants.MAX_REUSABLE_MESSAGE_SIZE);
        assertTrue(sb.length() <= Constants.MAX_REUSABLE_MESSAGE_SIZE, "trimmed OK");
    }

    @Test
    public void trimToMaxSizeWithLargeCapacity() {
        final StringBuilder sb = new StringBuilder();
        final char[] value = new char[4 * 1024];
        sb.append(value);
        sb.setLength(0);

        assertTrue(sb.capacity() > Constants.MAX_REUSABLE_MESSAGE_SIZE, "needs trimming");
        StringBuilders.trimToMaxSize(sb, Constants.MAX_REUSABLE_MESSAGE_SIZE);
        assertTrue(sb.capacity() <= Constants.MAX_REUSABLE_MESSAGE_SIZE, "trimmed OK");
    }

    @Test
    public void escapeJsonCharactersCorrectly() {
        String jsonValueNotEscaped = "{\"field\n1\":\"value_1\"}";
        String jsonValueEscaped = "{\\\"field\\n1\\\":\\\"value_1\\\"}";

        StringBuilder sb = new StringBuilder();
        sb.append(jsonValueNotEscaped);
        assertEquals(jsonValueNotEscaped, sb.toString());
        StringBuilders.escapeJson(sb, 0);
        assertEquals(jsonValueEscaped, sb.toString());

        sb = new StringBuilder();
        String jsonValuePartiallyEscaped = "{\"field\n1\":\\\"value_1\\\"}";
        sb.append(jsonValueNotEscaped);
        assertEquals(jsonValueNotEscaped, sb.toString());
        StringBuilders.escapeJson(sb, 10);
        assertEquals(jsonValuePartiallyEscaped, sb.toString());
    }

    @Test
    public void escapeJsonCharactersISOControl() {
        String jsonValueNotEscaped = "{\"field\n1\":\"value" + (char) 0x8F + "_1\"}";
        String jsonValueEscaped = "{\\\"field\\n1\\\":\\\"value\\u008F_1\\\"}";

        StringBuilder sb = new StringBuilder();
        sb.append(jsonValueNotEscaped);
        assertEquals(jsonValueNotEscaped, sb.toString());
        StringBuilders.escapeJson(sb, 0);
        assertEquals(jsonValueEscaped, sb.toString());
    }

    @Test
    public void escapeXMLCharactersCorrectly() {
        String xmlValueNotEscaped = "<\"Salt&Peppa'\">";
        String xmlValueEscaped = "&lt;&quot;Salt&amp;Peppa&apos;&quot;&gt;";

        StringBuilder sb = new StringBuilder();
        sb.append(xmlValueNotEscaped);
        assertEquals(xmlValueNotEscaped, sb.toString());
        StringBuilders.escapeXml(sb, 0);
        assertEquals(xmlValueEscaped, sb.toString());
    }
}
