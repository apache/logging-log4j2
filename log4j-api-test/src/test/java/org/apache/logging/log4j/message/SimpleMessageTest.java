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
package org.apache.logging.log4j.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import org.apache.logging.log4j.test.junit.SerialUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the SimpleMessage class.
 */
public class SimpleMessageTest {
    @Test
    public void formatTo_usesCachedMessageString() throws Exception {
        final StringBuilder charSequence = new StringBuilder("initial value");
        final SimpleMessage message = new SimpleMessage(charSequence);
        assertEquals("initial value", message.getFormattedMessage());

        charSequence.setLength(0);
        charSequence.append("different value");

        final StringBuilder result = new StringBuilder();
        message.formatTo(result);
        assertEquals("initial value", result.toString());
    }

    static Stream<CharSequence> testSerializable() {
        class NonSerializable implements CharSequence {

            private final CharSequence value;

            public NonSerializable(final CharSequence value) {
                this.value = value;
            }

            @Override
            public int length() {
                return value.length();
            }

            @Override
            public char charAt(int index) {
                return value.charAt(index);
            }

            @Override
            public String toString() {
                return value.toString();
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return value.subSequence(start, end);
            }
        }
        return Stream.of("World", new NonSerializable("World2"), null);
    }

    @ParameterizedTest
    @MethodSource
    void testSerializable(final CharSequence arg) {
        final Message expected = new SimpleMessage(arg);
        final Message actual = SerialUtil.deserialize(SerialUtil.serialize(expected));
        assertThat(actual).isInstanceOf(SimpleMessage.class);
        assertThat(actual.getFormattedMessage()).isEqualTo(expected.getFormattedMessage());
    }
}
