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
package org.apache.logging.log4j.core.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CyclicBufferTest {

    @Test
    public void testSize0() {
        final CyclicBuffer<Integer> buffer = new CyclicBuffer<>(Integer.class, 0);

        assertTrue(buffer.isEmpty());
        buffer.add(1);
        assertTrue(buffer.isEmpty());
        Integer[] items = buffer.removeAll();
        assertTrue("Incorrect number of items", items.length == 0);

        assertTrue(buffer.isEmpty());
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        buffer.add(4);
        items = buffer.removeAll();
        assertTrue("Incorrect number of items", items.length == 0);
        assertTrue(buffer.isEmpty());
    }

    @Test
    public void testSize1() {
        final CyclicBuffer<Integer> buffer = new CyclicBuffer<>(Integer.class, 1);

        assertTrue(buffer.isEmpty());
        buffer.add(1);
        assertFalse(buffer.isEmpty());
        Integer[] items = buffer.removeAll();
        assertTrue("Incorrect number of items", items.length == 1);

        assertTrue(buffer.isEmpty());
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        buffer.add(4);
        items = buffer.removeAll();
        assertTrue("Incorrect number of items", items.length == 1);
        assertArrayEquals(new Integer[] { 4 }, items);
        assertTrue(buffer.isEmpty());
    }

    @Test
    public void testSize3() {
        final CyclicBuffer<Integer> buffer = new CyclicBuffer<>(Integer.class, 3);

        assertTrue(buffer.isEmpty());
        buffer.add(1);
        assertFalse(buffer.isEmpty());
        Integer[] items = buffer.removeAll();
        assertTrue("Incorrect number of items", items.length == 1);

        assertTrue(buffer.isEmpty());
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        buffer.add(4);
        items = buffer.removeAll();
        assertTrue("Incorrect number of items", items.length == 3);
        assertArrayEquals(new Integer[] { 2, 3, 4 }, items);
        assertTrue(buffer.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSizeNegative() {
        final CyclicBuffer<Integer> buffer = new CyclicBuffer<>(Integer.class, -1);
    }

}
