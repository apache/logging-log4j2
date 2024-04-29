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
package org.apache.logging.log4j.spi;

import static org.apache.logging.log4j.test.ThreadLocalUtil.assertThreadLocalCount;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.apache.logging.log4j.test.ThreadLocalUtil;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ThreadContextMapTest {

    private static final String KEY = "key";

    static Stream<ThreadContextMap> defaultMaps() {
        return Stream.of(
                new DefaultThreadContextMap(),
                new CopyOnWriteSortedArrayThreadContextMap(),
                new GarbageFreeSortedArrayThreadContextMap());
    }

    static Stream<ThreadContextMap> inheritableMaps() {
        final Properties props = new Properties();
        props.setProperty("log4j2.isThreadContextMapInheritable", "true");
        final PropertiesUtil util = new PropertiesUtil(props);
        return Stream.of(
                new DefaultThreadContextMap(true, util),
                new CopyOnWriteSortedArrayThreadContextMap(util),
                new GarbageFreeSortedArrayThreadContextMap(util));
    }

    @ParameterizedTest
    @MethodSource("defaultMaps")
    void threadLocalNotInheritableByDefault(final ThreadContextMap contextMap) {
        contextMap.put(KEY, "threadLocalNotInheritableByDefault");
        verifyThreadContextValueFromANewThread(contextMap, null);
    }

    @ParameterizedTest
    @MethodSource("inheritableMaps")
    void threadLocalInheritableIfConfigured(final ThreadContextMap contextMap) {
        contextMap.put(KEY, "threadLocalInheritableIfConfigured");
        verifyThreadContextValueFromANewThread(contextMap, "threadLocalInheritableIfConfigured");
    }

    private static void verifyThreadContextValueFromANewThread(
            final ThreadContextMap contextMap, final String expected) {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            assertThat(executorService.submit(() -> contextMap.get(KEY)))
                    .succeedsWithin(Duration.ofSeconds(1))
                    .isEqualTo(expected);
        } finally {
            executorService.shutdown();
        }
    }

    static Stream<ThreadContextMap> threadLocalsRemovedWhenMapEmpty() {
        return Stream.of(new DefaultThreadContextMap(), new CopyOnWriteSortedArrayThreadContextMap());
    }

    @ParameterizedTest
    @MethodSource
    void threadLocalsRemovedWhenMapEmpty(final ThreadContextMap contextMap) {
        // JUnit calls contextMap#toString() and sets the `ThreadLocal`.
        contextMap.clear();
        final int threadLocalCount = ThreadLocalUtil.getThreadLocalCount();

        contextMap.put(KEY, "threadLocalsRemovedWhenMapEmpty");
        assertThreadLocalCount(threadLocalCount + 1);
        contextMap.remove(KEY);
        assertThreadLocalCount(threadLocalCount);

        contextMap.put("key1", "value1");
        contextMap.put("key2", "value2");
        assertThreadLocalCount(threadLocalCount + 1);
        if (contextMap instanceof DefaultThreadContextMap) {
            ((DefaultThreadContextMap) contextMap).removeAll(Arrays.asList("key1", "key2"));
        }
        if (contextMap instanceof CleanableThreadContextMap) {
            ((CleanableThreadContextMap) contextMap).removeAll(Arrays.asList("key1", "key2"));
        }
        assertThreadLocalCount(threadLocalCount);

        contextMap.put(KEY, "threadLocalsRemovedWhenMapEmpty");
        assertThreadLocalCount(threadLocalCount + 1);
        contextMap.clear();
        assertThreadLocalCount(threadLocalCount);
    }
}
