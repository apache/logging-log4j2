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

import org.junit.Test;

import static org.junit.Assert.*;

public class AsyncLoggerThreadNameStrategyTest {

    @Test
    public void testDefaultThreadNameIsCached() throws Exception {
        final Info.ThreadNameStrategy tns = Info.ThreadNameStrategy.create();
        assertSame(Info.ThreadNameStrategy.CACHED, tns);
    }

    @Test
    public void testUseCachedThreadNameIfInvalidConfig() throws Exception {
        System.setProperty("AsyncLogger.ThreadNameStrategy", "\\%%InValid ");
        final Info.ThreadNameStrategy tns = Info.ThreadNameStrategy.create();
        assertSame(Info.ThreadNameStrategy.CACHED, tns);
    }

    @Test
    public void testUseUncachedThreadNameIfConfigured() throws Exception {
        System.setProperty("AsyncLogger.ThreadNameStrategy", "UNCACHED");
        final Info.ThreadNameStrategy tns = Info.ThreadNameStrategy.create();
        assertSame(Info.ThreadNameStrategy.UNCACHED, tns);
    }

    @Test
    public void testUncachedThreadNameStrategyReturnsCurrentThreadName() throws Exception {
        final Info info = new Info(null, "original", false);
        final String name1 = "MODIFIED-THREADNAME1";
        Thread.currentThread().setName(name1);
        assertEquals(name1, Info.ThreadNameStrategy.UNCACHED.getThreadName(info));

        final String name2 = "OTHER-THREADNAME2";
        Thread.currentThread().setName(name2);
        assertEquals(name2, Info.ThreadNameStrategy.UNCACHED.getThreadName(info));
    }

    @Test
    public void testCachedThreadNameStrategyReturnsCachedThreadName() throws Exception {
        final String original = "Original-ThreadName";
        Thread.currentThread().setName(original);
        final Info info = new Info(null, original, false);
        assertEquals(original, Info.ThreadNameStrategy.CACHED.getThreadName(info));

        final String name2 = "OTHER-THREADNAME2";
        Thread.currentThread().setName(name2);
        assertEquals(original, Info.ThreadNameStrategy.CACHED.getThreadName(info));
    }

}
