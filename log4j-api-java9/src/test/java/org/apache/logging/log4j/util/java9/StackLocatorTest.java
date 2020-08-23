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
package org.apache.logging.log4j.util.java9;

import org.apache.logging.log4j.util.StackLocator;
import org.junit.jupiter.api.Test;

import java.util.Stack;

import static org.junit.jupiter.api.Assertions.*;

public class StackLocatorTest {

    @Test
    public void testGetCallerClass() {
        final Class<?> expected = StackLocatorTest.class;
        final StackLocator stackLocator = StackLocator.getInstance();
        final Class<?> actual = stackLocator.getCallerClass(1);
        assertSame(expected, actual);
    }

    @Test
    public void testGetCallerClassNameViaStackTrace() throws Exception {
        final Class<?> expected = StackLocatorTest.class;
        final Class<?> actual = Class.forName(new Throwable().getStackTrace()[0].getClassName());
        assertSame(expected, actual);
    }

    @Test
    public void testGetCurrentStackTrace() {
        final StackLocator stackLocator = StackLocator.getInstance();
        final Stack<Class<?>> classes = stackLocator.getCurrentStackTrace();
        final Stack<Class<?>> reversed = new Stack<>();
        reversed.ensureCapacity(classes.size());
        while (!classes.empty()) {
            reversed.push(classes.pop());
        }
        while (reversed.peek() != StackLocator.class) {
            reversed.pop();
        }
        reversed.pop(); // ReflectionUtil
        assertSame(StackLocatorTest.class, reversed.pop());
    }

    @Test
    public void testGetCallerClassViaName() {
        Inner.assertCallerClassViaName();
    }

    @Test
    public void testGetCallerClassViaAnchorClass() {
        Inner.assertCallerClassViaAnchorClass();
    }

    private static class Inner {
        private static void assertCallerClassViaName() {
            final Class<?> expected = StackLocatorTest.class;
            final StackLocator stackLocator = StackLocator.getInstance();
            final Class<?> actual = stackLocator.getCallerClass(Inner.class.getName());
            assertSame(expected, actual);
        }

        private static void assertCallerClassViaAnchorClass() {
            final Class<?> expected = StackLocatorTest.class;
            final StackLocator stackLocator = StackLocator.getInstance();
            final Class<?> actual = stackLocator.getCallerClass(Inner.class);
            assertSame(expected, actual);
        }
    }

    @Test
    public void testLocateClass() {
        ClassLocator locator = new ClassLocator();
        Class<?> clazz = locator.locateClass();
        assertNotNull(clazz, "Could not locate class");
        assertEquals(this.getClass(), clazz, "Incorrect class");
    }

    private final class Foo {

        private StackTraceElement foo() {
            return new Bar().bar(); // <--- testCalcLocation() line
        }

    }

    private final class Bar {

        private StackTraceElement bar() {
            return baz();
        }

        private StackTraceElement baz() {
            return quux();
        }

    }

    private StackTraceElement quux() {
        final StackLocator stackLocator = StackLocator.getInstance();
        return stackLocator.calcLocation("org.apache.logging.log4j.util.java9.StackLocatorTest$Bar");
    }

    @Test
    public void testCalcLocation() {
        /*
         * We are setting up a stack trace that looks like:
         *  - org.apache.logging.log4j.util.test.StackLocatorTest#quux(line:118)
         *  - org.apache.logging.log4j.util.test.StackLocatorTest$Bar#baz(line:112)
         *  - org.apache.logging.log4j.util.test.StackLocatorTest$Bar#bar(line:108)
         *  - org.apache.logging.log4j.util.test.StackLocatorTest$Foo(line:100)
         *
         * We are pretending that org.apache.logging.log4j.util.test.StackLocatorTest$Bar is the logging class, and
         * org.apache.logging.log4j.util.test.StackLocatorTest$Foo is where the log line emanated.
         */
        final StackTraceElement element = new Foo().foo();
        assertEquals("org.apache.logging.log4j.util.java9.StackLocatorTest$Foo", element.getClassName());
        assertEquals(96, element.getLineNumber());
    }

    @Test
    public void testCalcLocationWhenNotInTheStack() {
        final StackLocator stackLocator = StackLocator.getInstance();
        final StackTraceElement stackTraceElement = stackLocator.calcLocation("java.util.Logger");
        assertNull(stackTraceElement);
    }

    static class ClassLocator {

        public Class<?> locateClass() {
            final StackLocator stackLocator = StackLocator.getInstance();
            return stackLocator.getCallerClass(ClassLocator.class);
        }
    }

}
