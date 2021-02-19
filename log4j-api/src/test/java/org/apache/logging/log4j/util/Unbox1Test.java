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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

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
        assertThat(Unbox.getRingbufferSize()).isEqualTo(32);
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
            assertThat(probe[i + MAX]).describedAs("probe[" + i +"], probe[" + (i + MAX) +"]").isSameAs(probe[i]);
            for (int j = 1; j < MAX - 1; j++) {
                assertThat(probe[i + j]).describedAs("probe[" + i +"], probe[" + (i + j) +"]").isNotSameAs(probe[i]);
            }
        }
    }

    @Test
    public void testBoxBoolean() {
        assertThat(Unbox.box(true).toString()).isEqualTo("true");
        assertThat(Unbox.box(false).toString()).isEqualTo("false");
    }

    @Test
    public void testBoxByte() {
        assertThat(Unbox.box((byte) 0).toString()).isEqualTo("0");
        assertThat(Unbox.box((byte) 1).toString()).isEqualTo("1");
        assertThat(Unbox.box((byte) 127).toString()).isEqualTo("127");
        assertThat(Unbox.box((byte) -1).toString()).isEqualTo("-1");
        assertThat(Unbox.box((byte) -128).toString()).isEqualTo("-128");
    }

    @Test
    public void testBoxChar() {
        assertThat(Unbox.box('a').toString()).isEqualTo("a");
        assertThat(Unbox.box('b').toString()).isEqualTo("b");
        assertThat(Unbox.box('字').toString()).isEqualTo("字");
    }

    @Test
    public void testBoxDouble() {
        assertThat(Unbox.box(3.14).toString()).isEqualTo("3.14");
        assertThat(Unbox.box(Double.MAX_VALUE).toString()).isEqualTo(new Double(Double.MAX_VALUE).toString());
        assertThat(Unbox.box(Double.MIN_VALUE).toString()).isEqualTo(new Double(Double.MIN_VALUE).toString());
    }

    @Test
    public void testBoxFloat() {
        assertThat(Unbox.box(3.14F).toString()).isEqualTo("3.14");
        assertThat(Unbox.box(Float.MAX_VALUE).toString()).isEqualTo(new Float(Float.MAX_VALUE).toString());
        assertThat(Unbox.box(Float.MIN_VALUE).toString()).isEqualTo(new Float(Float.MIN_VALUE).toString());
    }

    @Test
    public void testBoxInt() {
        assertThat(Unbox.box(0).toString()).isEqualTo("0");
        assertThat(Unbox.box(1).toString()).isEqualTo("1");
        assertThat(Unbox.box(127).toString()).isEqualTo("127");
        assertThat(Unbox.box(-1).toString()).isEqualTo("-1");
        assertThat(Unbox.box(-128).toString()).isEqualTo("-128");
        assertThat(Unbox.box(Integer.MAX_VALUE).toString()).isEqualTo(new Integer(Integer.MAX_VALUE).toString());
        assertThat(Unbox.box(Integer.MIN_VALUE).toString()).isEqualTo(new Integer(Integer.MIN_VALUE).toString());
    }

    @Test
    public void testBoxLong() {
        assertThat(Unbox.box(0L).toString()).isEqualTo("0");
        assertThat(Unbox.box(1L).toString()).isEqualTo("1");
        assertThat(Unbox.box(127L).toString()).isEqualTo("127");
        assertThat(Unbox.box(-1L).toString()).isEqualTo("-1");
        assertThat(Unbox.box(-128L).toString()).isEqualTo("-128");
        assertThat(Unbox.box(Long.MAX_VALUE).toString()).isEqualTo(new Long(Long.MAX_VALUE).toString());
        assertThat(Unbox.box(Long.MIN_VALUE).toString()).isEqualTo(new Long(Long.MIN_VALUE).toString());
    }

    @Test
    public void testBoxShort() {
        assertThat(Unbox.box((short) 0).toString()).isEqualTo("0");
        assertThat(Unbox.box((short) 1).toString()).isEqualTo("1");
        assertThat(Unbox.box((short) 127).toString()).isEqualTo("127");
        assertThat(Unbox.box((short) -1).toString()).isEqualTo("-1");
        assertThat(Unbox.box((short) -128).toString()).isEqualTo("-128");
        assertThat(Unbox.box(Short.MAX_VALUE).toString()).isEqualTo(new Short(Short.MAX_VALUE).toString());
        assertThat(Unbox.box(Short.MIN_VALUE).toString()).isEqualTo(new Short(Short.MIN_VALUE).toString());
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
                assertThat(probe[i + j]).describedAs("probe[" + i +"]=" + probe[i] + ", probe[" + (i + j) +"]=" + probe[i + j]).isNotSameAs(probe[i]);
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
