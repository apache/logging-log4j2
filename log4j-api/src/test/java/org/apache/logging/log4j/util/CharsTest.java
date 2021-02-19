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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class CharsTest {
    @ParameterizedTest
    @ValueSource(ints = {-1, 16, 400, -1, 16, 400})
    public void invalidDigitReturnsNullCharacter(int invalidDigit) {
        assertAll(
                () -> assertThat(Chars.getUpperCaseHex(invalidDigit)).isEqualTo('\0'),
                () -> assertThat(Chars.getLowerCaseHex(invalidDigit)).isEqualTo('\0')
        );
    }

    @Test
    public void validDigitReturnsProperCharacter() {
        final char[] expectedLower = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        final char[] expectedUpper = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        assertAll(IntStream.range(0, 16).mapToObj(i -> () -> assertAll(
                () -> assertThat(Chars.getLowerCaseHex(i)).describedAs(String.format("Expected %x", i)).isEqualTo(expectedLower[i]),
                () -> assertThat(Chars.getUpperCaseHex(i)).describedAs(String.format("Expected %X", i)).isEqualTo(expectedUpper[i])
        )));
    }
}
