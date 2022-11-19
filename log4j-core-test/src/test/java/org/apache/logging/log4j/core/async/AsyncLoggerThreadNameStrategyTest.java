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
package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@Tag("async")
public class AsyncLoggerThreadNameStrategyTest {

    @Test
    public void testDefaultIfNotConfigured() throws Exception {
        final ThreadNameCachingStrategy tns = ThreadNameCachingStrategy.create();
        assertSame(ThreadNameCachingStrategy.DEFAULT_STRATEGY, tns);
    }

    @Test
    @SetSystemProperty(key = Log4jProperties.ASYNC_LOGGER_THREAD_NAME_STRATEGY, value = "\\%%InValid ")
    public void testDefaultIfInvalidConfig() throws Exception {
        final ThreadNameCachingStrategy tns = ThreadNameCachingStrategy.create();
        assertSame(ThreadNameCachingStrategy.DEFAULT_STRATEGY, tns);
    }

    @Test
    @SetSystemProperty(key = Log4jProperties.ASYNC_LOGGER_THREAD_NAME_STRATEGY, value = "CACHED")
    public void testUseCachedThreadNameIfConfigured() throws Exception {
        final ThreadNameCachingStrategy tns = ThreadNameCachingStrategy.create();
        assertSame(ThreadNameCachingStrategy.CACHED, tns);
    }

    @Test
    @SetSystemProperty(key = Log4jProperties.ASYNC_LOGGER_THREAD_NAME_STRATEGY, value = "UNCACHED")
    public void testUseUncachedThreadNameIfConfigured() throws Exception {
        final ThreadNameCachingStrategy tns = ThreadNameCachingStrategy.create();
        assertSame(ThreadNameCachingStrategy.UNCACHED, tns);
    }

    @Test
    public void testUncachedThreadNameStrategyReturnsCurrentThreadName() throws Exception {
        final String name1 = "MODIFIED-THREADNAME1";
        Thread.currentThread().setName(name1);
        assertEquals(name1, ThreadNameCachingStrategy.UNCACHED.getThreadName());

        final String name2 = "OTHER-THREADNAME2";
        Thread.currentThread().setName(name2);
        assertEquals(name2, ThreadNameCachingStrategy.UNCACHED.getThreadName());
    }

    @Test
    public void testCachedThreadNameStrategyReturnsCachedThreadName() throws Exception {
        final String original = "Original-ThreadName";
        Thread.currentThread().setName(original);
        assertEquals(original, ThreadNameCachingStrategy.CACHED.getThreadName());

        final String name2 = "OTHER-THREADNAME2";
        Thread.currentThread().setName(name2);
        assertEquals(original, ThreadNameCachingStrategy.CACHED.getThreadName());
    }

}
