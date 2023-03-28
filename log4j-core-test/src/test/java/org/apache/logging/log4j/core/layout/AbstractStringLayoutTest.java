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
package org.apache.logging.log4j.core.layout;

import java.nio.charset.Charset;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests AbstractStringLayout.
 */
public class AbstractStringLayoutTest {
    static class ConcreteStringLayout extends AbstractStringLayout {
        public static int DEFAULT_STRING_BUILDER_SIZE = AbstractStringLayout.DEFAULT_STRING_BUILDER_SIZE;
        public static int MAX_STRING_BUILDER_SIZE = AbstractStringLayout.MAX_STRING_BUILDER_SIZE;

        public ConcreteStringLayout() {
            super(new DefaultConfiguration(), Charset.defaultCharset());
        }

        @Override
        public String toSerializable(final LogEvent event) {
            return null;
        }
    }

    @Test
    public void testGetStringBuilderCapacityRestrictedToMax() {

        final ConcreteStringLayout layout = new ConcreteStringLayout();
        final StringBuilder sb = layout.stringBuilderRecycler.acquire();
        final int initialCapacity = sb.capacity();
        try {
            assertEquals(ConcreteStringLayout.DEFAULT_STRING_BUILDER_SIZE, sb.capacity(), "initial capacity");

            final int SMALL = 100;
            final String smallMessage = new String(new char[SMALL]);
            sb.append(smallMessage);
            assertEquals(initialCapacity, sb.capacity(), "capacity not grown");
            assertEquals(SMALL, sb.length(), "length=msg length");
        } finally {
            layout.stringBuilderRecycler.release(sb);
        }

        final StringBuilder sb2 = layout.stringBuilderRecycler.acquire();
        try {
            assertEquals(sb2.capacity(), initialCapacity, "capacity unchanged");
            assertEquals(0, sb2.length(), "empty, ready for use");

            final int LARGE = ConcreteStringLayout.MAX_STRING_BUILDER_SIZE * 2;
            final String largeMessage = new String(new char[LARGE]);
            sb2.append(largeMessage);
            assertTrue(sb2.capacity() >= LARGE, "capacity grown to fit msg length");
            assertTrue(sb2.capacity() >= ConcreteStringLayout.MAX_STRING_BUILDER_SIZE,
                    "capacity is now greater than max length");
            assertEquals(LARGE, sb2.length(), "length=msg length");
            sb2.setLength(0); // set 0 before next getStringBuilder() call
            assertEquals(0, sb2.length(), "empty, cleared");
            assertTrue(sb2.capacity() >= ConcreteStringLayout.MAX_STRING_BUILDER_SIZE, "capacity remains very large");
        } finally {
            layout.stringBuilderRecycler.release(sb2);
        }

        final StringBuilder sb3 = layout.stringBuilderRecycler.acquire();
        try {
            assertEquals(ConcreteStringLayout.MAX_STRING_BUILDER_SIZE, sb3.capacity(),
                    "capacity, trimmed to MAX_STRING_BUILDER_SIZE");
            assertEquals(0, sb3.length(), "empty, ready for use");
        } finally {
            layout.stringBuilderRecycler.release(sb3);
        }

    }

}
