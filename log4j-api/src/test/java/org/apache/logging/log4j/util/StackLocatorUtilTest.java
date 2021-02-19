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

import static org.apache.logging.log4j.junit.ClassMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeThat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Stack;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.ParentRunner;

@RunWith(BlockJUnit4ClassRunner.class)
public class StackLocatorUtilTest {


    @Test
    public void testStackTraceEquivalence() throws Throwable {
        String reflectionClassName = "sun.reflect.Reflection";
        assumeThat("Running in a JDK without deprecated " + reflectionClassName,
                reflectionClassName, is(loadableClassName()));
        Class<?> reflectionClass = LoaderUtil.loadClass(reflectionClassName);
        MethodHandle getCallerClass = MethodHandles.lookup()
                .findStatic(reflectionClass, "getCallerClass", MethodType.methodType(Class.class, int.class));
        for (int i = 1; i < 15; i++) {
            final Class<?> expected = (Class<?>) getCallerClass.invoke(i + StackLocator.JDK_7u25_OFFSET);
            final Class<?> actual = StackLocatorUtil.getCallerClass(i);
            final Class<?> fallbackActual = Class.forName(
                StackLocatorUtil.getStackTraceElement(i).getClassName());
            assertThat(actual).isSameAs(expected);
            assertThat(fallbackActual).isSameAs(expected);
        }
    }

    @Test
    public void testGetCallerClass() throws Exception {
        final Class<?> expected = StackLocatorUtilTest.class;
        final Class<?> actual = StackLocatorUtil.getCallerClass(1);
        assertThat(actual).isSameAs(expected);
    }

    @Test
    public void testGetCallerClassNameViaStackTrace() throws Exception {
        final Class<?> expected = StackLocatorUtilTest.class;
        final Class<?> actual = Class.forName(new Throwable().getStackTrace()[0].getClassName());
        assertThat(actual).isSameAs(expected);
    }

    @Test
    public void testGetCurrentStackTrace() throws Exception {
        final Stack<Class<?>> classes = StackLocatorUtil.getCurrentStackTrace();
        final Stack<Class<?>> reversed = new Stack<>();
        reversed.ensureCapacity(classes.size());
        while (!classes.empty()) {
            reversed.push(classes.pop());
        }
        while (reversed.peek() != StackLocatorUtil.class) {
            reversed.pop();
        }
        reversed.pop(); // ReflectionUtil
        assertThat(reversed.pop()).isSameAs(StackLocatorUtilTest.class);
    }

    @Test
    public void testGetCallerClassViaName() throws Exception {
        final Class<?> expected = BlockJUnit4ClassRunner.class;
        final Class<?> actual = StackLocatorUtil.getCallerClass("org.junit.runners.ParentRunner");
        // if this test fails in the future, it's probably because of a JUnit upgrade; check the new stack trace and
        // update this test accordingly
        assertThat(actual).isSameAs(expected);
    }

    @Test
    public void testGetCallerClassViaAnchorClass() throws Exception {
        final Class<?> expected = BlockJUnit4ClassRunner.class;
        final Class<?> actual = StackLocatorUtil.getCallerClass(ParentRunner.class);
        // if this test fails in the future, it's probably because of a JUnit upgrade; check the new stack trace and
        // update this test accordingly
        assertThat(actual).isSameAs(expected);
    }

    @Test
    public void testLocateClass() {
        final ClassLocator locator = new ClassLocator();
        final Class<?> clazz = locator.locateClass();
        assertThat(clazz).describedAs("Could not locate class").isNotNull();
        assertThat(clazz).describedAs("Incorrect class").isEqualTo(this.getClass());
    }

}
