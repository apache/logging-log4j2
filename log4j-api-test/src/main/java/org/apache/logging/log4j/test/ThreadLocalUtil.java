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
package org.apache.logging.log4j.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Utility class to test {@link ThreadLocal} fields.
 */
public final class ThreadLocalUtil {

    private ThreadLocalUtil() {}

    /**
     * Returns the number of {@link ThreadLocal} objects that have a value for the current thread.
     * <p>
     *     <strong>WARNING:</strong> The {@link ThreadLocal#get()} method is <strong>not</strong> side-effect free.
     *     If the thread local is not initialized, it initializes it with a value of {@code null}.
     * </p>
     * @return The number of {@code ThreadLocal} objects with a value in the current thread.
     */
    public static int getThreadLocalCount() {
        return assertDoesNotThrow(() -> {
            final Field threadLocalsField = Thread.class.getDeclaredField("threadLocals");
            threadLocalsField.setAccessible(true);
            final Object threadLocalMap = threadLocalsField.get(Thread.currentThread());
            final Method expungeStaleEntries = threadLocalMap.getClass().getDeclaredMethod("expungeStaleEntries");
            expungeStaleEntries.setAccessible(true);
            expungeStaleEntries.invoke(threadLocalMap);
            final Field sizeField = threadLocalMap.getClass().getDeclaredField("size");
            sizeField.setAccessible(true);
            return (int) sizeField.get(threadLocalMap);
        });
    }

    public static void assertThreadLocalCount(final int expexted) {
        assertThat(getThreadLocalCount()).as("Count ThreadLocals with value").isEqualTo(expexted);
    }
}
