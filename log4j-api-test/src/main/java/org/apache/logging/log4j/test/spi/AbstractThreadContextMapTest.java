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
package org.apache.logging.log4j.test.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.spi.ThreadContextMap;

/**
 * Provides a set of utility methods to test implementations of {@link ThreadContextMap}.
 */
public abstract class AbstractThreadContextMapTest {

    private static final String KEY = "key";

    /**
     * Implementations SHOULD not propagate the context to newly created threads by default.
     *
     * @param contextMap A {@link ThreadContextMap implementation}.
     */
    protected static void assertThreadLocalNotInheritable(final ThreadContextMap contextMap) {
        contextMap.put(KEY, "threadLocalNotInheritableByDefault");
        verifyThreadContextValueFromANewThread(contextMap, null);
    }

    /**
     * Implementations MAY offer a configuration that propagates the context to newly created threads.
     *
     * @param contextMap A {@link ThreadContextMap implementation}.
     */
    protected static void assertThreadLocalInheritable(final ThreadContextMap contextMap) {
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
}
