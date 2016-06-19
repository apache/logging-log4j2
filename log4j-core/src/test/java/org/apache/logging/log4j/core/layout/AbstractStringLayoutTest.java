package org.apache.logging.log4j.core.layout;/*
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

import java.nio.charset.Charset;

import org.apache.logging.log4j.core.LogEvent;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests AbstractStringLayout.
 */
public class AbstractStringLayoutTest {
    static class ConcreteStringLayout extends AbstractStringLayout {
        public static int DEFAULT_STRING_BUILDER_SIZE = AbstractStringLayout.DEFAULT_STRING_BUILDER_SIZE;
        public static int MAX_STRING_BUILDER_SIZE = AbstractStringLayout.MAX_STRING_BUILDER_SIZE;

        public ConcreteStringLayout() {
            super(Charset.defaultCharset());
        }

        public static StringBuilder getStringBuilder() {
            return AbstractStringLayout.getStringBuilder();
        }

        @Override
        public String toSerializable(final LogEvent event) {
            return null;
        }
    }

    @Test
    public void testGetStringBuilderCapacityRestrictedToMax() throws Exception {
        final int LARGE = 4096;
        final String largeMessage = new String(new char[LARGE]);
        final StringBuilder sb = ConcreteStringLayout.getStringBuilder();
        assertEquals("initial capacity", ConcreteStringLayout.DEFAULT_STRING_BUILDER_SIZE, sb.capacity());
        sb.append(largeMessage);
        assertEquals("capacity=msg length", LARGE, sb.capacity());
        assertEquals("capacity=msg length", LARGE, sb.length());

        final StringBuilder next = ConcreteStringLayout.getStringBuilder();
        assertEquals("max capacity", ConcreteStringLayout.MAX_STRING_BUILDER_SIZE, next.capacity());
        assertEquals("empty, ready for use", 0, sb.length());
    }
}