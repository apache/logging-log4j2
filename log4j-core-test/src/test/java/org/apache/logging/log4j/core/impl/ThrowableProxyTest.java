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
package org.apache.logging.log4j.core.impl;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.security.Permission;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.pattern.PlainTextRenderer;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
public class ThrowableProxyTest {

    public static class AlwaysThrowsError {
        static {
            if (true) {
                throw new Error("I always throw an Error when initialized");
            }
        }
    }

    static class Fixture {
        @JsonProperty
        ThrowableProxy proxy = new ThrowableProxy(new IOException("test"));
    }

    private boolean allLinesContain(final String text, final String containedText) {
        final String[] lines = text.split("\n");
        for (final String line : lines) {
            if (line.isEmpty()) {
                continue;
            }
            if (!line.contains(containedText)) {
                return false;
            }
        }
        return true;
    }

    private boolean lastLineContains(final String text, final String containedText) {
        final String[] lines = text.split("\n");
        final String lastLine = lines[lines.length-1];
        return lastLine.contains(containedText);
    }

    private void testIoContainer(final ObjectMapper objectMapper ) throws IOException {
        final Fixture expected = new Fixture();
        final String s = objectMapper.writeValueAsString(expected);
        final Fixture actual = objectMapper.readValue(s, Fixture.class);
        assertEquals(expected.proxy.getName(), actual.proxy.getName());
        assertEquals(expected.proxy.getMessage(), actual.proxy.getMessage());
        assertEquals(expected.proxy.getLocalizedMessage(), actual.proxy.getLocalizedMessage());
        assertEquals(expected.proxy.getCommonElementCount(), actual.proxy.getCommonElementCount());
        assertArrayEquals(expected.proxy.getExtendedStackTrace(), actual.proxy.getExtendedStackTrace());
        assertEquals(expected.proxy, actual.proxy);
    }

    /**
     * Attempts to instantiate a class that cannot initialize and then logs the stack trace of the Error. The logger
     * must not fail when using {@link ThrowableProxy} to inspect the frames of the stack trace.
     */
    @Test
    public void testLogStackTraceWithClassThatCannotInitialize() {
        final Error e = assertThrows(Error.class, AlwaysThrowsError::new);
        // Print the stack trace to System.out for informational purposes
        // System.err.println("### Here's the stack trace that we'll log with log4j ###");
        // e.printStackTrace();
        // System.err.println("### End stack trace ###");

        final Logger logger = LogManager.getLogger(getClass());

        assertDoesNotThrow(() -> {
            // This is the critical portion of the test. The log message must be printed without
            // throwing a java.lang.Error when introspecting the AlwaysThrowError class in the
            // stack trace.
            logger.error(e.getMessage(), e);
            logger.error(e);
        });
    }

    @Test
    @DisabledForJreRange(min = JRE.JAVA_18) // custom SecurityManager instances throw UnsupportedOperationException
    public void testLogStackTraceWithClassThatWillCauseSecurityException() throws IOException {
        final SecurityManager sm = System.getSecurityManager();
        try {
            System.setSecurityManager(
                    new SecurityManager() {
                        @Override
                        public void checkPermission(final Permission perm) {
                            if (perm instanceof RuntimePermission) {
                                // deny access to the class to trigger the security exception
                                if ("accessClassInPackage.sun.nio.ch".equals(perm.getName())) {
                                    throw new SecurityException(perm.toString());
                                }
                            }
                        }
                    });
            final BindException e = assertThrows(BindException.class, () -> {
                ServerSocketChannel.open().socket().bind(new InetSocketAddress("localhost", 9300));
                ServerSocketChannel.open().socket().bind(new InetSocketAddress("localhost", 9300));
            });
            assertDoesNotThrow(() -> new ThrowableProxy(e));
        } finally {
            // restore the security manager
            System.setSecurityManager(sm);
        }
    }

