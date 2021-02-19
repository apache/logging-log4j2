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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.Charset;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;

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
        final StringBuilder sb = ConcreteStringLayout.getStringBuilder();
        final int initialCapacity = sb.capacity();
        assertThat(sb.capacity()).describedAs("initial capacity").isEqualTo(ConcreteStringLayout.DEFAULT_STRING_BUILDER_SIZE);

        final int SMALL = 100;
        final String smallMessage = new String(new char[SMALL]);
        sb.append(smallMessage);
        assertThat(sb.capacity()).describedAs("capacity not grown").isEqualTo(initialCapacity);
        assertThat(sb).describedAs("length=msg length").hasSize(SMALL);

        final StringBuilder sb2 = ConcreteStringLayout.getStringBuilder();
        assertThat(initialCapacity).describedAs("capacity unchanged").isEqualTo(sb2.capacity());
        assertThat(sb2).describedAs("empty, ready for use").hasSize(0);

        final int LARGE = ConcreteStringLayout.MAX_STRING_BUILDER_SIZE * 2;
        final String largeMessage = new String(new char[LARGE]);
        sb2.append(largeMessage);
        assertTrue(sb2.capacity() >= LARGE, "capacity grown to fit msg length");
        assertTrue(sb2.capacity() >= ConcreteStringLayout.MAX_STRING_BUILDER_SIZE,
                "capacity is now greater than max length");
        assertThat(sb2).describedAs("length=msg length").hasSize(LARGE);
        sb2.setLength(0); // set 0 before next getStringBuilder() call
        assertThat(sb2).describedAs("empty, cleared").hasSize(0);
        assertTrue(sb2.capacity() >= ConcreteStringLayout.MAX_STRING_BUILDER_SIZE, "capacity remains very large");

        final StringBuilder sb3 = ConcreteStringLayout.getStringBuilder();
        assertThat(sb3.capacity()).describedAs("capacity, trimmed to MAX_STRING_BUILDER_SIZE").isEqualTo(ConcreteStringLayout.MAX_STRING_BUILDER_SIZE);
        assertThat(sb3).describedAs("empty, ready for use").hasSize(0);
    }

}
