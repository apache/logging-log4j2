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
package org.apache.logging.log4j.layout.template.json.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CharSequencePointerTest {

    private final CharSequencePointer pointer = new CharSequencePointer();

    @Test
    void length_should_fail_without_reset() {
        // noinspection ResultOfMethodCallIgnored
        assertMissingReset(pointer::length);
    }

    @Test
    void charAt_should_fail_without_reset() {
        assertMissingReset(() -> pointer.charAt(0));
    }

    @Test
    @SuppressWarnings("ReturnValueIgnored")
    void toString_should_fail_without_reset() {
        // noinspection ResultOfMethodCallIgnored
        assertMissingReset(pointer::toString);
    }

    private static void assertMissingReset(final Runnable runnable) {
        Assertions.assertThatThrownBy(runnable::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("pointer must be reset first");
    }

    @ParameterizedTest
    @CsvSource({"'',0,0,''", "foo,0,1,f", "foo,1,1,''", "foo,1,2,o", "foo,3,3,''"})
    void toString_should_subSequence(
            final CharSequence delegate, final int startIndex, final int endIndex, final String expectedOutput) {
        pointer.reset(delegate, startIndex, endIndex);
        Assertions.assertThat(pointer).hasToString(expectedOutput);
    }

    @Test
    void subSequence_should_not_be_supported() {
        pointer.reset("", 0, 0);
        assertUnsupportedOperation(() -> pointer.subSequence(0, 0));
    }

    @Test
    void chars_should_not_be_supported() {
        pointer.reset("", 0, 0);
        assertUnsupportedOperation(() -> pointer.subSequence(0, 0));
    }

    @Test
    void codePoints_should_not_be_supported() {
        pointer.reset("", 0, 0);
        assertUnsupportedOperation(() -> pointer.subSequence(0, 0));
    }

    private static void assertUnsupportedOperation(final Runnable runnable) {
        Assertions.assertThatThrownBy(runnable::run)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("operation requires allocation, contradicting with the purpose of the class");
    }

    @Test
    void reset_should_fail_on_null_delegate() {
        Assertions.assertThatThrownBy(() -> pointer.reset(null, 0, 0))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("delegate");
    }

    @ParameterizedTest
    @CsvSource({
        "foo,-1,3,invalid start: -1",
        "foo,4,3,invalid length: -1",
        "foo,0,-1,invalid length: -1",
        "foo,1,0,invalid length: -1",
        "foo,0,4,invalid end: 4"
    })
    void reset_should_fail_on_invalid_indices(
            final CharSequence delegate, final int startIndex, final int endIndex, final String expectedErrorMessage) {
        Assertions.assertThatThrownBy(() -> pointer.reset(delegate, startIndex, endIndex))
                .isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessage(expectedErrorMessage);
    }
}
