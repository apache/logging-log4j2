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
package org.apache.logging.log4j.core;

import org.apache.logging.log4j.categories.AsyncLoggers;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Verifies steady state logging is GC-free.
 *
 * @see <a href="https://github.com/google/allocation-instrumenter">https://github.com/google/allocation-instrumenter</a>
 */
@Category(AsyncLoggers.class)
public class GcFreeAsynchronousLoggingTest {

    @Test
    public void testNoAllocationDuringSteadyStateLogging() throws Throwable {
        GcFreeLoggingTestUtil.runTest(getClass());
    }

    /**
     * This code runs in a separate process, instrumented with the Google Allocation Instrumenter.
     */
    public static void main(final String[] args) throws Exception {
        System.setProperty("log4j2.garbagefree.threadContextMap", "true");
        System.setProperty("AsyncLogger.RingBufferSize", "128"); // minimum ringbuffer size
        System.setProperty("Log4jContextSelector", AsyncLoggerContextSelector.class.getName());
        GcFreeLoggingTestUtil.executeLogging("gcFreeLogging.xml", GcFreeAsynchronousLoggingTest.class);
    }
}
