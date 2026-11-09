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
package org.apache.logging.log4j.core.appender.db.jpa.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.LoaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

@Tag("Appenders.Jpa")
class ThrowableAttributeConverterTest {
    private static final AtomicInteger NON_THROWABLE_INITIALIZATION_COUNT = new AtomicInteger();

    private ThrowableAttributeConverter converter;

    @BeforeEach
    void setUp() {
        this.converter = new ThrowableAttributeConverter();
    }

    @Test
    void testConvert01() {
        final RuntimeException exception = new RuntimeException("My message 01.");

        final String stackTrace = getStackTrace(exception);

        final String converted = this.converter.convertToDatabaseColumn(exception);

        assertNotNull(converted, "The converted value is not correct.");
        assertEquals(stackTrace, converted, "The converted value is not correct.");

        final Throwable reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull(reversed, "The reversed value should not be null.");
        assertEquals(stackTrace, getStackTrace(reversed), "The reversed value is not correct.");
    }

    @Test
    void testConvert02() {
        final SQLException cause2 = new SQLException("This is a test cause.");
        final Error cause1 = new Error(cause2);
        final RuntimeException exception = new RuntimeException("My message 01.", cause1);

        final String stackTrace = getStackTrace(exception);

        final String converted = this.converter.convertToDatabaseColumn(exception);

        assertNotNull(converted, "The converted value is not correct.");
        assertEquals(stackTrace, converted, "The converted value is not correct.");

        final Throwable reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull(reversed, "The reversed value should not be null.");
        assertEquals(stackTrace, getStackTrace(reversed), "The reversed value is not correct.");
    }

    @Test
    void testConvertCyclicCause() {
        final Exception exception1 = new Exception("exception1");
        final Exception exception2 = new Exception("exception2");
        exception1.initCause(exception2);
        exception2.initCause(exception1);
        final String converted = converter.convertToDatabaseColumn(exception1);
        assertTrue(converted.contains("exception1"));
        assertTrue(converted.contains("exception2"));
        assertEquals(1, StringUtils.countMatches(converted, "Caused by "));
    }

    @Test
    void testMessageWithLineBreaksRoundTrips() {
        final String message = "first line\nCaused by java.lang.Error: second line\r\nC:\\new\\path and \\\\n";
        final IllegalStateException exception = new IllegalStateException(message);

        final Throwable reversed =
                this.converter.convertToEntityAttribute(this.converter.convertToDatabaseColumn(exception));

        assertNotNull(reversed);
        assertEquals(message, reversed.getMessage());
        assertNull(reversed.getCause());
        assertEquals(exception.getStackTrace().length, reversed.getStackTrace().length);
    }

    @Test
    void testEscapedMessageEdgeCasesRoundTrip() {
        for (final String message : new String[] {"!LOG4J2!:C:\\new", "ends with\r", "C:\\new"}) {
            final String converted = this.converter.convertToDatabaseColumn(new IllegalStateException(message));
            final Throwable reversed = this.converter.convertToEntityAttribute(converted);

            assertNotNull(reversed);
            assertEquals(message, reversed.getMessage());
        }
        assertTrue(this.converter
                .convertToDatabaseColumn(new IllegalStateException("C:\\new"))
                .startsWith(IllegalStateException.class.getName() + ": C:\\new\n"));
    }

    @Test
    void testThrowableClassIsLoadedFromContextClassLoader() throws Exception {
        final URL testClasses =
                ContextOnlyException.class.getProtectionDomain().getCodeSource().getLocation();
        final Thread thread = Thread.currentThread();
        final ClassLoader original = thread.getContextClassLoader();
        try (final URLClassLoader contextLoader = new URLClassLoader(new URL[] {testClasses}, null)) {
            thread.setContextClassLoader(contextLoader);

            final Throwable reversed =
                    this.converter.convertToEntityAttribute(ContextOnlyException.class.getName() + ": message\n");

            assertNotNull(reversed);
            assertSame(contextLoader, reversed.getClass().getClassLoader());
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    @Test
    @ResourceLock(Resources.SYSTEM_PROPERTIES)
    void testIgnoreTcclLoadsThrowableClassFromLog4jClassLoader() throws Exception {
        final URL testClasses =
                ContextOnlyException.class.getProtectionDomain().getCodeSource().getLocation();
        final Thread thread = Thread.currentThread();
        final ClassLoader original = thread.getContextClassLoader();
        System.setProperty(LoaderUtil.IGNORE_TCCL_PROPERTY, "true");
        try (final URLClassLoader contextLoader = new URLClassLoader(new URL[] {testClasses}, null)) {
            thread.setContextClassLoader(contextLoader);

            final Throwable reversed =
                    this.converter.convertToEntityAttribute(ContextOnlyException.class.getName() + ": message\n");

            assertNotNull(reversed);
            assertSame(ContextOnlyException.class, reversed.getClass());
        } finally {
            thread.setContextClassLoader(original);
            System.clearProperty(LoaderUtil.IGNORE_TCCL_PROPERTY);
        }
    }

    @Test
    void testMalformedStackTraceLineIsIgnored() {
        final String serialized = IllegalStateException.class.getName()
                + ": message\n"
                + "com.example.Text.line(not a frame)\n"
                + "\tat not a frame\n"
                + "\tat java.lang.Thread.run(Thread.java:1)\n";

        final Throwable reversed = this.converter.convertToEntityAttribute(serialized);

        assertNotNull(reversed);
        assertEquals("message", reversed.getMessage());
        assertEquals(1, reversed.getStackTrace().length);
        assertEquals("run", reversed.getStackTrace()[0].getMethodName());
    }

    @Test
    void testNonThrowableClassIsNotInitialized() {
        assertEquals(0, NON_THROWABLE_INITIALIZATION_COUNT.get());

        final String serialized = NonThrowableWithStaticInitializer.class.getName() + ": message\n";
        assertNull(this.converter.convertToEntityAttribute(serialized));

        assertEquals(0, NON_THROWABLE_INITIALIZATION_COUNT.get());
    }

    @Test
    void testConvertNullToDatabaseColumn() {
        assertNull(this.converter.convertToDatabaseColumn(null), "The converted value should be null.");
    }

    @Test
    void testConvertNullOrBlankToEntityAttribute() {
        assertNull(this.converter.convertToEntityAttribute(null), "The converted attribute should be null (1).");
        assertNull(this.converter.convertToEntityAttribute(""), "The converted attribute should be null (2).");
    }

    private static String getStackTrace(final Throwable throwable) {
        String returnValue = throwable.toString() + '\n';

        for (final StackTraceElement element : throwable.getStackTrace()) {
            returnValue += "\tat " + element.toString() + '\n';
        }

        if (throwable.getCause() != null) {
            returnValue += "Caused by " + getStackTrace(throwable.getCause());
        }

        return returnValue;
    }

    public static class ContextOnlyException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public ContextOnlyException(final String message) {
            super(message);
        }
    }

    private static class NonThrowableWithStaticInitializer {
        static {
            NON_THROWABLE_INITIALIZATION_COUNT.incrementAndGet();
        }
    }
}
