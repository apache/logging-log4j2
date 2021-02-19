/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional rmation regarding copyright ownership.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import org.apache.logging.log4j.categories.AsyncLoggers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(AsyncLoggers.class)
public class AsyncLoggerThreadNameStrategyTest {
    @After
    public void after() {
        System.clearProperty("AsyncLogger.ThreadNameStrategy");
    }

    @Before
    public void before() {
        System.clearProperty("AsyncLogger.ThreadNameStrategy");
    }

    @Test
    public void testDefaultIfNotConfigured() throws Exception {
        final ThreadNameCachingStrategy tns = ThreadNameCachingStrategy.create();
        assertThat(tns).isSameAs(ThreadNameCachingStrategy.DEFAULT_STRATEGY);
    }

    @Test
    public void testDefaultIfInvalidConfig() throws Exception {
        System.setProperty("AsyncLogger.ThreadNameStrategy", "\\%%InValid ");
        final ThreadNameCachingStrategy tns = ThreadNameCachingStrategy.create();
        assertThat(tns).isSameAs(ThreadNameCachingStrategy.DEFAULT_STRATEGY);
    }

    @Test
    public void testUseCachedThreadNameIfConfigured() throws Exception {
        System.setProperty("AsyncLogger.ThreadNameStrategy", "CACHED");
        final ThreadNameCachingStrategy tns = ThreadNameCachingStrategy.create();
        assertThat(tns).isSameAs(ThreadNameCachingStrategy.CACHED);
    }

    @Test
    public void testUseUncachedThreadNameIfConfigured() throws Exception {
        System.setProperty("AsyncLogger.ThreadNameStrategy", "UNCACHED");
        final ThreadNameCachingStrategy tns = ThreadNameCachingStrategy.create();
        assertThat(tns).isSameAs(ThreadNameCachingStrategy.UNCACHED);
    }

    @Test
    public void testUncachedThreadNameStrategyReturnsCurrentThreadName() throws Exception {
        final String name1 = "MODIFIED-THREADNAME1";
        Thread.currentThread().setName(name1);
        assertThat(ThreadNameCachingStrategy.UNCACHED.getThreadName()).isEqualTo(name1);

        final String name2 = "OTHER-THREADNAME2";
        Thread.currentThread().setName(name2);
        assertThat(ThreadNameCachingStrategy.UNCACHED.getThreadName()).isEqualTo(name2);
    }

    @Test
    public void testCachedThreadNameStrategyReturnsCachedThreadName() throws Exception {
        final String original = "Original-ThreadName";
        Thread.currentThread().setName(original);
        assertThat(ThreadNameCachingStrategy.CACHED.getThreadName()).isEqualTo(original);

        final String name2 = "OTHER-THREADNAME2";
        Thread.currentThread().setName(name2);
        assertThat(ThreadNameCachingStrategy.CACHED.getThreadName()).isEqualTo(original);
    }

}
