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
package org.apache.logging.log4j.util;

import java.util.Deque;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.engine.execution.InterceptingExecutableInvoker;
import org.junit.jupiter.engine.execution.InvocationInterceptorChain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class StackLocatorUtilTest {

    @Test
    public void testStackTraceEquivalence() throws Throwable {
        StackTraceElement[] stackTraceElements = expectedStack(new Throwable().getStackTrace());
        for (int i = 1; i < 10; i++) {
            final String expected = stackTraceElements[i-1].getClassName();
            final String actual = StackLocatorUtil.getCallerClass(i).getName();
            final String fallbackActual = Class.forName(
                StackLocatorUtil.getStackTraceElement(i).getClassName()).getName();
            assertSame(expected, actual);
            assertSame(expected, fallbackActual);
        }
    }

    private StackTraceElement[] expectedStack(StackTraceElement[] elements) {
        StackTraceElement[] elementArray = new StackTraceElement[10];
        int i = 0;
        for (int index = 0; index < 10;) {
            if (elements[i].getClassName().startsWith("org.")) {
                elementArray[index] = elements[i];
                ++index;
            }
            ++i;
        }
        return elementArray;
    }

    @Test
    public void testGetCallerClass() throws Exception {
        final Class<?> expected = StackLocatorUtilTest.class;
        final Class<?> actual = StackLocatorUtil.getCallerClass(1);
        assertSame(expected, actual);
    }

    @Test
    public void testGetCallerClassLoader() throws Exception {
        assertSame(StackLocatorUtilTest.class.getClassLoader(), StackLocatorUtil.getCallerClassLoader(1));
    }

    @Test
    public void testGetCallerClassNameViaStackTrace() throws Exception {
        final Class<?> expected = StackLocatorUtilTest.class;
        final Class<?> actual = Class.forName(new Throwable().getStackTrace()[0].getClassName());
        assertSame(expected, actual);
    }

    @Test
    public void testGetCurrentStackTrace() throws Exception {
        final Deque<Class<?>> classes = StackLocatorUtil.getCurrentStackTrace();
        while (classes.peekFirst() != StackLocatorUtil.class) {
            classes.removeFirst();
        }
        classes.removeFirst(); // StackLocatorUtil
        assertSame(StackLocatorUtilTest.class, classes.removeFirst());
    }

    @Test
    public void testTopElementInStackTrace() {
        final StackLocator stackLocator = StackLocator.getInstance();
        final Deque<Class<?>> classes = stackLocator.getCurrentStackTrace();
        //Removing private class in "PrivateSecurityManagerStackTraceUtil"
        classes.removeFirst();
        assertSame(PrivateSecurityManagerStackTraceUtil.class, classes.getFirst());
    }

    @Test
    public void testGetCallerClassViaName() throws Exception {
        final Class<?> expected = InterceptingExecutableInvoker.class;
        final Class<?> actual = StackLocatorUtil.getCallerClass("org.junit.jupiter.engine.execution.InvocationInterceptorChain");
        // if this test fails in the future, it's probably because of a JUnit upgrade; check the new stack trace and
        // update this test accordingly
        assertSame(expected, actual);
    }

    @Test
    public void testGetCallerClassViaAnchorClass() throws Exception {
        final Class<?> expected = InterceptingExecutableInvoker.class;
        final Class<?> actual = StackLocatorUtil.getCallerClass(InvocationInterceptorChain.class);
        // if this test fails in the future, it's probably because of a JUnit upgrade; check the new stack trace and
        // update this test accordingly
        assertSame(expected, actual);
    }

    @Test
    public void testLocateClass() {
        final ClassLocator locator = new ClassLocator();
        final Class<?> clazz = locator.locateClass();
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
        return stackLocator.calcLocation("org.apache.logging.log4j.util.StackLocatorUtilTest$Bar");
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
        assertEquals("org.apache.logging.log4j.util.StackLocatorUtilTest$Foo", element.getClassName());
        // The line number below may need adjustment if this file is changed.
        assertEquals(125, element.getLineNumber());
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
