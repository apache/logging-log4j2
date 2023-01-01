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
package org.apache.logging.log4j.core.pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for the {@link DynamicWordAbbreviator} class.
 */
class DynamicWordAbbreviatorTest extends Assertions {

    @Test
    void testNullAndEmptyInputs() {
        DynamicWordAbbreviator abbreviator = DynamicWordAbbreviator.create("1.1*");

        assertDoesNotThrow(() -> abbreviator.abbreviate("orig", null));
        assertDoesNotThrow(() -> abbreviator.abbreviate(null, new StringBuilder()));

        StringBuilder dest = new StringBuilder();
        abbreviator.abbreviate(null, dest);
        assertEquals("", dest.toString());

        abbreviator.abbreviate("", dest);
        assertEquals("", dest.toString());
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {
            "",
            " ",
            "0.0*",
            "0,0*",
            "1.2",
            "1.2**",
            "1.0*"
    })
    void testInvalidPatterns(String pattern) {
        assertNull(DynamicWordAbbreviator.create(pattern));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" \"{1}\" \"{2}\"")
    @CsvSource(delimiter = '|', value = {
            "1.1*|.|.",
            "1.1*|\\ |\\ ",
            "1.1*|org.novice.o|o.n.o",
            "1.1*|org.novice.|o.novice",
            "1.1*|org......novice|o.novice",
            "1.1*|org. . .novice|o. . .novice",
    })
    void testStrangeWords(String pattern, String input, String expected) {
        DynamicWordAbbreviator abbreviator = DynamicWordAbbreviator.create(pattern);
        StringBuilder actual = new StringBuilder();
        abbreviator.abbreviate(input, actual);
        assertEquals(expected, actual.toString());
    }

}
