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
package org.apache.logging.log4j.core.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.security.Permission;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.xml.bind.DatatypeConverter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.jackson.Log4jJsonObjectMapper;
import org.apache.logging.log4j.core.jackson.Log4jXmlObjectMapper;
import org.apache.logging.log4j.util.Strings;
import org.junit.Test;

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

    private ThrowableProxy deserialize(final byte[] binary) throws IOException, ClassNotFoundException {
        final ByteArrayInputStream inArr = new ByteArrayInputStream(binary);
        final ObjectInputStream in = new ObjectInputStream(inArr);
        return (ThrowableProxy) in.readObject();
    }

    private byte[] serialize(final ThrowableProxy proxy) throws IOException {
        final ByteArrayOutputStream arr = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(arr);
        out.writeObject(proxy);
        return arr.toByteArray();
    }

    private boolean allLinesContain(final String text, final String containedText) {
        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.isEmpty()) {
                continue;
            }
            if (!line.contains(containedText)) {
                return false;
            }
        }
        return true;
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

    @Test
    public void testIoContainerAsJson() throws IOException {
        testIoContainer(new Log4jJsonObjectMapper());
    }

    @Test
    public void testIoContainerAsXml() throws IOException {
        testIoContainer(new Log4jXmlObjectMapper());
    }

    /**
     * Attempts to instantiate a class that cannot initialize and then logs the stack trace of the Error. The logger
     * must not fail when using {@link ThrowableProxy} to inspect the frames of the stack trace.
     */
    @Test
    public void testLogStackTraceWithClassThatCannotInitialize() {
        try {
            // Try to create the object, which will always fail during class initialization
            final AlwaysThrowsError error = new AlwaysThrowsError();

            // If the error was not triggered then fail
            fail("Test did not throw expected error: " + error);
        } catch (final Throwable e) {
            // Print the stack trace to System.out for informational purposes
            // System.err.println("### Here's the stack trace that we'll log with log4j ###");
            // e.printStackTrace();
            // System.err.println("### End stack trace ###");

            final Logger logger = LogManager.getLogger(getClass());

            // This is the critical portion of the test. The log message must be printed without
            // throwing a java.lang.Error when introspecting the AlwaysThrowError class in the
            // stack trace.
            logger.error(e.getMessage(), e);
            logger.error(e);
        }
    }

    @Test
    public void testLogStackTraceWithClassThatWillCauseSecurityException() throws IOException {
        final SecurityManager sm = System.getSecurityManager();
        try {
            System.setSecurityManager(
                    new SecurityManager() {
                        @Override
                        public void checkPermission(Permission perm) {
                            if (perm instanceof RuntimePermission) {
                                // deny access to the class to trigger the security exception
                                if ("accessClassInPackage.sun.nio.ch".equals(perm.getName())) {
                                    throw new SecurityException(perm.toString());
                                }
                            }
                        }
                    });
            ServerSocketChannel.open().socket().bind(new InetSocketAddress("localhost", 9300));
            ServerSocketChannel.open().socket().bind(new InetSocketAddress("localhost", 9300));
            fail("expected a java.net.BindException");
        } catch (final BindException e) {
            new ThrowableProxy(e);
        } finally {
            // restore the security manager
            System.setSecurityManager(sm);
        }
    }

    @Test
    public void testLogStackTraceWithClassLoaderThatWithCauseSecurityException() throws Exception {
        final SecurityManager sm = System.getSecurityManager();
        try {
            System.setSecurityManager(
                    new SecurityManager() {
                        @Override
                        public void checkPermission(Permission perm) {
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
            dc.doFinal(encrypted);
            fail("expected a javax.crypto.BadPaddingException");
        } catch (final BadPaddingException e) {
            new ThrowableProxy(e);
        } finally {
            // restore the existing security manager
            System.setSecurityManager(sm);
        }
    }

    // DO NOT REMOVE THIS COMMENT:
    // UNCOMMENT WHEN GENERATING SERIALIZED THROWABLEPROXY FOR #testSerializationWithUnknownThrowable
    // public static class DeletedException extends Exception {
    // private static final long serialVersionUID = 1L;
    //
    // public DeletedException(String msg) {
    // super(msg);
    // }
    // };

    @Test
    public void testSerialization() throws Exception {
        final Throwable throwable = new IllegalArgumentException("This is a test");
        final ThrowableProxy proxy = new ThrowableProxy(throwable);
        final byte[] binary = serialize(proxy);
        final ThrowableProxy proxy2 = deserialize(binary);

        assertEquals(proxy.getName(), proxy2.getName());
        assertEquals(proxy.getMessage(), proxy2.getMessage());
        assertEquals(proxy.getCauseProxy(), proxy2.getCauseProxy());
        assertArrayEquals(proxy.getExtendedStackTrace(), proxy2.getExtendedStackTrace());
    }

    @Test
    public void testSerialization_getExtendedStackTraceAsString() throws Exception {
        final Throwable throwable = new IllegalArgumentException("This is a test");
        final ThrowableProxy proxy = new ThrowableProxy(throwable);
        final byte[] binary = serialize(proxy);
        final ThrowableProxy proxy2 = deserialize(binary);

        assertEquals(proxy.getExtendedStackTraceAsString(Strings.EMPTY), proxy2.getExtendedStackTraceAsString(Strings.EMPTY));
    }

    @Test
	public void testSerialization_getExtendedStackTraceAsStringWithNestedThrowableDepth1() throws Exception {
		final Throwable throwable = new RuntimeException(new IllegalArgumentException("This is a test"));
		testSerialization_getExtendedStackTraceAsStringWithNestedThrowable(throwable);
	}

    @Test
	public void testSerialization_getExtendedStackTraceAsStringWithNestedThrowableDepth2() throws Exception {
		final Throwable throwable = new RuntimeException(
				new IllegalArgumentException("This is a test", new IOException("level 2")));
		testSerialization_getExtendedStackTraceAsStringWithNestedThrowable(throwable);
	}

    @Test
	public void testSerialization_getExtendedStackTraceAsStringWithNestedThrowableDepth3() throws Exception {
		final Throwable throwable = new RuntimeException(new IllegalArgumentException("level 1",
				new IOException("level 2", new IllegalStateException("level 3"))));
		testSerialization_getExtendedStackTraceAsStringWithNestedThrowable(throwable);
	}

    private void testSerialization_getExtendedStackTraceAsStringWithNestedThrowable(final Throwable throwable) throws Exception {
        final ThrowableProxy proxy = new ThrowableProxy(throwable);
        final byte[] binary = serialize(proxy);
        final ThrowableProxy proxy2 = deserialize(binary);

        assertEquals(proxy.getExtendedStackTraceAsString(Strings.EMPTY), proxy2.getExtendedStackTraceAsString(Strings.EMPTY));
    }

    @Test
    public void testSerializationWithUnknownThrowable() throws Exception {

        final String msg = "OMG I've been deleted!";

        // DO NOT DELETE THIS COMMENT:
        // UNCOMMENT TO RE-GENERATE SERIALIZED EVENT WHEN UPDATING THIS TEST.
        // final Exception thrown = new DeletedException(msg);
        // final ThrowableProxy proxy = new ThrowableProxy(thrown);
        // final byte[] binary = serialize(proxy);
        // String base64 = DatatypeConverter.printBase64Binary(binary);
        // System.out.println("final String base64 = \"" + base64.replaceAll("\r\n", "\\\\r\\\\n\" +\r\n\"") + "\";");

        final String base64 = "rO0ABXNyADFvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLlRocm93YWJsZVByb3h52cww1Zp7rPoCAAdJABJjb21tb25FbGVtZW50Q291bnRMAApjYXVzZVByb3h5dAAzTG9yZy9hcGFjaGUvbG9nZ2luZy9sb2c0ai9jb3JlL2ltcGwvVGhyb3dhYmxlUHJveHk7WwASZXh0ZW5kZWRTdGFja1RyYWNldAA/W0xvcmcvYXBhY2hlL2xvZ2dpbmcvbG9nNGovY29yZS9pbXBsL0V4dGVuZGVkU3RhY2tUcmFjZUVsZW1lbnQ7TAAQbG9jYWxpemVkTWVzc2FnZXQAEkxqYXZhL2xhbmcvU3RyaW5nO0wAB21lc3NhZ2VxAH4AA0wABG5hbWVxAH4AA1sAEXN1cHByZXNzZWRQcm94aWVzdAA0W0xvcmcvYXBhY2hlL2xvZ2dpbmcvbG9nNGovY29yZS9pbXBsL1Rocm93YWJsZVByb3h5O3hwAAAAAHB1cgA/W0xvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLkV4dGVuZGVkU3RhY2tUcmFjZUVsZW1lbnQ7ys+II6XHz7wCAAB4cAAAABhzcgA8b3JnLmFwYWNoZS5sb2dnaW5nLmxvZzRqLmNvcmUuaW1wbC5FeHRlbmRlZFN0YWNrVHJhY2VFbGVtZW504d7Pusa2kAcCAAJMAA5leHRyYUNsYXNzSW5mb3QANkxvcmcvYXBhY2hlL2xvZ2dpbmcvbG9nNGovY29yZS9pbXBsL0V4dGVuZGVkQ2xhc3NJbmZvO0wAEXN0YWNrVHJhY2VFbGVtZW50dAAdTGphdmEvbGFuZy9TdGFja1RyYWNlRWxlbWVudDt4cHNyADRvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLkV4dGVuZGVkQ2xhc3NJbmZvAAAAAAAAAAECAANaAAVleGFjdEwACGxvY2F0aW9ucQB+AANMAAd2ZXJzaW9ucQB+AAN4cAF0AA10ZXN0LWNsYXNzZXMvdAABP3NyABtqYXZhLmxhbmcuU3RhY2tUcmFjZUVsZW1lbnRhCcWaJjbdhQIABEkACmxpbmVOdW1iZXJMAA5kZWNsYXJpbmdDbGFzc3EAfgADTAAIZmlsZU5hbWVxAH4AA0wACm1ldGhvZE5hbWVxAH4AA3hwAAAAaHQANW9yZy5hcGFjaGUubG9nZ2luZy5sb2c0ai5jb3JlLmltcGwuVGhyb3dhYmxlUHJveHlUZXN0dAAXVGhyb3dhYmxlUHJveHlUZXN0LmphdmF0ACV0ZXN0U2VyaWFsaXphdGlvbldpdGhVbmtub3duVGhyb3dhYmxlc3EAfgAIc3EAfgAMAHEAfgAPdAAIMS43LjBfNTVzcQB+ABD////+dAAkc3VuLnJlZmxlY3QuTmF0aXZlTWV0aG9kQWNjZXNzb3JJbXBscHQAB2ludm9rZTBzcQB+AAhzcQB+AAwAcQB+AA9xAH4AF3NxAH4AEP////9xAH4AGXB0AAZpbnZva2VzcQB+AAhzcQB+AAwAcQB+AA9xAH4AF3NxAH4AEP////90AChzdW4ucmVmbGVjdC5EZWxlZ2F0aW5nTWV0aG9kQWNjZXNzb3JJbXBscHEAfgAec3EAfgAIc3EAfgAMAHEAfgAPcQB+ABdzcQB+ABD/////dAAYamF2YS5sYW5nLnJlZmxlY3QuTWV0aG9kcHEAfgAec3EAfgAIc3EAfgAMAXQADmp1bml0LTQuMTEuamFycQB+AA9zcQB+ABAAAAAvdAApb3JnLmp1bml0LnJ1bm5lcnMubW9kZWwuRnJhbWV3b3JrTWV0aG9kJDF0ABRGcmFtZXdvcmtNZXRob2QuamF2YXQAEXJ1blJlZmxlY3RpdmVDYWxsc3EAfgAIc3EAfgAMAXQADmp1bml0LTQuMTEuamFycQB+AA9zcQB+ABAAAAAMdAAzb3JnLmp1bml0LmludGVybmFsLnJ1bm5lcnMubW9kZWwuUmVmbGVjdGl2ZUNhbGxhYmxldAAXUmVmbGVjdGl2ZUNhbGxhYmxlLmphdmF0AANydW5zcQB+AAhzcQB+AAwBdAAOanVuaXQtNC4xMS5qYXJxAH4AD3NxAH4AEAAAACx0ACdvcmcuanVuaXQucnVubmVycy5tb2RlbC5GcmFtZXdvcmtNZXRob2RxAH4ALHQAEWludm9rZUV4cGxvc2l2ZWx5c3EAfgAIc3EAfgAMAXQADmp1bml0LTQuMTEuamFycQB+AA9zcQB+ABAAAAARdAAyb3JnLmp1bml0LmludGVybmFsLnJ1bm5lcnMuc3RhdGVtZW50cy5JbnZva2VNZXRob2R0ABFJbnZva2VNZXRob2QuamF2YXQACGV2YWx1YXRlc3EAfgAIc3EAfgAMAXQADmp1bml0LTQuMTEuamFycQB+AA9zcQB+ABAAAAEPdAAeb3JnLmp1bml0LnJ1bm5lcnMuUGFyZW50UnVubmVydAARUGFyZW50UnVubmVyLmphdmF0AAdydW5MZWFmc3EAfgAIc3EAfgAMAXQADmp1bml0LTQuMTEuamFycQB+AA9zcQB+ABAAAABGdAAob3JnLmp1bml0LnJ1bm5lcnMuQmxvY2tKVW5pdDRDbGFzc1J1bm5lcnQAG0Jsb2NrSlVuaXQ0Q2xhc3NSdW5uZXIuamF2YXQACHJ1bkNoaWxkc3EAfgAIc3EAfgAMAXQADmp1bml0LTQuMTEuamFycQB+AA9zcQB+ABAAAAAycQB+AE1xAH4ATnEAfgBPc3EAfgAIc3EAfgAMAXQADmp1bml0LTQuMTEuamFycQB+AA9zcQB+ABAAAADudAAgb3JnLmp1bml0LnJ1bm5lcnMuUGFyZW50UnVubmVyJDNxAH4AR3EAfgA0c3EAfgAIc3EAfgAMAXQADmp1bml0LTQuMTEuamFycQB+AA9zcQB+ABAAAAA/dAAgb3JnLmp1bml0LnJ1bm5lcnMuUGFyZW50UnVubmVyJDFxAH4AR3QACHNjaGVkdWxlc3EAfgAIc3EAfgAMAXQADmp1bml0LTQuMTEuamFycQB+AA9zcQB+ABAAAADscQB+AEZxAH4AR3QAC3J1bkNoaWxkcmVuc3EAfgAIc3EAfgAMAXQADmp1bml0LTQuMTEuamFycQB+AA9zcQB+ABAAAAA1cQB+AEZxAH4AR3QACmFjY2VzcyQwMDBzcQB+AAhzcQB+AAwBdAAOanVuaXQtNC4xMS5qYXJxAH4AD3NxAH4AEAAAAOV0ACBvcmcuanVuaXQucnVubmVycy5QYXJlbnRSdW5uZXIkMnEAfgBHcQB+AEFzcQB+AAhzcQB+AAwBdAAOanVuaXQtNC4xMS5qYXJxAH4AD3NxAH4AEAAAATVxAH4ARnEAfgBHcQB+ADRzcQB+AAhzcQB+AAwBdAAELmNwL3EAfgAPc3EAfgAQAAAAMnQAOm9yZy5lY2xpcHNlLmpkdC5pbnRlcm5hbC5qdW5pdDQucnVubmVyLkpVbml0NFRlc3RSZWZlcmVuY2V0ABhKVW5pdDRUZXN0UmVmZXJlbmNlLmphdmFxAH4ANHNxAH4ACHNxAH4ADAF0AAQuY3AvcQB+AA9zcQB+ABAAAAAmdAAzb3JnLmVjbGlwc2UuamR0LmludGVybmFsLmp1bml0LnJ1bm5lci5UZXN0RXhlY3V0aW9udAASVGVzdEV4ZWN1dGlvbi5qYXZhcQB+ADRzcQB+AAhzcQB+AAwBdAAELmNwL3EAfgAPc3EAfgAQAAAB03QANm9yZy5lY2xpcHNlLmpkdC5pbnRlcm5hbC5qdW5pdC5ydW5uZXIuUmVtb3RlVGVzdFJ1bm5lcnQAFVJlbW90ZVRlc3RSdW5uZXIuamF2YXQACHJ1blRlc3Rzc3EAfgAIc3EAfgAMAXQABC5jcC9xAH4AD3NxAH4AEAAAAqtxAH4AgnEAfgCDcQB+AIRzcQB+AAhzcQB+AAwBdAAELmNwL3EAfgAPc3EAfgAQAAABhnEAfgCCcQB+AINxAH4ANHNxAH4ACHNxAH4ADAF0AAQuY3AvcQB+AA9zcQB+ABAAAADFcQB+AIJxAH4Ag3QABG1haW50ABZPTUcgSSd2ZSBiZWVuIGRlbGV0ZWQhcQB+AJJ0AEZvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLlRocm93YWJsZVByb3h5VGVzdCREZWxldGVkRXhjZXB0aW9udXIANFtMb3JnLmFwYWNoZS5sb2dnaW5nLmxvZzRqLmNvcmUuaW1wbC5UaHJvd2FibGVQcm94eTv67QHghaLrOQIAAHhwAAAAAA==";

        final byte[] binaryDecoded = DatatypeConverter.parseBase64Binary(base64);
        final ThrowableProxy proxy2 = deserialize(binaryDecoded);

        assertEquals(this.getClass().getName() + "$DeletedException", proxy2.getName());
        assertEquals(msg, proxy2.getMessage());
    }

    @Test
    public void testSuffix_getExtendedStackTraceAsString() throws Exception {
        final Throwable throwable = new IllegalArgumentException("This is a test");
        final ThrowableProxy proxy = new ThrowableProxy(throwable);

        String suffix = "some suffix";
        assertTrue(allLinesContain(proxy.getExtendedStackTraceAsString(suffix), suffix));
    }

    @Test
    public void testSuffix_getExtendedStackTraceAsStringWithCausedThrowable() throws Exception {
        final Throwable throwable = new RuntimeException(new IllegalArgumentException("This is a test"));
        final ThrowableProxy proxy = new ThrowableProxy(throwable);

        String suffix = "some suffix";
        assertTrue(allLinesContain(proxy.getExtendedStackTraceAsString(suffix), suffix));
    }

    @Test
    public void testSuffix_getExtendedStackTraceAsStringWithSuppressedThrowable() throws Exception {
        IllegalArgumentException cause = new IllegalArgumentException("This is a test");
        final Throwable throwable = new RuntimeException(cause);
        throwable.addSuppressed(new IOException("This is a test"));
        final ThrowableProxy proxy = new ThrowableProxy(throwable);

        String suffix = "some suffix";
        assertTrue(allLinesContain(proxy.getExtendedStackTraceAsString(suffix), suffix));
    }

    @Test
    public void testSuffix_getCauseStackTraceAsString() throws Exception {
        final Throwable throwable = new IllegalArgumentException("This is a test");
        final ThrowableProxy proxy = new ThrowableProxy(throwable);

        String suffix = "some suffix";
        assertTrue(allLinesContain(proxy.getCauseStackTraceAsString(suffix), suffix));
    }

    @Test
    public void testSuffix_getCauseStackTraceAsStringWithCausedThrowable() throws Exception {
        final Throwable throwable = new RuntimeException(new IllegalArgumentException("This is a test"));
        final ThrowableProxy proxy = new ThrowableProxy(throwable);

        String suffix = "some suffix";
        assertTrue(allLinesContain(proxy.getCauseStackTraceAsString(suffix), suffix));
    }

    @Test
    public void testSuffix_getCauseStackTraceAsStringWithSuppressedThrowable() throws Exception {
        IllegalArgumentException cause = new IllegalArgumentException("This is a test");
        final Throwable throwable = new RuntimeException(cause);
        throwable.addSuppressed(new IOException("This is a test"));
        final ThrowableProxy proxy = new ThrowableProxy(throwable);

        String suffix = "some suffix";
        assertTrue(allLinesContain(proxy.getCauseStackTraceAsString(suffix), suffix));
    }

    @Test
    public void testStack() {
        final Map<String, ThrowableProxy.CacheEntry> map = new HashMap<>();
        final Stack<Class<?>> stack = new Stack<>();
        final Throwable throwable = new IllegalStateException("This is a test");
        final ThrowableProxy proxy = new ThrowableProxy(throwable);
        final ExtendedStackTraceElement[] callerPackageData = proxy.toExtendedStackTrace(stack, map, null,
                throwable.getStackTrace());
        assertNotNull("No package data returned", callerPackageData);
    }

    /**
     * Asserts that LOG4J2-834 is solved by constructing a ThrowableProxy over a RuntimeException object thrown at a
     * unloaded known class (already compiled and available as a test resource:
     * org.apache.logging.log4j.core.impl.ForceNoDefClassFoundError.class).
     */
    @Test
    public void testStackWithUnloadableClass() throws Exception {
        final Stack<Class<?>> stack = new Stack<>();
        final Map<String, ThrowableProxy.CacheEntry> map = new HashMap<>();

        final String runtimeExceptionThrownAtUnloadableClass_base64 = "rO0ABXNyABpqYXZhLmxhbmcuUnVudGltZUV4Y2VwdGlvbp5fBkcKNIPlAgAAeHIAE2phdmEubGFuZy5FeGNlcHRpb27Q/R8+GjscxAIAAHhyABNqYXZhLmxhbmcuVGhyb3dhYmxl1cY1Jzl3uMsDAANMAAVjYXVzZXQAFUxqYXZhL2xhbmcvVGhyb3dhYmxlO0wADWRldGFpbE1lc3NhZ2V0ABJMamF2YS9sYW5nL1N0cmluZztbAApzdGFja1RyYWNldAAeW0xqYXZhL2xhbmcvU3RhY2tUcmFjZUVsZW1lbnQ7eHBxAH4ABnB1cgAeW0xqYXZhLmxhbmcuU3RhY2tUcmFjZUVsZW1lbnQ7AkYqPDz9IjkCAAB4cAAAAAFzcgAbamF2YS5sYW5nLlN0YWNrVHJhY2VFbGVtZW50YQnFmiY23YUCAARJAApsaW5lTnVtYmVyTAAOZGVjbGFyaW5nQ2xhc3NxAH4ABEwACGZpbGVOYW1lcQB+AARMAAptZXRob2ROYW1lcQB+AAR4cAAAAAZ0ADxvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLkZvcmNlTm9EZWZDbGFzc0ZvdW5kRXJyb3J0AB5Gb3JjZU5vRGVmQ2xhc3NGb3VuZEVycm9yLmphdmF0AARtYWlueA==";
        final byte[] binaryDecoded = DatatypeConverter
                .parseBase64Binary(runtimeExceptionThrownAtUnloadableClass_base64);
        final ByteArrayInputStream inArr = new ByteArrayInputStream(binaryDecoded);
        final ObjectInputStream in = new ObjectInputStream(inArr);
        final Throwable throwable = (Throwable) in.readObject();
        final ThrowableProxy subject = new ThrowableProxy(throwable);

        subject.toExtendedStackTrace(stack, map, null, throwable.getStackTrace());
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
