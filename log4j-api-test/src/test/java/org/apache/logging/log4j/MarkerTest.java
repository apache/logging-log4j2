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
package org.apache.logging.log4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.test.junit.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

@ResourceLock(value = Resources.MARKER_MANAGER, mode = ResourceAccessMode.READ_WRITE)
public class MarkerTest {

    @BeforeEach
    public void setUp() {
        MarkerManager.clear();
    }

    @Test
    public void testGetMarker() {
        final Marker expected = MarkerManager.getMarker("A");
        assertNull(expected.getParents());
    }

    @Test
    public void testGetMarkerWithParents() {
        final Marker expected = MarkerManager.getMarker("A");
        final Marker p1 = MarkerManager.getMarker("P1");
        p1.addParents(MarkerManager.getMarker("PP1"));
        final Marker p2 = MarkerManager.getMarker("P2");
        expected.addParents(p1);
        expected.addParents(p2);
        assertEquals(2, expected.getParents().length);
    }

    @Test
    public void testHasParents() {
        final Marker parent = MarkerManager.getMarker("PARENT");
        final Marker existing = MarkerManager.getMarker("EXISTING");
        assertFalse(existing.hasParents());
        existing.setParents(parent);
        assertTrue(existing.hasParents());
    }

    @Test
    public void testMarker() {
        // root (level 1)
        final Marker parent = MarkerManager.getMarker("PARENT");
        // level 2
        final Marker test1 = MarkerManager.getMarker("TEST1").setParents(parent);
        final Marker test2 = MarkerManager.getMarker("TEST2").addParents(parent);
        assertTrue(test1.isInstanceOf(parent), "TEST1 is not an instance of PARENT");
        assertTrue(test2.isInstanceOf(parent), "TEST2 is not an instance of PARENT");
        assertFalse(parent.isInstanceOf(test1));
        assertFalse(parent.isInstanceOf(test2));
        // level 3
        final Marker test3 = MarkerManager.getMarker("TEST3").addParents(test2);
        assertTrue(test3.isInstanceOf(test2));
        assertTrue(test3.isInstanceOf("TEST2"));
        assertTrue(test3.isInstanceOf("PARENT"));
        assertTrue(test2.isInstanceOf("PARENT"));
        assertFalse(parent.isInstanceOf(test3));
        assertFalse(parent.isInstanceOf(test3));
    }

    @Test
    public void testMarkerSharedIntermediaryMarker() {
        final Marker parent1 = MarkerManager.getMarker("PARENT1");
        final Marker parent2 = MarkerManager.getMarker("PARENT2");
        final Marker test1 = MarkerManager.getMarker("TEST1").setParents(parent1, parent2);
        assertTrue(test1.isInstanceOf(parent1));
        // Leaf
        final Marker leaf = MarkerManager.getMarker("LEAF").setParents(test1);
        assertTrue(leaf.isInstanceOf("TEST1"));
        assertTrue(leaf.isInstanceOf("PARENT1"));
        assertTrue(leaf.isInstanceOf("PARENT2"));
    }

    @Test
    public void testMultipleParents() {
        final Marker parent1 = MarkerManager.getMarker("PARENT1");
        final Marker parent2 = MarkerManager.getMarker("PARENT2");
        final Marker test1 = MarkerManager.getMarker("TEST1").setParents(parent1, parent2);
        final Marker test2 = MarkerManager.getMarker("TEST2").addParents(parent1, parent2);
        assertTrue(test1.isInstanceOf(parent1), "TEST1 is not an instance of PARENT1");
        assertTrue(test1.isInstanceOf("PARENT1"), "TEST1 is not an instance of PARENT1");
        assertTrue(test1.isInstanceOf(parent2), "TEST1 is not an instance of PARENT2");
        assertTrue(test1.isInstanceOf("PARENT2"), "TEST1 is not an instance of PARENT2");
        assertTrue(test2.isInstanceOf(parent1), "TEST2 is not an instance of PARENT1");
        assertTrue(test2.isInstanceOf("PARENT1"), "TEST2 is not an instance of PARENT1");
        assertTrue(test2.isInstanceOf(parent2), "TEST2 is not an instance of PARENT2");
        assertTrue(test2.isInstanceOf("PARENT2"), "TEST2 is not an instance of PARENT2");
    }

    @Test
    public void testAddToExistingParents() {
        final Marker parent = MarkerManager.getMarker("PARENT");
        final Marker existing = MarkerManager.getMarker("EXISTING");
        final Marker test1 = MarkerManager.getMarker("TEST1").setParents(existing);
        test1.addParents(parent);
        assertTrue(test1.isInstanceOf(parent), "TEST1 is not an instance of PARENT");
        assertTrue(test1.isInstanceOf("PARENT"), "TEST1 is not an instance of PARENT");
        assertTrue(test1.isInstanceOf(existing), "TEST1 is not an instance of EXISTING");
        assertTrue(test1.isInstanceOf("EXISTING"), "TEST1 is not an instance of EXISTING");
    }

    @Test
    public void testDuplicateParents() {
        final Marker parent = MarkerManager.getMarker("PARENT");
        final Marker existing = MarkerManager.getMarker("EXISTING");
        final Marker test1 = MarkerManager.getMarker("TEST1").setParents(existing);
        test1.addParents(parent);
        final Marker[] parents = test1.getParents();
        test1.addParents(existing);
        assertEquals(parents.length, test1.getParents().length, "duplicate add allowed");
        test1.addParents(existing, MarkerManager.getMarker("EXTRA"));
        assertEquals(parents.length + 1, test1.getParents().length, "incorrect add");
        assertTrue(test1.isInstanceOf(parent), "TEST1 is not an instance of PARENT");
        assertTrue(test1.isInstanceOf("PARENT"), "TEST1 is not an instance of PARENT");
        assertTrue(test1.isInstanceOf(existing), "TEST1 is not an instance of EXISTING");
        assertTrue(test1.isInstanceOf("EXISTING"), "TEST1 is not an instance of EXISTING");
    }
}
