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

import org.junit.jupiter.api.Test;

/**
 * Testing FormattingInfo.
 */
class FormattingInfoTest {

    @Test
    void testFormatTruncateFromBeginning() {
        final StringBuilder message = new StringBuilder("Hello, world");

        final FormattingInfo formattingInfo = new FormattingInfo(false, 0, 5, true);
        formattingInfo.format(0, message);

        assertEquals("world", message.toString());
    }

    @Test
    void testFormatTruncateFromEnd() {
        final StringBuilder message = new StringBuilder("Hello, world");

        final FormattingInfo formattingInfo = new FormattingInfo(false, 0, 5, false);
        formattingInfo.format(0, message);

        assertEquals("Hello", message.toString());
    }

    @Test
    void testFormatTruncateFromEndGivenFieldStart() {
        final StringBuilder message =
                new StringBuilder("2015-03-09 11:49:28,295; INFO  org.apache.logging.log4j.PatternParserTest");

        final FormattingInfo formattingInfo = new FormattingInfo(false, 0, 5, false);
        formattingInfo.format(31, message);

        assertEquals("2015-03-09 11:49:28,295; INFO  org.a", message.toString());
    }
}
