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

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.parallel.Isolated;
import org.junitpioneer.jupiter.SetSystemProperty;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the Unbox ring buffer size is configurable.
 * Must be run in a separate process as the other UnboxTest or the last-run test will fail.
 * Run this test on its own via {@code mvn --projects log4j-api test -Dtest=Unbox2ConfigurableTest} which will automatically
 * enable the test, too.
 */
@EnabledIfSystemProperty(named = "test", matches = ".*Unbox2ConfigurableTest.*")
@Isolated
@SetSystemProperty(key = "log4j.unbox.ringbuffer.size", value = "65")
public class Unbox2ConfigurableTest {
    @AfterAll
    public static void afterClass() throws Exception {
        // ensure subsequent tests (which assume 32 slots) pass
        // make non-private
        final Field field = FieldUtils.getDeclaredField(Unbox.class, "RINGBUFFER_SIZE", true);
        // make non-final
        FieldUtils.writeDeclaredField(field, "modifiers", field.getModifiers() &~ Modifier.FINAL);

        field.set(null, 32); // reset to default

        final Field threadLocalField = FieldUtils.getDeclaredField(Unbox.class, "threadLocalState", true);
        final ThreadLocal<?> threadLocal = (ThreadLocal<?>) threadLocalField.get(null);
        threadLocal.remove();
        threadLocalField.set(null, new ThreadLocal<>());
    }

    @Test
    public void testBoxConfiguredTo128Slots() {
        // next power of 2 that is 65 or more
        assertEquals(128, Unbox.getRingbufferSize());
    }

    @Test
    public void testBoxSuccessfullyConfiguredTo128Slots() {
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
            assertSame(probe[i], probe[i + MAX], "probe[" + i +"], probe[" + (i + MAX) +"]");
            for (int j = 1; j < MAX - 1; j++) {
                assertNotSame(probe[i], probe[i + j], "probe[" + i +"], probe[" + (i + j) +"]");
            }
        }
    }
}
