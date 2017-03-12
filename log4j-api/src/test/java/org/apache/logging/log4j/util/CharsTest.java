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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class CharsTest {
    @Test
    public void invalidDigitReturnsNullCharacter() throws Exception {
        assertEquals('\0', Chars.getUpperCaseHex(-1));
        assertEquals('\0', Chars.getUpperCaseHex(16));
        assertEquals('\0', Chars.getUpperCaseHex(400));
        assertEquals('\0', Chars.getLowerCaseHex(-1));
        assertEquals('\0', Chars.getLowerCaseHex(16));
        assertEquals('\0', Chars.getLowerCaseHex(400));
    }

    @Test
    public void validDigitReturnsProperCharacter() throws Exception {
        final char[] expectedLower = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        final char[] expectedUpper = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        for (int i = 0; i < 16; i++) {
            assertEquals(String.format("Expected %x", i), expectedLower[i], Chars.getLowerCaseHex(i));
            assertEquals(String.format("Expected %X", i), expectedUpper[i], Chars.getUpperCaseHex(i));
        }
    }
}