    @Test
    @DisabledForJreRange(min = JRE.JAVA_18) // custom SecurityManager instances throw UnsupportedOperationException
    public void testLogStackTraceWithClassLoaderThatWithCauseSecurityException() throws Exception {
        final SecurityManager sm = System.getSecurityManager();
        try {
            System.setSecurityManager(
                    new SecurityManager() {
                        @Override
                        public void checkPermission(final Permission perm) {
                            if (perm instanceof RuntimePermission) {
                                // deny access to the classloader to trigger the security exception
                                if ("getClassLoader".equals(perm.getName())) {
                                    throw new SecurityException(perm.toString());
                                }
                            }
                        }
                    });
            final String algorithm = "AES/CBC/PKCS5Padding";
            final Cipher ec = Cipher.getInstance(algorithm);
            final byte[] bytes = new byte[16]; // initialization vector
            final SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(bytes);
            final KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(128);
            final IvParameterSpec algorithmParameterSpec = new IvParameterSpec(bytes);
            ec.init(Cipher.ENCRYPT_MODE, generator.generateKey(), algorithmParameterSpec, secureRandom);
            final byte[] raw = new byte[0];
            final byte[] encrypted = ec.doFinal(raw);
            final Cipher dc = Cipher.getInstance(algorithm);
            dc.init(Cipher.DECRYPT_MODE, generator.generateKey(), algorithmParameterSpec, secureRandom);
            final BadPaddingException e = assertThrows(BadPaddingException.class, () -> dc.doFinal(encrypted));
            assertDoesNotThrow(() -> new ThrowableProxy(e));
        } finally {
            // restore the existing security manager
            System.setSecurityManager(sm);
        }
    }

    @Test
    public void testSeparator_getExtendedStackTraceAsString() throws Exception {
        final Throwable throwable = new IllegalArgumentException("This is a test");
        final ThrowableProxy proxy = new ThrowableProxy(throwable);

        final String separator = " | ";
        final String extendedStackTraceAsString = proxy.getExtendedStackTraceAsString(null,
                PlainTextRenderer.getInstance(), " | ", Strings.EMPTY);
        assertTrue(allLinesContain(extendedStackTraceAsString, separator), extendedStackTraceAsString);
    }

    @Test
    public void testSuffix_getExtendedStackTraceAsString() throws Exception {
        final Throwable throwable = new IllegalArgumentException("This is a test");
        final ThrowableProxy proxy = new ThrowableProxy(throwable);

        final String suffix = "some suffix";
        final String extendedStackTraceAsString = proxy.getExtendedStackTraceAsString(suffix);
        assertTrue(lastLineContains(extendedStackTraceAsString, suffix), extendedStackTraceAsString);
    }

    @Test
    public void testSuffix_getExtendedStackTraceAsStringWithCausedThrowable() throws Exception {
        final Throwable throwable = new RuntimeException(new IllegalArgumentException("This is a test"));
        final ThrowableProxy proxy = new ThrowableProxy(throwable);

        final String suffix = "some suffix";
        assertTrue(allLinesContain(proxy.getExtendedStackTraceAsString(suffix), suffix));
    }

    @Test
    public void testSuffix_getExtendedStackTraceAsStringWithSuppressedThrowable() throws Exception {
        final IllegalArgumentException cause = new IllegalArgumentException("This is a test");
        final Throwable throwable = new RuntimeException(cause);
        throwable.addSuppressed(new IOException("This is a test"));
        final ThrowableProxy proxy = new ThrowableProxy(throwable);

        final String suffix = "some suffix";
        assertTrue(allLinesContain(proxy.getExtendedStackTraceAsString(suffix), suffix));
    }

    @Test
    public void testSuffix_getCauseStackTraceAsString() throws Exception {
        final Throwable throwable = new IllegalArgumentException("This is a test");
        final ThrowableProxy proxy = new ThrowableProxy(throwable);

        final String suffix = "some suffix";
        assertTrue(allLinesContain(proxy.getCauseStackTraceAsString(suffix), suffix));
    }

