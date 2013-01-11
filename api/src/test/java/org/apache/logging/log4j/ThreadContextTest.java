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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class ThreadContextTest {

    @Test
    public void testPush() {
        ThreadContext.push("Hello");
        ThreadContext.push("{} is {}", ThreadContextTest.class.getSimpleName(), "running");
        assertEquals("Incorrect parameterized stack value", ThreadContext.pop(), "ThreadContextTest is running");
        assertEquals("Incorrect simple stack value", ThreadContext.pop(), "Hello");
    }

    @Test
    public void testInheritance() throws Exception {
        ThreadContext.clear();
        ThreadContext.put("Greeting", "Hello");
        StringBuilder sb = new StringBuilder();
        TestThread thread = new TestThread(sb);
        thread.start();
        thread.join();
        String str = sb.toString();
        assertTrue("Unexpected ThreadContext value. Expected Hello. Actual " + str, "Hello".equals(str));
        sb = new StringBuilder();
        thread = new TestThread(sb);
        thread.start();
        thread.join();
        str = sb.toString();
        assertTrue("Unexpected ThreadContext value. Expected Hello. Actual " + str, "Hello".equals(str));
    }


    private class TestThread extends Thread {

        private final StringBuilder sb;

        public TestThread(final StringBuilder sb) {
            this.sb = sb;
        }

        @Override
        public void run() {
            final String greeting = ThreadContext.get("Greeting");
            if (greeting == null) {
                sb.append("null");
            } else {
                sb.append(greeting);
            }
            ThreadContext.clear();
        }
    }
}
