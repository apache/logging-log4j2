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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class NameAbbreviatorTest {

    @Test
    public void testZero() {
        final String str = this.getClass().getName();
        final NameAbbreviator a = NameAbbreviator.getAbbreviator("0");
        final String result = a.abbreviate(str);
        assertEquals("NameAbbreviatorTest", result);
    }

    @Test
    public void testNameAbbreviation() {
        final String str = this.getClass().getName();
        final NameAbbreviator a = NameAbbreviator.getAbbreviator("1");
        final String result = a.abbreviate(str);
        assertEquals("NameAbbreviatorTest", result);
    }

     @Test
    public void testTwo() {
        final String str = this.getClass().getName();
        final NameAbbreviator a = NameAbbreviator.getAbbreviator("2");
        final String result = a.abbreviate(str);
        assertEquals("pattern.NameAbbreviatorTest", result);

    }

    @Test
    public void testShortName() {
        final String str = this.getClass().getName();
        final NameAbbreviator a = NameAbbreviator.getAbbreviator("1.");
        final String result = a.abbreviate(str);
        assertEquals("o.a.l.l.c.p.NameAbbreviatorTest", result);

    }

    @Test
    public void testSkipNames() {
        final String str = this.getClass().getName();
        final NameAbbreviator a = NameAbbreviator.getAbbreviator("1.1.~");
        final String result = a.abbreviate(str);
        assertEquals("o.a.~.~.~.~.NameAbbreviatorTest", result);

    }

     @Test
    public void testZeroDot() {
        final String str = this.getClass().getName();
        final NameAbbreviator a = NameAbbreviator.getAbbreviator(".");
        final String result = a.abbreviate(str);
        assertEquals("......NameAbbreviatorTest", result);

    }
}
