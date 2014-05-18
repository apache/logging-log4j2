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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class AsyncLoggerThreadNameStrategyTest {

    @Test
    public void testDefaultThreadNameIsCached() throws Exception {
        AsyncLogger.ThreadNameStrategy tns = AsyncLogger.ThreadNameStrategy.create();
        assertSame(AsyncLogger.ThreadNameStrategy.CACHED, tns);
    }

    @Test
    public void testUseCachedThreadNameIfInvalidConfig() throws Exception {
        System.setProperty("AsyncLogger.ThreadNameStrategy", "\\%%InValid ");
        AsyncLogger.ThreadNameStrategy tns = AsyncLogger.ThreadNameStrategy.create();
        assertSame(AsyncLogger.ThreadNameStrategy.CACHED, tns);
    }

    @Test
    public void testUseUncachedThreadNameIfConfigured() throws Exception {
        System.setProperty("AsyncLogger.ThreadNameStrategy", "UNCACHED");
        AsyncLogger.ThreadNameStrategy tns = AsyncLogger.ThreadNameStrategy.create();
        assertSame(AsyncLogger.ThreadNameStrategy.UNCACHED, tns);
    }

    @Test
    public void testUncachedThreadNameStrategyReturnsCurrentThreadName() throws Exception {
        AsyncLogger.Info info = new AsyncLogger.Info(null, "original", false);
        final String name1 = "MODIFIED-THREADNAME1";
        Thread.currentThread().setName(name1);
        assertEquals(name1, AsyncLogger.ThreadNameStrategy.UNCACHED.getThreadName(info));

        final String name2 = "OTHER-THREADNAME2";
        Thread.currentThread().setName(name2);
        assertEquals(name2, AsyncLogger.ThreadNameStrategy.UNCACHED.getThreadName(info));
    }

    @Test
    public void testCachedThreadNameStrategyReturnsCachedThreadName() throws Exception {
        final String original = "Original-ThreadName";
        Thread.currentThread().setName(original);
        AsyncLogger.Info info = new AsyncLogger.Info(null, original, false);
        assertEquals(original, AsyncLogger.ThreadNameStrategy.CACHED.getThreadName(info));

        final String name2 = "OTHER-THREADNAME2";
        Thread.currentThread().setName(name2);
        assertEquals(original, AsyncLogger.ThreadNameStrategy.CACHED.getThreadName(info));
    }

}
