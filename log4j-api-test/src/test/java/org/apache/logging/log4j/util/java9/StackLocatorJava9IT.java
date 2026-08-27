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
package org.apache.logging.log4j.util.java9;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.apache.logging.log4j.util.StackLocator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests the Java 9 variant of {@link StackLocator} contained in the {@code META-INF/versions/9} directory of
 * {@code log4j-api.jar}.
 * <p>
 * This test must run against the packaged multi-release JAR, hence it is an integration test.
 * </p>
 */
class StackLocatorJava9IT {

    @BeforeAll
    static void assertJava9VariantLoaded() {
        assertThat(StackLocator.class.getResource("StackLocator.class"))
                .asString()
                .as("java9 variant of StackLocator must be loaded from the MRJ")
                .contains("META-INF/versions/9");
    }

    @Test
    void testGetCallerClass() {
        final Class<?> expected = StackLocatorJava9IT.class;
        final StackLocator stackLocator = StackLocator.getInstance();
        final Class<?> actual = stackLocator.getCallerClass(1);
        assertSame(expected, actual);
    }

    @Test
    void testGetCallerClassViaName() {
        Inner.assertCallerClassViaName();
    }

    @Test
    void testGetCallerClassViaAnchorClass() {
        Inner.assertCallerClassViaAnchorClass();
    }

    private static class Inner {
        private static void assertCallerClassViaName() {
            final Class<?> expected = StackLocatorJava9IT.class;
            final StackLocator stackLocator = StackLocator.getInstance();
            final Class<?> actual = stackLocator.getCallerClass(Inner.class.getName(), "");
            assertSame(expected, actual);
        }

        private static void assertCallerClassViaAnchorClass() {
            final Class<?> expected = StackLocatorJava9IT.class;
            final StackLocator stackLocator = StackLocator.getInstance();
            final Class<?> actual = stackLocator.getCallerClass(Inner.class);
            assertSame(expected, actual);
        }
    }

    @Test
    void testLocateClass() {
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
        return stackLocator.calcLocation("org.apache.logging.log4j.util.java9.StackLocatorJava9IT$Bar");
    }

    @Test
    void testCalcLocation() {
        /*
         * We are setting up a stack trace that looks like:
         *  - org.apache.logging.log4j.util.java9.StackLocatorJava9IT#quux
         *  - org.apache.logging.log4j.util.java9.StackLocatorJava9IT$Bar#baz
         *  - org.apache.logging.log4j.util.java9.StackLocatorJava9IT$Bar#bar
         *  - org.apache.logging.log4j.util.java9.StackLocatorJava9IT$Foo#foo
         *
         * We are pretending that org.apache.logging.log4j.util.java9.StackLocatorJava9IT$Bar is the logging class, and
         * org.apache.logging.log4j.util.java9.StackLocatorJava9IT$Foo is where the log line emanated.
         */
        final StackTraceElement element = new Foo().foo();
        assertEquals("org.apache.logging.log4j.util.java9.StackLocatorJava9IT$Foo", element.getClassName());
        // The line number below may need adjustment if this file is changed.
        assertEquals(91, element.getLineNumber());
    }

    @Test
    void testCalcLocationWhenNotInTheStack() {
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
