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
package org.apache.logging.log4j.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import org.junit.jupiter.api.Test;

/**
 * Tests {@linkStrings}.
 */
public class StringsTest {

    @Test
    public void testConcat() {
        assertEquals("ab", Strings.concat("a", "b"));
        assertEquals("a", Strings.concat("a", ""));
        assertEquals("a", Strings.concat("a", null));
        assertEquals("b", Strings.concat("", "b"));
        assertEquals("b", Strings.concat(null, "b"));
    }

    /**
     * A sanity test to make sure a typo does not mess up {@link Strings#EMPTY}.
     */
    @Test
    public void testEMPTY() {
        assertEquals("", Strings.EMPTY);
        assertEquals(0, Strings.EMPTY.length());
    }

    @Test
    public void testIsBlank() {
        assertTrue(Strings.isBlank(null));
        assertTrue(Strings.isBlank(""));
        assertTrue(Strings.isBlank(" "));
        assertTrue(Strings.isBlank("\n"));
        assertTrue(Strings.isBlank("\r"));
        assertTrue(Strings.isBlank("\t"));
        assertFalse(Strings.isEmpty("a"));
    }

    @Test
    public void testIsEmpty() {
        assertTrue(Strings.isEmpty(null));
        assertTrue(Strings.isEmpty(""));
        assertFalse(Strings.isEmpty(" "));
        assertFalse(Strings.isEmpty("a"));
    }

    @Test
    public void testJoin() {
        assertNull(Strings.join((Iterable<?>) null, '.'));
        assertNull(Strings.join((Iterator<?>) null, '.'));
        assertEquals("", Strings.join((Collections.emptyList()), '.'));

        assertEquals("a", Strings.join(Collections.singletonList("a"), '.'));
        assertEquals("a.b", Strings.join(Arrays.asList("a", "b"), '.'));
        assertEquals("a.b.c", Strings.join(Arrays.asList("a", "b", "c"), '.'));

        assertEquals("", Strings.join(Collections.singletonList((String) null), ':'));
        assertEquals(":", Strings.join(Arrays.asList(null, null), ':'));
        assertEquals("a:", Strings.join(Arrays.asList("a", null), ':'));
        assertEquals(":b", Strings.join(Arrays.asList(null, "b"), ':'));
    }

    @Test
    public void splitList() {
        String[] list = Strings.splitList("1, 2, 3");
        assertEquals(3, list.length);
        list = Strings.splitList("");
        assertEquals(1, list.length);
        list = Strings.splitList(null);
        assertEquals(0, list.length);
    }

    @Test
    public void testQuote() {
        assertEquals("'Q'", Strings.quote("Q"));
    }

    @Test
    public void testToLowerCase() {
        assertEquals("a", Strings.toRootLowerCase("A"));
        assertEquals("a", Strings.toRootLowerCase("a"));
    }
}
