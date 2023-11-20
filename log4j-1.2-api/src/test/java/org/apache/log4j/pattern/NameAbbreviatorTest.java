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
package org.apache.log4j.pattern;

import junit.framework.TestCase;

/**
 * Tests for NameAbbrevator.
 *
 */
public class NameAbbreviatorTest extends TestCase {
    /**
     * Create a new instance.
     *
     * @param name test name
     */
    public NameAbbreviatorTest(final String name) {
        super(name);
    }

    /**
     * Check that getAbbreviator(" ") returns default abbreviator.
     *
     */
    public void testBlank() {
        final NameAbbreviator abbrev = NameAbbreviator.getAbbreviator("   ");
        final NameAbbreviator defaultAbbrev = NameAbbreviator.getDefaultAbbreviator();
        assertTrue(abbrev == defaultAbbrev);
    }

    /**
     * Check that blanks are trimmed in evaluating abbreviation pattern.
     */
    public void testBlankOne() {
        final NameAbbreviator abbrev = NameAbbreviator.getAbbreviator(" 1 ");
        final StringBuffer buf = new StringBuffer("DEBUG - ");
        int fieldStart = buf.length();
        buf.append("org.example.foo.bar");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - bar", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        buf.append("bar");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - bar", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - ", buf.toString());
    }

    /**
     * Check that getDefaultAbbreviator does not return null.
     *
     */
    public void testGetDefault() {
        final NameAbbreviator abbrev = NameAbbreviator.getDefaultAbbreviator();
        assertNotNull(abbrev);
    }

    /**
     * Check that getAbbreviator("-1").abbreviate() drops first name element.
     *
     */
    public void testMinusOne() {
        final NameAbbreviator abbrev = NameAbbreviator.getAbbreviator("-1");
        final StringBuffer buf = new StringBuffer("DEBUG - ");
        int fieldStart = buf.length();
        buf.append("org.example.foo.bar");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - example.foo.bar", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        buf.append("bar");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - bar", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - ", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        buf.append(".");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - ", buf.toString());
    }

    /**
     * Check that getAbbreviator("1.*.2").abbreviate drops all but the first character from the first element, uses all of
     * the second element and drops all but the first two characters of the rest of the non-final elements.
     *
     */
    public void testMulti() {
        final NameAbbreviator abbrev = NameAbbreviator.getAbbreviator("1.*.2");
        final StringBuffer buf = new StringBuffer("DEBUG - ");
        int fieldStart = buf.length();
        buf.append("org.example.foo.bar");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - o.example.fo.bar", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        buf.append("org.example.foo.");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - o.example.fo.", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        buf.append("foo.bar");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - f.bar", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        buf.append("bar");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - bar", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - ", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        buf.append(".");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - .", buf.toString());
    }

    /**
     * Check that getAbbreviator("1").abbreviate() drops all but the final name element.
     *
     */
    public void testOne() {
        final NameAbbreviator abbrev = NameAbbreviator.getAbbreviator("1");
        final StringBuffer buf = new StringBuffer("DEBUG - ");
        int fieldStart = buf.length();
        buf.append("org.example.foo.bar");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - bar", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        buf.append("bar");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - bar", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - ", buf.toString());
    }

    /**
     * Check that getAbbreviator("1.").abbreviate abbreviates non-final elements to one character.
     *
     */
    public void testOneDot() {
        final NameAbbreviator abbrev = NameAbbreviator.getAbbreviator("1.");
        final StringBuffer buf = new StringBuffer("DEBUG - ");
        int fieldStart = buf.length();
        buf.append("org.example.foo.bar");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - o.e.f.bar", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        buf.append("org.example.foo.");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - o.e.f.", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        buf.append("foo.bar");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - f.bar", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        buf.append("bar");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - bar", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - ", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        buf.append(".");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - .", buf.toString());
    }

    /**
     * Check that getAbbreviator("1~.").abbreviate abbreviates non-final elements to one character and a tilde.
     *
     */
    public void testOneTildeDot() {
        final NameAbbreviator abbrev = NameAbbreviator.getAbbreviator("1~.");
        final StringBuffer buf = new StringBuffer("DEBUG - ");
        int fieldStart = buf.length();
        buf.append("org.example.foo.bar");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - o~.e~.f~.bar", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        buf.append("org.example.foo.");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - o~.e~.f~.", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        buf.append("foo.bar");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - f~.bar", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        buf.append("bar");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - bar", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - ", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        buf.append(".");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - .", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        buf.append("o.e.f.bar");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - o.e.f.bar", buf.toString());
    }

    /**
     * Check that getAbbreviator("2").abbreviate drops all but the last two elements.
     *
     */
    public void testTwo() {
        final NameAbbreviator abbrev = NameAbbreviator.getAbbreviator("2");
        final StringBuffer buf = new StringBuffer("DEBUG - ");
        int fieldStart = buf.length();
        buf.append("org.example.foo.bar");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - foo.bar", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        buf.append("foo.bar");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - foo.bar", buf.toString());

        buf.setLength(0);
        buf.append("DEBUG - ");
        fieldStart = buf.length();
        buf.append("bar");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - bar", buf.toString());
    }

    /**
     * Check that "0" drops all name content.
     *
     */
    public void testZero() {
        final NameAbbreviator abbrev = NameAbbreviator.getAbbreviator("0");
        final StringBuffer buf = new StringBuffer("DEBUG - ");
        final int fieldStart = buf.length();
        buf.append("org.example.foo.bar");
        abbrev.abbreviate(fieldStart, buf);
        assertEquals("DEBUG - ", buf.toString());
    }
}
