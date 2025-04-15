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
package org.apache.logging.log4j.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * This class is borrowed from <a href="https://github.com/FasterXML/jackson-core">Jackson</a>.
 */
class JsonUtilsTest {

    @Test
    void testQuoteCharSequenceAsString() {
        final StringBuilder output = new StringBuilder();
        final StringBuilder builder = new StringBuilder();
        builder.append("foobar");
        JsonUtils.quoteAsString(builder, output);
        assertEquals("foobar", output.toString());
        builder.setLength(0);
        output.setLength(0);
        builder.append("\"x\"");
        JsonUtils.quoteAsString(builder, output);
        assertEquals("\\\"x\\\"", output.toString());
    }

    // For [JACKSON-853]
    @Test
    void testQuoteLongCharSequenceAsString() {
        final StringBuilder output = new StringBuilder();
        final StringBuilder input = new StringBuilder();
        final StringBuilder sb2 = new StringBuilder();
        for (int i = 0; i < 1111; ++i) {
            input.append('"');
            sb2.append("\\\"");
        }
        final String exp = sb2.toString();
        JsonUtils.quoteAsString(input, output);
        assertEquals(2 * input.length(), output.length());
        assertEquals(exp, output.toString());
    }

    // [JACKSON-884]
    @Test
    void testCharSequenceWithCtrlChars() {
        final char[] input = new char[] {0, 1, 2, 3, 4};
        final StringBuilder builder = new StringBuilder();
        builder.append(input);
        final StringBuilder output = new StringBuilder();
        JsonUtils.quoteAsString(builder, output);
        assertEquals("\\u0000\\u0001\\u0002\\u0003\\u0004", output.toString());
    }
}
