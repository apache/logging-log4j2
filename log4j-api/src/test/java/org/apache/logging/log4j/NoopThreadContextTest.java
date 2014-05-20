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
package org.apache.logging.log4j;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests {@link ThreadContext}.
 */
public class NoopThreadContextTest {

    private static final String TRUE = "true";
    private static final String PROPERY_KEY_ALL = "disableThreadContext";
    private static final String PROPERY_KEY_MAP = "disableThreadContextMap";

    @BeforeClass
    public static void before() {
        System.setProperty(PROPERY_KEY_ALL, TRUE);
        System.setProperty(PROPERY_KEY_MAP, TRUE);
        ThreadContext.init();
    }

    @AfterClass
    public static void after() {
        System.clearProperty(PROPERY_KEY_ALL);
        System.clearProperty(PROPERY_KEY_MAP);
        ThreadContext.init();
    }

    @Test
    public void testNoop() {
        ThreadContext.put("Test", "Test");
        final String value = ThreadContext.get("Test");
        assertNull("value was saved", value);
    }


}
