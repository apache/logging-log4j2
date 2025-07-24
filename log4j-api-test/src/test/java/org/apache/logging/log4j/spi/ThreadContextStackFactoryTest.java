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
package org.apache.logging.log4j.spi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.apache.logging.log4j.test.junit.UsingAnyThreadContext;
import org.junit.jupiter.api.Test;

@UsingAnyThreadContext
class ThreadContextStackFactoryTest {

    @Test
    void testDefaultThreadContextStack() {
        final ThreadContextStack stack = ThreadContextStackFactory.createThreadContextStack();
        assertNotNull(stack, "ThreadContextStack should not be null");
        assertTrue(stack instanceof DefaultThreadContextStack, "Should return DefaultThreadContextStack by default");
    }

    @Test
    @SetTestProperty(
            key = "log4j2.threadContextStack",
            value = "org.apache.logging.log4j.spi.ThreadContextStackFactoryTest$CustomThreadContextStack")
    void testCustomThreadContextStack() {
        final ThreadContextStack stack = ThreadContextStackFactory.createThreadContextStack();
        assertNotNull(stack, "ThreadContextStack should not be null");
        assertTrue(
                stack instanceof CustomThreadContextStack,
                "Expected CustomThreadContextStack but got " + stack.getClass().getName());

        stack.push("test");
        assertEquals("test", stack.peek(), "Custom stack should work normally");
    }

    @Test
    @SetTestProperty(
            key = "log4j2.threadContextStack",
            value = "org.apache.logging.log4j.spi.ThreadContextStackFactoryTest$VerifiableThreadContextStack")
    void testCustomStackRealBehavior() {
        final ThreadContextStack stack = ThreadContextStackFactory.createThreadContextStack();
        assertTrue(stack instanceof VerifiableThreadContextStack, "Should be VerifiableThreadContextStack");

        VerifiableThreadContextStack verifiableStack = (VerifiableThreadContextStack) stack;

        stack.push("operation1");
        assertEquals("CUSTOM:operation1", stack.peek(), "Push should add custom prefix");
        assertEquals(1, verifiableStack.getCallCount(), "Should track method calls");

        stack.push("operation2");
        assertEquals("CUSTOM:operation2", stack.peek(), "Second push should also have prefix");
        assertEquals(2, verifiableStack.getCallCount(), "Call count should increment");

        String popped = stack.pop();
        assertEquals("CUSTOM:operation2", popped, "Pop should return prefixed value");
        assertEquals(3, verifiableStack.getCallCount(), "Pop should increment call count");
        assertEquals("CUSTOM:operation1", stack.peek(), "Remaining item should have prefix");

        List<String> stackList = stack.asList();
        assertEquals(1, stackList.size(), "Should have one remaining item");
        assertEquals("CUSTOM:operation1", stackList.get(0), "List should contain prefixed item");
        assertEquals(4, verifiableStack.getCallCount(), "asList should increment call count");

        assertTrue(verifiableStack.wasMethodCalled("push"), "Should track push calls");
        assertTrue(verifiableStack.wasMethodCalled("pop"), "Should track pop calls");
        assertTrue(verifiableStack.wasMethodCalled("asList"), "Should track asList calls");
        assertFalse(verifiableStack.wasMethodCalled("clear"), "Should not track uncalled methods");

        stack.clear();
        assertEquals(5, verifiableStack.getCallCount(), "Clear should also be tracked");
        assertTrue(verifiableStack.wasMethodCalled("clear"), "Should track clear call");
    }

    @Test
    @SetTestProperty(key = "log4j2.threadContextStack", value = "com.nonexistent.StackClass")
    void testInvalidThreadContextStackClass() {
        final ThreadContextStack stack = ThreadContextStackFactory.createThreadContextStack();
        assertNotNull(stack, "ThreadContextStack should not be null");
        assertTrue(
                stack instanceof DefaultThreadContextStack,
                "Should fallback to DefaultThreadContextStack when custom class fails to load");
    }

    @Test
    @SetTestProperty(key = "log4j2.disableThreadContextStack", value = "true")
    void testDisabledThreadContextStack() {
        final ThreadContextStack stack = ThreadContextStackFactory.createThreadContextStack();
        assertNotNull(stack, "ThreadContextStack should not be null");
        assertSame(ThreadContext.NOOP_STACK, stack, "Should return NOOP_STACK when disabled");
    }

    @Test
    @SetTestProperty(key = "log4j2.disableThreadContext", value = "true")
    void testDisabledThreadContext() {
        final ThreadContextStack stack = ThreadContextStackFactory.createThreadContextStack();
        assertNotNull(stack, "ThreadContextStack should not be null");
        assertSame(ThreadContext.NOOP_STACK, stack, "Should return NOOP_STACK when ThreadContext is disabled");
    }

    @Test
    void testFactoryInitDoesNotThrow() {
        assertDoesNotThrow(() -> ThreadContextStackFactory.init(), "ThreadContextStackFactory.init() should not throw");
    }

    @Test
    void testFactoryCreateReturnsNonNull() {
        final ThreadContextStack stack = ThreadContextStackFactory.createThreadContextStack();
        assertNotNull(stack, "createThreadContextStack() should never return null");
    }

    public static class CustomThreadContextStack extends DefaultThreadContextStack {
        public CustomThreadContextStack() {
            super();
        }

        @Override
        public String toString() {
            return "CustomThreadContextStack";
        }
    }

    public static class VerifiableThreadContextStack extends DefaultThreadContextStack {

        private static final String PREFIX = "CUSTOM:";
        private int callCount = 0;
        private final Set<String> calledMethods = new HashSet<>();

        @Override
        public void push(String message) {
            trackCall("push");
            super.push(PREFIX + message);
        }

        @Override
        public String pop() {
            trackCall("pop");
            return super.pop();
        }

        @Override
        public String peek() {
            calledMethods.add("peek");
            return super.peek();
        }

        @Override
        public List<String> asList() {
            trackCall("asList");
            return super.asList();
        }

        @Override
        public void clear() {
            trackCall("clear");
            super.clear();
        }

        private void trackCall(String methodName) {
            callCount++;
            calledMethods.add(methodName);
        }

        public int getCallCount() {
            return callCount;
        }

        public boolean wasMethodCalled(String methodName) {
            return calledMethods.contains(methodName);
        }

        @Override
        public String toString() {
            return "VerifiableThreadContextStack[calls=" + callCount + ", methods=" + calledMethods + "]";
        }
    }
}
