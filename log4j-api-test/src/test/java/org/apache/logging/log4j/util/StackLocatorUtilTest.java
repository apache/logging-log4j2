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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.Deque;
import java.util.Stack;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.ParentRunner;

import sun.reflect.Reflection;

/** 
 * Tests {@link StackLocatorUtilTest}.
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class StackLocatorUtilTest {

    @Test
    public void testStackTraceEquivalence() throws Exception {
        for (int i = 1; i < 15; i++) {
            final Class<?> expected = Reflection.getCallerClass(i + StackLocator.JDK_7U25_OFFSET);
            final Class<?> actual = StackLocatorUtil.getCallerClass(i);
            final Class<?> fallbackActual = Class.forName(
                StackLocatorUtil.getStackTraceElement(i).getClassName());
            assertSame(expected, actual);
            assertSame(expected, fallbackActual);
        }
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
        final Stack<Class<?>> reversed = new Stack<>();
        reversed.ensureCapacity(classes.size());
        while (!classes.isEmpty()) {
            reversed.push(classes.removeLast());
        }
        while (reversed.peek() != StackLocatorUtil.class) {
            reversed.pop();
        }
        reversed.pop(); // ReflectionUtil
        assertSame(StackLocatorUtilTest.class, reversed.pop());
    }

    @Test
    public void testGetCallerClassViaName() throws Exception {
        final Class<?> expected = BlockJUnit4ClassRunner.class;
        final Class<?> actual = StackLocatorUtil.getCallerClass("org.junit.runners.ParentRunner");
        // if this test fails in the future, it's probably because of a JUnit upgrade; check the new stack trace and
        // update this test accordingly
        assertSame(expected, actual);
    }

    @Test
    public void testGetCallerClassViaAnchorClass() throws Exception {
        final Class<?> expected = BlockJUnit4ClassRunner.class;
        final Class<?> actual = StackLocatorUtil.getCallerClass(ParentRunner.class);
        // if this test fails in the future, it's probably because of a JUnit upgrade; check the new stack trace and
        // update this test accordingly
        assertSame(expected, actual);
    }

    @Test
    public void testLocateClass() {
        final ClassLocator locator = new ClassLocator();
        final Class<?> clazz = locator.locateClass();
        assertNotNull("Could not locate class", clazz);
        assertEquals("Incorrect class", this.getClass(), clazz);
    }

}
