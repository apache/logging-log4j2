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
package org.apache.logging.log4j.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CharsTest {
    @ParameterizedTest
    @ValueSource(ints = {-1, 16, 400, -1, 16, 400})
    void invalidDigitReturnsNullCharacter(final int invalidDigit) {
        assertAll(
                () -> assertEquals('\0', Chars.getUpperCaseHex(invalidDigit)),
                () -> assertEquals('\0', Chars.getLowerCaseHex(invalidDigit)));
    }

    @Test
    void validDigitReturnsProperCharacter() {
        final char[] expectedLower = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        final char[] expectedUpper = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        assertAll(IntStream.range(0, 16)
                .mapToObj(i -> () -> assertAll(
                        () -> assertEquals(expectedLower[i], Chars.getLowerCaseHex(i), String.format("Expected %x", i)),
                        () -> assertEquals(
                                expectedUpper[i], Chars.getUpperCaseHex(i), String.format("Expected %X", i)))));
    }
}
