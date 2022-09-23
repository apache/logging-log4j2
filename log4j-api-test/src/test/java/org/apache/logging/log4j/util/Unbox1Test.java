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

package org.apache.logging.log4j.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the Unbox class.
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
public class Unbox1Test {
    @BeforeAll
    public static void beforeClass() {
        System.clearProperty("log4j.unbox.ringbuffer.size");
    }

    @Test
    public void testBoxClaimsItHas32Slots() {
        assertEquals(32, Unbox.getRingbufferSize());
    }

    @Test
    public void testBoxHas32Slots() {
        final int MAX = 32;
        final StringBuilder[] probe = new StringBuilder[MAX * 3];
        for (int i = 0; i <= probe.length - 8; ) {
            probe[i++] = Unbox.box(true);
            probe[i++] = Unbox.box('c');
            probe[i++] = Unbox.box(Byte.MAX_VALUE);
            probe[i++] = Unbox.box(Double.MAX_VALUE);
            probe[i++] = Unbox.box(Float.MAX_VALUE);
            probe[i++] = Unbox.box(Integer.MAX_VALUE);
            probe[i++] = Unbox.box(Long.MAX_VALUE);
            probe[i++] = Unbox.box(Short.MAX_VALUE);
        }
        for (int i = 0; i < probe.length - MAX; i++) {
            assertSame(probe[i], probe[i + MAX], "probe[" + i +"], probe[" + (i + MAX) +"]");
            for (int j = 1; j < MAX - 1; j++) {
                assertNotSame(probe[i], probe[i + j], "probe[" + i +"], probe[" + (i + j) +"]");
            }
        }
    }

    @Test
    public void testBoxBoolean() {
        assertEquals("true", Unbox.box(true).toString());
        assertEquals("false", Unbox.box(false).toString());
    }

    @Test
    public void testBoxByte() {
        assertEquals("0", Unbox.box((byte) 0).toString());
        assertEquals("1", Unbox.box((byte) 1).toString());
        assertEquals("127", Unbox.box((byte) 127).toString());
        assertEquals("-1", Unbox.box((byte) -1).toString());
        assertEquals("-128", Unbox.box((byte) -128).toString());
    }

    @Test
    public void testBoxChar() {
        assertEquals("a", Unbox.box('a').toString());
        assertEquals("b", Unbox.box('b').toString());
        assertEquals("字", Unbox.box('字').toString());
    }

    @Test
    public void testBoxDouble() {
        assertEquals("3.14", Unbox.box(3.14).toString());
        assertEquals(Double.toString(Double.MAX_VALUE), Unbox.box(Double.MAX_VALUE).toString());
        assertEquals(Double.toString(Double.MIN_VALUE), Unbox.box(Double.MIN_VALUE).toString());
    }

    @Test
    public void testBoxFloat() {
        assertEquals("3.14", Unbox.box(3.14F).toString());
        assertEquals(Float.toString(Float.MAX_VALUE), Unbox.box(Float.MAX_VALUE).toString());
        assertEquals(Float.toString(Float.MIN_VALUE), Unbox.box(Float.MIN_VALUE).toString());
    }

    @Test
    public void testBoxInt() {
        assertEquals("0", Unbox.box(0).toString());
        assertEquals("1", Unbox.box(1).toString());
        assertEquals("127", Unbox.box(127).toString());
        assertEquals("-1", Unbox.box(-1).toString());
        assertEquals("-128", Unbox.box(-128).toString());
        assertEquals(Integer.toString(Integer.MAX_VALUE), Unbox.box(Integer.MAX_VALUE).toString());
        assertEquals(Integer.toString(Integer.MIN_VALUE), Unbox.box(Integer.MIN_VALUE).toString());
    }

    @Test
    public void testBoxLong() {
        assertEquals("0", Unbox.box(0L).toString());
        assertEquals("1", Unbox.box(1L).toString());
        assertEquals("127", Unbox.box(127L).toString());
        assertEquals("-1", Unbox.box(-1L).toString());
        assertEquals("-128", Unbox.box(-128L).toString());
        assertEquals(Long.toString(Long.MAX_VALUE), Unbox.box(Long.MAX_VALUE).toString());
        assertEquals(Long.toString(Long.MIN_VALUE), Unbox.box(Long.MIN_VALUE).toString());
    }

    @Test
    public void testBoxShort() {
        assertEquals("0", Unbox.box((short) 0).toString());
        assertEquals("1", Unbox.box((short) 1).toString());
        assertEquals("127", Unbox.box((short) 127).toString());
        assertEquals("-1", Unbox.box((short) -1).toString());
        assertEquals("-128", Unbox.box((short) -128).toString());
        assertEquals(Short.toString(Short.MAX_VALUE), Unbox.box(Short.MAX_VALUE).toString());
        assertEquals(Short.toString(Short.MIN_VALUE), Unbox.box(Short.MIN_VALUE).toString());
    }

    @Test
    public void testBoxIsThreadLocal() throws Exception {
        final StringBuilder[] probe = new StringBuilder[16 * 3];
        populate(0, probe);
        final Thread t1 = new Thread(() -> populate(16, probe));
        t1.start();
        t1.join();
        final Thread t2 = new Thread(() -> populate(16, probe));
        t2.start();
        t2.join();
        for (int i = 0; i < probe.length - 16; i++) {
            for (int j = 1; j < 16; j++) {
                assertNotSame(
                        probe[i], probe[i + j], "probe[" + i +"]=" + probe[i] + ", probe[" + (i + j) +"]=" + probe[i + j]);
            }
        }
    }

    private void populate(final int start, final StringBuilder[] probe) {
        for (int i = start; i <= start + 8; ) {
            probe[i++] = Unbox.box(true);
            probe[i++] = Unbox.box('c');
            probe[i++] = Unbox.box(Byte.MAX_VALUE);
            probe[i++] = Unbox.box(Double.MAX_VALUE);
            probe[i++] = Unbox.box(Float.MAX_VALUE);
            probe[i++] = Unbox.box(Integer.MAX_VALUE);
            probe[i++] = Unbox.box(Long.MAX_VALUE);
            probe[i++] = Unbox.box(Short.MAX_VALUE);
        }
    }
}