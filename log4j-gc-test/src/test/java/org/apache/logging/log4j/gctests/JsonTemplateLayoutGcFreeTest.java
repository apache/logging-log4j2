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
package org.apache.logging.log4j.gctests;

import org.apache.logging.log4j.core.test.TestConstants;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Verifies steady state logging with JSON Template Layout is GC-free.
 *
 * @see <a href="https://github.com/google/allocation-instrumenter">Google Allocation Instrumenter</a>
 */
@Tag("allocation")
@Tag("functional")
public class JsonTemplateLayoutGcFreeTest {

    @Test
    void test_no_allocation_during_steady_state_logging() throws Exception {
        GcFreeLoggingTestUtil.runTest(getClass());
    }

    /**
     * This code runs in a separate process, instrumented with the Google Allocation Instrumenter.
     */
    public static void main(final String[] args) throws Exception {
        System.setProperty(TestConstants.THREAD_CONTEXT_MAP_GARBAGE_FREE, "true");
        GcFreeLoggingTestUtil.executeLogging("gcFreeJsonTemplateLayoutLogging.xml", JsonTemplateLayoutGcFreeTest.class);
    }
}
