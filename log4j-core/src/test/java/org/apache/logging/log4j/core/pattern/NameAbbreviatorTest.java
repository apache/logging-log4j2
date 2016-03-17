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

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

/**
 *
 */
@RunWith(Parameterized.class)
public class NameAbbreviatorTest {

    private final String pattern;
    private final String expected;

    public NameAbbreviatorTest(final String pattern, final String expected) {
        this.pattern = pattern;
        this.expected = expected;
    }

    @Parameterized.Parameters(name = "pattern=\"{0}\", expected={1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
            new Object[][]{
                // { pattern, expected }
                { "0", "NameAbbreviatorTest" },
                { "1", "NameAbbreviatorTest" },
                { "2", "pattern.NameAbbreviatorTest" },
                { "3", "core.pattern.NameAbbreviatorTest" },
                { "1.", "o.a.l.l.c.p.NameAbbreviatorTest" },
                { "1.1.~", "o.a.~.~.~.~.NameAbbreviatorTest" },
                { ".", "......NameAbbreviatorTest" }
            }
        );
    }

    @Test
    public void testAbbreviatorPatterns() throws Exception {
        final NameAbbreviator abbreviator = NameAbbreviator.getAbbreviator(this.pattern);
        final StringBuilder destination = new StringBuilder();
        abbreviator.abbreviate(this.getClass().getName(), destination);
        final String actual = destination.toString();
        assertEquals(expected, actual);
    }

    @Test
    public void testAbbreviatorPatternsAppend() throws Exception {
        final NameAbbreviator abbreviator = NameAbbreviator.getAbbreviator(this.pattern);
        final String PREFIX = "some random text";
        final StringBuilder destination = new StringBuilder(PREFIX);
        abbreviator.abbreviate(this.getClass().getName(), destination);
        final String actual = destination.toString();
        assertEquals(PREFIX + expected, actual);
    }

}
