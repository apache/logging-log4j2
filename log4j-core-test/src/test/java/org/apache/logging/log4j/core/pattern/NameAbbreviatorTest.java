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

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 *  Unit tests for {@link NameAbbreviator} which abbreviates dot-delimited strings such as logger and class names.
 */
public class NameAbbreviatorTest {

    public static Stream<Object[]> data() {
        return Stream.of(new Object[][] {
            // { pattern, expected }
            {"0", "NameAbbreviatorTest"},
            {"1", "NameAbbreviatorTest"},
            {"2", "pattern.NameAbbreviatorTest"},
            {"3", "core.pattern.NameAbbreviatorTest"},
            {"1.", "o.a.l.l.c.p.NameAbbreviatorTest"},
            {"1.1.~", "o.a.~.~.~.~.NameAbbreviatorTest"},
            {"1.1.1.*", "o.a.l.log4j.core.pattern.NameAbbreviatorTest"},
            {".", "......NameAbbreviatorTest"},
            {"1.2*", "o.a.l.l.c.pattern.NameAbbreviatorTest"},
            {"1.3*", "o.a.l.l.core.pattern.NameAbbreviatorTest"},
            {"1.8*", "org.apache.logging.log4j.core.pattern.NameAbbreviatorTest"}
        });
    }

    @ParameterizedTest(name = "pattern=\"{0}\", expected={1}")
    @MethodSource("data")
    public void testAbbreviatorPatterns(String pattern, String expected) throws Exception {
        final NameAbbreviator abbreviator = NameAbbreviator.getAbbreviator(pattern);
        final StringBuilder destination = new StringBuilder();
        abbreviator.abbreviate(this.getClass().getName(), destination);
        final String actual = destination.toString();
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "pattern=\"{0}\", expected={1}")
    @MethodSource("data")
    public void testAbbreviatorPatternsAppendLongPrefix(String pattern, String expected) throws Exception {
        final NameAbbreviator abbreviator = NameAbbreviator.getAbbreviator(pattern);
        final String PREFIX = "some random text big enough to be larger than abbreviated string ";
        final StringBuilder destination = new StringBuilder(PREFIX);
        abbreviator.abbreviate(this.getClass().getName(), destination);
        final String actual = destination.toString();
        assertEquals(PREFIX + expected, actual);
    }

    @ParameterizedTest(name = "pattern=\"{0}\", expected={1}")
    @MethodSource("data")
    public void testAbbreviatorPatternsAppend(String pattern, String expected) throws Exception {
        final NameAbbreviator abbreviator = NameAbbreviator.getAbbreviator(pattern);
        final String PREFIX = "some random text";
        final StringBuilder destination = new StringBuilder(PREFIX);
        abbreviator.abbreviate(this.getClass().getName(), destination);
        final String actual = destination.toString();
        assertEquals(PREFIX + expected, actual);
    }
}