    @Test
    public void testSuffix_getCauseStackTraceAsStringWithCausedThrowable() throws Exception {
        final Throwable throwable = new RuntimeException(new IllegalArgumentException("This is a test"));
        final ThrowableProxy proxy = new ThrowableProxy(throwable);

        final String suffix = "some suffix";
        assertTrue(allLinesContain(proxy.getCauseStackTraceAsString(suffix), suffix));
    }

    @Test
    public void testSuffix_getCauseStackTraceAsStringWithSuppressedThrowable() throws Exception {
        final IllegalArgumentException cause = new IllegalArgumentException("This is a test");
        final Throwable throwable = new RuntimeException(cause);
        throwable.addSuppressed(new IOException("This is a test"));
        final ThrowableProxy proxy = new ThrowableProxy(throwable);

        final String suffix = "some suffix";
        assertTrue(allLinesContain(proxy.getCauseStackTraceAsString(suffix), suffix));
    }

    @Test
    public void testStack() {
        final Map<String, ThrowableProxyHelper.CacheEntry> map = new HashMap<>();
        final Deque<Class<?>> stack = new ArrayDeque<>();
        final Throwable throwable = new IllegalStateException("This is a test");
        final ThrowableProxy proxy = new ThrowableProxy(throwable);
        final ExtendedStackTraceElement[] callerPackageData = ThrowableProxyHelper.toExtendedStackTrace(proxy, stack, map, null,
                throwable.getStackTrace());
        assertNotNull(callerPackageData, "No package data returned");
    }

    /**
     * Tests LOG4J2-934.
     */
    @Test
    public void testCircularSuppressedExceptions() {
        final Exception e1 = new Exception();
        final Exception e2 = new Exception();
        e2.addSuppressed(e1);
        e1.addSuppressed(e2);
        LogManager.getLogger().error("Error", e1);
    }

    @Test
    public void testSuppressedExceptions() {
        final Exception e = new Exception("Root exception");
        e.addSuppressed(new IOException("Suppressed #1"));
        e.addSuppressed(new IOException("Suppressed #2"));
        LogManager.getLogger().error("Error", e);
        final ThrowableProxy proxy = new ThrowableProxy(e);
        final String extendedStackTraceAsString = proxy.getExtendedStackTraceAsString("same suffix");
        assertTrue(extendedStackTraceAsString.contains("\tSuppressed: java.io.IOException: Suppressed #1"));
        assertTrue(extendedStackTraceAsString.contains("\tSuppressed: java.io.IOException: Suppressed #1"));
    }

    @Test
    public void testCauseSuppressedExceptions() {
        final Exception cause = new Exception("Nested exception");
        cause.addSuppressed(new IOException("Suppressed #1"));
        cause.addSuppressed(new IOException("Suppressed #2"));
        LogManager.getLogger().error("Error", new Exception(cause));
        final ThrowableProxy proxy = new ThrowableProxy(new Exception("Root exception", cause));
        final String extendedStackTraceAsString = proxy.getExtendedStackTraceAsString("same suffix");
        assertTrue(extendedStackTraceAsString.contains("\tSuppressed: java.io.IOException: Suppressed #1"));
        assertTrue(extendedStackTraceAsString.contains("\tSuppressed: java.io.IOException: Suppressed #1"));
    }

    /**
     * Tests LOG4J2-934.
     */
    @Test
    public void testCircularSuppressedNestedException() {
        final Exception e1 = new Exception();
        final Exception e2 = new Exception(e1);
        e2.addSuppressed(e1);
        e1.addSuppressed(e2);
        LogManager.getLogger().error("Error", e1);
    }

    /**
     * .
     */
    @Test
    public void testCircularCauseExceptions() {
        final Exception e1 = new Exception();
        final Exception e2 = new Exception(e1);
        e1.initCause(e2);
        LogManager.getLogger().error("Error", e1);
    }
}
