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

package org.apache.logging.log4j.core.appender.rolling.action;

import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.apache.logging.log4j.core.appender.rolling.action.IfFileName;
import org.junit.Test;

import static org.junit.Assert.*;

public class IfFileNameTest {

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNameFilterFailsIfBothRegexAndPathAreNull() {
        IfFileName.createNameFilter(null, null);
    }

    @Test()
    public void testCreateNameFilterAcceptsIfEitherRegexOrPathOrBothAreNonNull() {
        IfFileName.createNameFilter("bar", null);
        IfFileName.createNameFilter(null, "foo");
        IfFileName.createNameFilter("bar", "foo");
    }

    @Test
    public void testGetRegexReturnsConstructorValue() {
        assertEquals("bar", IfFileName.createNameFilter(null, "bar").getRegex().pattern());
        assertEquals(null, IfFileName.createNameFilter("path", null).getRegex());
    }

    @Test
    public void testGetPathReturnsConstructorValue() {
        assertEquals("path", IfFileName.createNameFilter("path", null).getPathPattern());
        assertEquals(null, IfFileName.createNameFilter(null, "bar").getPathPattern());
    }

    @Test
    public void testAcceptUsesPathPatternIfExists() {
        final IfFileName filter = IfFileName.createNameFilter("path", "regex");
        final Path relativePath = FileSystems.getDefault().getPath("path");
        assertTrue(filter.accept(null, relativePath, null));
        
        final Path pathMatchingRegex = FileSystems.getDefault().getPath("regex");
        assertFalse(filter.accept(null, pathMatchingRegex, null));
    }

    @Test
    public void testAcceptUsesRegexIfNoPathPatternExists() {
        final IfFileName regexFilter = IfFileName.createNameFilter(null, "regex");
        final Path pathMatchingRegex = FileSystems.getDefault().getPath("regex");
        assertTrue(regexFilter.accept(null, pathMatchingRegex, null));
        
        final Path noMatch = FileSystems.getDefault().getPath("nomatch");
        assertFalse(regexFilter.accept(null, noMatch, null));
    }

    @Test
    public void testAcceptIgnoresBasePathAndAttributes() {
        final IfFileName pathFilter = IfFileName.createNameFilter("path", null);
        final Path relativePath = FileSystems.getDefault().getPath("path");
        assertTrue(pathFilter.accept(null, relativePath, null));
        
        final IfFileName regexFilter = IfFileName.createNameFilter(null, "regex");
        final Path pathMatchingRegex = FileSystems.getDefault().getPath("regex");
        assertTrue(regexFilter.accept(null, pathMatchingRegex, null));
    }

    @Test
    public void testIsMatch() {
        assertTrue(IfFileName.isMatch("abc", "???"));
        assertTrue(IfFileName.isMatch("abc", "a??"));
        assertTrue(IfFileName.isMatch("abc", "?b?"));
        assertTrue(IfFileName.isMatch("abc", "??c"));
        assertTrue(IfFileName.isMatch("abc", "ab?"));
        assertTrue(IfFileName.isMatch("abc", "?bc"));
        assertTrue(IfFileName.isMatch("abc", "*"));
        assertTrue(IfFileName.isMatch("abc", "*bc"));
        assertTrue(IfFileName.isMatch("abc", "*c"));
        assertTrue(IfFileName.isMatch("abc", "*c*"));
        assertTrue(IfFileName.isMatch("abc", "a*"));
        assertTrue(IfFileName.isMatch("abc", "*a*"));
        assertTrue(IfFileName.isMatch("abc", "*b*"));

        assertFalse(IfFileName.isMatch("abc", "????"));
        assertFalse(IfFileName.isMatch("abc", "b*"));
        assertFalse(IfFileName.isMatch("abc", "*b"));
    }

}
