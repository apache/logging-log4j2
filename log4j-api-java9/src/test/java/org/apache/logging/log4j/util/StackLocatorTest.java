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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.ParentRunner;

import java.util.Stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@RunWith(BlockJUnit4ClassRunner.class)
public class StackLocatorTest {

    private static StackLocator stackLocator;

    @BeforeClass
    public static void setupClass() {

        stackLocator = StackLocator.getInstance();
    }

    @Test
    public void testGetCallerClass() throws Exception {
        final Class<?> expected = StackLocatorTest.class;
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
    public void testGetCurrentStackTrace() throws Exception {
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
    public void testGetCallerClassViaName() throws Exception {
        final Class<?> expected = BlockJUnit4ClassRunner.class;
        final Class<?> actual = stackLocator.getCallerClass("org.junit.runners.ParentRunner");
        // if this test fails in the future, it's probably because of a JUnit upgrade; check the new stack trace and
        // update this test accordingly
        assertSame(expected, actual);
    }

    @Test
    public void testGetCallerClassViaAnchorClass() throws Exception {
        final Class<?> expected = BlockJUnit4ClassRunner.class;
        final Class<?> actual = stackLocator.getCallerClass(ParentRunner.class);
        // if this test fails in the future, it's probably because of a JUnit upgrade; check the new stack trace and
        // update this test accordingly
        assertSame(expected, actual);
    }

    @Test
    public void testLocateClass() {
        ClassLocator locator = new ClassLocator();
        Class<?> clazz = locator.locateClass();
        assertNotNull("Could not locate class", clazz);
        assertEquals("Incorrect class", this.getClass(), clazz);
    }

    private final class Foo {

        private StackTraceElement foo() {
            return new Bar().bar();
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
        return stackLocator.calcLocation("org.apache.logging.log4j.util.StackLocatorTest$Bar");
    }

    @Test
    public void testCalcLocation() {
        /*
         * We are setting up a stack trace that looks like:
         *  - org.apache.logging.log4j.util.StackLocatorTest#quux(line:118)
         *  - org.apache.logging.log4j.util.StackLocatorTest$Bar#baz(line:112)
         *  - org.apache.logging.log4j.util.StackLocatorTest$Bar#bar(line:108)
         *  - org.apache.logging.log4j.util.StackLocatorTest$Foo(line:100)
         *
         * We are pretending that org.apache.logging.log4j.util.StackLocatorTest$Bar is the logging class, and
         * org.apache.logging.log4j.util.StackLocatorTest$Foo is where the log line emanated.
         */
        final StackTraceElement element = new Foo().foo();
        assertEquals("org.apache.logging.log4j.util.StackLocatorTest$Foo", element.getClassName());
        assertEquals(100, element.getLineNumber());
    }

    class ClassLocator {

        public Class<?> locateClass() {
            return stackLocator.getCallerClass(ClassLocator.class);
        }
    }

}
