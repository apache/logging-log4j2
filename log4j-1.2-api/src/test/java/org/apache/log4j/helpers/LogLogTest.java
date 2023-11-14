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
package org.apache.log4j.helpers;

import junit.framework.TestCase;

/**
 * Tests {@link LogLog}.
 */
public class LogLogTest extends TestCase {

    /**
     * Create new instance of LogLogTest.
     *
     * @param testName test name
     */
    public LogLogTest(final String testName) {
        super(testName);
    }

    /**
     * Check value of CONFIG_DEBUG_KEY.
     *
     * @deprecated since constant is deprecated
     */
    @Deprecated
    public void testConfigDebugKey() {
        assertEquals("log4j.configDebug", LogLog.CONFIG_DEBUG_KEY);
    }

    /**
     * Check value of DEBUG_KEY.
     */
    public void testDebugKey() {
        assertEquals("log4j.debug", LogLog.DEBUG_KEY);
    }
}
