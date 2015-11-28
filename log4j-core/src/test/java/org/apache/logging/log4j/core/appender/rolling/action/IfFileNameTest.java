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

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import static org.junit.Assert.*;

public class IfFileNameTest {

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNameConditionFailsIfBothRegexAndPathAreNull() {
        IfFileName.createNameCondition(null, null);
    }

    @Test()
    public void testCreateNameConditionAcceptsIfEitherRegexOrPathOrBothAreNonNull() {
        IfFileName.createNameCondition("bar", null);
        IfFileName.createNameCondition(null, "foo");
        IfFileName.createNameCondition("bar", "foo");
    }

    @Test
    public void testGetSyntaxAndPattern() {
        assertEquals("glob:path", IfFileName.createNameCondition("path", null).getSyntaxAndPattern());
        assertEquals("glob:path", IfFileName.createNameCondition("glob:path", null).getSyntaxAndPattern());
        assertEquals("regex:bar", IfFileName.createNameCondition(null, "bar").getSyntaxAndPattern());
        assertEquals("regex:bar", IfFileName.createNameCondition(null, "regex:bar").getSyntaxAndPattern());
    }

    @Test
    public void testAcceptUsesPathPatternIfExists() {
        final IfFileName filter = IfFileName.createNameCondition("path", "regex");
        final Path relativePath = Paths.get("path");
        assertTrue(filter.accept(null, relativePath, null));
        
        final Path pathMatchingRegex = Paths.get("regex");
        assertFalse(filter.accept(null, pathMatchingRegex, null));
    }

    @Test
    public void testAcceptUsesRegexIfNoPathPatternExists() {
        final IfFileName regexFilter = IfFileName.createNameCondition(null, "regex");
        final Path pathMatchingRegex = Paths.get("regex");
        assertTrue(regexFilter.accept(null, pathMatchingRegex, null));
        
        final Path noMatch = Paths.get("nomatch");
        assertFalse(regexFilter.accept(null, noMatch, null));
    }

    @Test
    public void testAcceptIgnoresBasePathAndAttributes() {
        final IfFileName pathFilter = IfFileName.createNameCondition("path", null);
        final Path relativePath = Paths.get("path");
        assertTrue(pathFilter.accept(null, relativePath, null));
        
        final IfFileName regexFilter = IfFileName.createNameCondition(null, "regex");
        final Path pathMatchingRegex = Paths.get("regex");
        assertTrue(regexFilter.accept(null, pathMatchingRegex, null));
    }

    @Test
    public void testAcceptCallsNestedConditionsOnlyIfPathAccepted1() {
        final CountingCondition counter = new CountingCondition(true);
        final IfFileName regexFilter = IfFileName.createNameCondition(null, "regex", counter);
        final Path pathMatchingRegex = Paths.get("regex");
        
        assertTrue(regexFilter.accept(null, pathMatchingRegex, null));
        assertEquals(1, counter.getAcceptCount());
        assertTrue(regexFilter.accept(null, pathMatchingRegex, null));
        assertEquals(2, counter.getAcceptCount());
        assertTrue(regexFilter.accept(null, pathMatchingRegex, null));
        assertEquals(3, counter.getAcceptCount());
        
        final Path noMatch = Paths.get("nomatch");
        assertFalse(regexFilter.accept(null, noMatch, null));
        assertEquals(3, counter.getAcceptCount()); // no increase
        assertFalse(regexFilter.accept(null, noMatch, null));
        assertEquals(3, counter.getAcceptCount());
        assertFalse(regexFilter.accept(null, noMatch, null));
        assertEquals(3, counter.getAcceptCount());
    }

    @Test
    public void testAcceptCallsNestedConditionsOnlyIfPathAccepted2() {
        final CountingCondition counter = new CountingCondition(true);
        final IfFileName globFilter = IfFileName.createNameCondition("glob", null, counter);
        final Path pathMatchingGlob = Paths.get("glob");
        
        assertTrue(globFilter.accept(null, pathMatchingGlob, null));
        assertEquals(1, counter.getAcceptCount());
        assertTrue(globFilter.accept(null, pathMatchingGlob, null));
        assertEquals(2, counter.getAcceptCount());
        assertTrue(globFilter.accept(null, pathMatchingGlob, null));
        assertEquals(3, counter.getAcceptCount());

        final Path noMatch = Paths.get("nomatch");
        assertFalse(globFilter.accept(null, noMatch, null));
        assertEquals(3, counter.getAcceptCount()); // no increase
        assertFalse(globFilter.accept(null, noMatch, null));
        assertEquals(3, counter.getAcceptCount());
        assertFalse(globFilter.accept(null, noMatch, null));
        assertEquals(3, counter.getAcceptCount());
    }

    @Test
    public void testBeforeTreeWalk() {
        final CountingCondition counter = new CountingCondition(true);
        final IfFileName pathFilter = IfFileName.createNameCondition("path", null, counter, counter, counter);
        pathFilter.beforeFileTreeWalk();
        assertEquals(3, counter.getBeforeFileTreeWalkCount());
    }
}
