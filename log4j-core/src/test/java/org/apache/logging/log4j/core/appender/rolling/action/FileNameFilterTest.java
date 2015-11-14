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

import org.apache.logging.log4j.core.appender.rolling.action.FileNameFilter;
import org.junit.Test;

import static org.junit.Assert.*;

public class FileNameFilterTest {

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNameFilterFailsIfBothRegexAndPathAreNull() {
        FileNameFilter.createNameFilter(null, null);
    }

    @Test()
    public void testCreateNameFilterAcceptsIfEitherRegexOrPathOrBothAreNonNull() {
        FileNameFilter.createNameFilter("bar", null);
        FileNameFilter.createNameFilter(null, "foo");
        FileNameFilter.createNameFilter("bar", "foo");
    }

    @Test
    public void testGetRegexReturnsConstructorValue() {
        assertEquals("bar", FileNameFilter.createNameFilter(null, "bar").getRegex().pattern());
        assertEquals(null, FileNameFilter.createNameFilter("path", null).getRegex());
    }

    @Test
    public void testGetPathReturnsConstructorValue() {
        assertEquals("path", FileNameFilter.createNameFilter("path", null).getPathPattern());
        assertEquals(null, FileNameFilter.createNameFilter(null, "bar").getPathPattern());
    }

    @Test
    public void testAcceptUsesPathPatternIfExists() {
        final FileNameFilter filter = FileNameFilter.createNameFilter("path", "regex");
        final Path relativePath = FileSystems.getDefault().getPath("path");
        assertTrue(filter.accept(null, relativePath, null));
        
        final Path pathMatchingRegex = FileSystems.getDefault().getPath("regex");
        assertFalse(filter.accept(null, pathMatchingRegex, null));
    }

    @Test
    public void testAcceptUsesRegexIfNoPathPatternExists() {
        final FileNameFilter regexFilter = FileNameFilter.createNameFilter(null, "regex");
        final Path pathMatchingRegex = FileSystems.getDefault().getPath("regex");
        assertTrue(regexFilter.accept(null, pathMatchingRegex, null));
        
        final Path noMatch = FileSystems.getDefault().getPath("nomatch");
        assertFalse(regexFilter.accept(null, noMatch, null));
    }

    @Test
    public void testAcceptIgnoresBasePathAndAttributes() {
        final FileNameFilter pathFilter = FileNameFilter.createNameFilter("path", null);
        final Path relativePath = FileSystems.getDefault().getPath("path");
        assertTrue(pathFilter.accept(null, relativePath, null));
        
        final FileNameFilter regexFilter = FileNameFilter.createNameFilter(null, "regex");
        final Path pathMatchingRegex = FileSystems.getDefault().getPath("regex");
        assertTrue(regexFilter.accept(null, pathMatchingRegex, null));
    }

    @Test
    public void testIsMatch() {
        assertTrue(FileNameFilter.isMatch("abc", "???"));
        assertTrue(FileNameFilter.isMatch("abc", "a??"));
        assertTrue(FileNameFilter.isMatch("abc", "?b?"));
        assertTrue(FileNameFilter.isMatch("abc", "??c"));
        assertTrue(FileNameFilter.isMatch("abc", "ab?"));
        assertTrue(FileNameFilter.isMatch("abc", "?bc"));
        assertTrue(FileNameFilter.isMatch("abc", "*"));
        assertTrue(FileNameFilter.isMatch("abc", "*bc"));
        assertTrue(FileNameFilter.isMatch("abc", "*c"));
        assertTrue(FileNameFilter.isMatch("abc", "*c*"));
        assertTrue(FileNameFilter.isMatch("abc", "a*"));
        assertTrue(FileNameFilter.isMatch("abc", "*a*"));
        assertTrue(FileNameFilter.isMatch("abc", "*b*"));

        assertFalse(FileNameFilter.isMatch("abc", "????"));
        assertFalse(FileNameFilter.isMatch("abc", "b*"));
        assertFalse(FileNameFilter.isMatch("abc", "*b"));
    }

}
