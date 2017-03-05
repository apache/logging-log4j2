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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests that the Unbox ring buffer size is configurable.
 * Must be run in a separate process as the other UnboxTest or the last-run test will fail.
 */
public class Unbox2ConfigurableTest {
    @Ignore
    @BeforeClass
    public static void beforeClass() {
        System.setProperty("log4j.unbox.ringbuffer.size", "65");
    }

    @Ignore
    @AfterClass
    public static void afterClass() throws Exception {
        System.clearProperty("log4j.unbox.ringbuffer.size");

        // ensure subsequent tests (which assume 32 slots) pass
        final Field field = Unbox.class.getDeclaredField("RINGBUFFER_SIZE");
        field.setAccessible(true); // make non-private

        final Field modifierField = Field.class.getDeclaredField("modifiers");
        modifierField.setAccessible(true);
        modifierField.setInt(field, field.getModifiers() &~ Modifier.FINAL); // make non-final

        field.set(null, 32); // reset to default

        final Field threadLocalField = Unbox.class.getDeclaredField("threadLocalState");
        threadLocalField.setAccessible(true);
        final ThreadLocal<?> threadLocal = (ThreadLocal<?>) threadLocalField.get(null);
        threadLocal.remove();
        threadLocalField.set(null, new ThreadLocal<>());
    }

    @Ignore
    @Test
    public void testBoxConfiguredTo128Slots() throws Exception {
        // next power of 2 that is 65 or more
        assertEquals(128, Unbox.getRingbufferSize());
    }

    @Ignore
    @Test
    public void testBoxSuccessfullyConfiguredTo128Slots() throws Exception {
        final int MAX = 128;
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
            assertSame("probe[" + i +"], probe[" + (i + MAX) +"]", probe[i], probe[i + MAX]);
            for (int j = 1; j < MAX - 1; j++) {
                assertNotSame("probe[" + i +"], probe[" + (i + j) +"]", probe[i], probe[i + j]);
            }
        }
    }
}