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
package org.apache.logging.log4j.core.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.apache.logging.log4j.core.test.junit.Tags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(Tags.ASYNC_LOGGERS)
class AsyncLoggerThreadNameStrategyTest {
    @AfterEach
    void after() {
        System.clearProperty("AsyncLogger.ThreadNameStrategy");
    }

    @BeforeEach
    void before() {
        System.clearProperty("AsyncLogger.ThreadNameStrategy");
    }

    @Test
    void testDefaultIfNotConfigured() {
        final ThreadNameCachingStrategy tns = ThreadNameCachingStrategy.create();
        assertSame(ThreadNameCachingStrategy.DEFAULT_STRATEGY, tns);
    }

    @Test
    void testDefaultIfInvalidConfig() {
        System.setProperty("AsyncLogger.ThreadNameStrategy", "\\%%InValid ");
        final ThreadNameCachingStrategy tns = ThreadNameCachingStrategy.create();
        assertSame(ThreadNameCachingStrategy.DEFAULT_STRATEGY, tns);
    }

    @Test
    void testUseCachedThreadNameIfConfigured() {
        System.setProperty("AsyncLogger.ThreadNameStrategy", "CACHED");
        final ThreadNameCachingStrategy tns = ThreadNameCachingStrategy.create();
        assertSame(ThreadNameCachingStrategy.CACHED, tns);
    }

    @Test
    void testUseUncachedThreadNameIfConfigured() {
        System.setProperty("AsyncLogger.ThreadNameStrategy", "UNCACHED");
        final ThreadNameCachingStrategy tns = ThreadNameCachingStrategy.create();
        assertSame(ThreadNameCachingStrategy.UNCACHED, tns);
    }

    @Test
    void testUncachedThreadNameStrategyReturnsCurrentThreadName() {
        final String name1 = "MODIFIED-THREADNAME1";
        Thread.currentThread().setName(name1);
        assertEquals(name1, ThreadNameCachingStrategy.UNCACHED.getThreadName());

        final String name2 = "OTHER-THREADNAME2";
        Thread.currentThread().setName(name2);
        assertEquals(name2, ThreadNameCachingStrategy.UNCACHED.getThreadName());
    }

    @Test
    void testCachedThreadNameStrategyReturnsCachedThreadName() {
        final String original = "Original-ThreadName";
        Thread.currentThread().setName(original);
        assertEquals(original, ThreadNameCachingStrategy.CACHED.getThreadName());

        final String name2 = "OTHER-THREADNAME2";
        Thread.currentThread().setName(name2);
        assertEquals(original, ThreadNameCachingStrategy.CACHED.getThreadName());
    }
}
