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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Regression tests for ANSI SGR attribute codes (GitHub issue #4105).
 * <p>
 * Expected values are hard-coded SGR sequences so a wrong mapping cannot
 * compare equal to itself.
 * </p>
 */
class AnsiEscapeTest {

    @ParameterizedTest
    @CsvSource({
        // Log4j style names
        "normal, 0",
        "bold, 1",
        "dim, 2",
        "italic, 3",
        "underline, 4",
        "blink, 5",
        "reverse, 7",
        "hidden, 8",
        // Jansi AnsiRenderer.Code names / aliases (post-#3070 parity)
        "reset, 0",
        "intensity_bold, 1",
        "faint, 2",
        "intensity_faint, 2",
        "blink_slow, 5",
        "blink_fast, 6",
        "blink_off, 25",
        "negative_on, 7",
        "negative_off, 27",
        "conceal_on, 8",
        "conceal_off, 28",
        "underline_double, 21",
        "underline_off, 24",
        "bg_default, 49",
    })
    void styleMapsToSgr(final String name, final String sgrCode) {
        assertEquals("\u001B[" + sgrCode + "m", AnsiEscape.createSequence(name));
    }
}
