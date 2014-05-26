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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.core.jackson.Log4jJsonObjectMapper;
import org.junit.Test;

import sun.misc.BASE64Decoder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.*;

/**
 *
 */
public class ThrowableProxyTest {

    static class Fixture {
        @JsonProperty
        ThrowableProxy proxy = new ThrowableProxy(new IOException("test"));
    }

    @Test
    public void testJsonIoContainer() throws IOException {
        ObjectMapper objectMapper = new Log4jJsonObjectMapper();
        Fixture expected = new Fixture();
        final String s = objectMapper.writeValueAsString(expected);
        Fixture actual = objectMapper.readValue(s, Fixture.class);
        assertEquals(expected.proxy.getName(), actual.proxy.getName());
        assertEquals(expected.proxy.getMessage(), actual.proxy.getMessage());
        assertEquals(expected.proxy.getLocalizedMessage(), actual.proxy.getLocalizedMessage());
        assertEquals(expected.proxy.getCommonElementCount(), actual.proxy.getCommonElementCount());
        assertArrayEquals(expected.proxy.getExtendedStackTrace(), actual.proxy.getExtendedStackTrace());
        assertEquals(expected.proxy, actual.proxy);
    }

    @Test
    public void testStack() {
        final Map<String, ThrowableProxy.CacheEntry> map = new HashMap<String, ThrowableProxy.CacheEntry>();
        final Stack<Class<?>> stack = new Stack<Class<?>>();
        final Throwable throwable = new IllegalStateException("This is a test");
        final ThrowableProxy proxy = new ThrowableProxy(throwable);
        final ExtendedStackTraceElement[] callerPackageData = proxy.toExtendedStackTrace(stack, map, null,
                throwable.getStackTrace());
        assertNotNull("No package data returned", callerPackageData);
    }

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
    public void testSerializationWithUnknownThrowable() throws Exception {

        final String msg = "OMG I've been deleted!";
        
        // DO NOT DELETE THIS COMMENT:
        // UNCOMMENT TO RE-GENERATE SERIALIZED EVENT WHEN UPDATING THIS TEST.
        // final Exception thrown = new DeletedException(msg);
        // final ThrowableProxy proxy = new ThrowableProxy(thrown);
        // final byte[] binary = serialize(proxy);
        // String base64 = new BASE64Encoder().encode(binary);
        // System.out.println("final String base64 = \"" + base64.replaceAll("\r\n", "\\\\r\\\\n\" +\r\n\"") + "\";");

         final String base64 = "rO0ABXNyADFvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLlRocm93YWJsZVByb3h5\r\n" +
                 "2cww1Zp7rPoCAAdJABJjb21tb25FbGVtZW50Q291bnRMAApjYXVzZVByb3h5dAAzTG9yZy9hcGFj\r\n" +
                 "aGUvbG9nZ2luZy9sb2c0ai9jb3JlL2ltcGwvVGhyb3dhYmxlUHJveHk7WwASZXh0ZW5kZWRTdGFj\r\n" +
                 "a1RyYWNldAA/W0xvcmcvYXBhY2hlL2xvZ2dpbmcvbG9nNGovY29yZS9pbXBsL0V4dGVuZGVkU3Rh\r\n" +
                 "Y2tUcmFjZUVsZW1lbnQ7TAAQbG9jYWxpemVkTWVzc2FnZXQAEkxqYXZhL2xhbmcvU3RyaW5nO0wA\r\n" +
                 "B21lc3NhZ2VxAH4AA0wABG5hbWVxAH4AA1sAEXN1cHByZXNzZWRQcm94aWVzdAA0W0xvcmcvYXBh\r\n" +
                 "Y2hlL2xvZ2dpbmcvbG9nNGovY29yZS9pbXBsL1Rocm93YWJsZVByb3h5O3hwAAAAAHB1cgA/W0xv\r\n" +
                 "cmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLkV4dGVuZGVkU3RhY2tUcmFjZUVsZW1l\r\n" +
                 "bnQ7ys+II6XHz7wCAAB4cAAAABhzcgA8b3JnLmFwYWNoZS5sb2dnaW5nLmxvZzRqLmNvcmUuaW1w\r\n" +
                 "bC5FeHRlbmRlZFN0YWNrVHJhY2VFbGVtZW504d7Pusa2kAcCAAJMAA5leHRyYUNsYXNzSW5mb3QA\r\n" +
                 "NkxvcmcvYXBhY2hlL2xvZ2dpbmcvbG9nNGovY29yZS9pbXBsL0V4dGVuZGVkQ2xhc3NJbmZvO0wA\r\n" +
                 "EXN0YWNrVHJhY2VFbGVtZW50dAAdTGphdmEvbGFuZy9TdGFja1RyYWNlRWxlbWVudDt4cHNyADRv\r\n" +
                 "cmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLkV4dGVuZGVkQ2xhc3NJbmZvAAAAAAAA\r\n" +
                 "AAECAANaAAVleGFjdEwACGxvY2F0aW9ucQB+AANMAAd2ZXJzaW9ucQB+AAN4cAF0AA10ZXN0LWNs\r\n" +
                 "YXNzZXMvdAABP3NyABtqYXZhLmxhbmcuU3RhY2tUcmFjZUVsZW1lbnRhCcWaJjbdhQIABEkACmxp\r\n" +
                 "bmVOdW1iZXJMAA5kZWNsYXJpbmdDbGFzc3EAfgADTAAIZmlsZU5hbWVxAH4AA0wACm1ldGhvZE5h\r\n" +
                 "bWVxAH4AA3hwAAAAaHQANW9yZy5hcGFjaGUubG9nZ2luZy5sb2c0ai5jb3JlLmltcGwuVGhyb3dh\r\n" +
                 "YmxlUHJveHlUZXN0dAAXVGhyb3dhYmxlUHJveHlUZXN0LmphdmF0ACV0ZXN0U2VyaWFsaXphdGlv\r\n" +
                 "bldpdGhVbmtub3duVGhyb3dhYmxlc3EAfgAIc3EAfgAMAHEAfgAPdAAIMS42LjBfNDVzcQB+ABD/\r\n" +
                 "///+dAAkc3VuLnJlZmxlY3QuTmF0aXZlTWV0aG9kQWNjZXNzb3JJbXBsdAAdTmF0aXZlTWV0aG9k\r\n" +
                 "QWNjZXNzb3JJbXBsLmphdmF0AAdpbnZva2Uwc3EAfgAIc3EAfgAMAHEAfgAPcQB+ABdzcQB+ABAA\r\n" +
                 "AAAncQB+ABlxAH4AGnQABmludm9rZXNxAH4ACHNxAH4ADABxAH4AD3EAfgAXc3EAfgAQAAAAGXQA\r\n" +
                 "KHN1bi5yZWZsZWN0LkRlbGVnYXRpbmdNZXRob2RBY2Nlc3NvckltcGx0ACFEZWxlZ2F0aW5nTWV0\r\n" +
                 "aG9kQWNjZXNzb3JJbXBsLmphdmFxAH4AH3NxAH4ACHNxAH4ADABxAH4AD3EAfgAXc3EAfgAQAAAC\r\n" +
                 "VXQAGGphdmEubGFuZy5yZWZsZWN0Lk1ldGhvZHQAC01ldGhvZC5qYXZhcQB+AB9zcQB+AAhzcQB+\r\n" +
                 "AAwBdAAOanVuaXQtNC4xMS5qYXJxAH4AD3NxAH4AEAAAAC90AClvcmcuanVuaXQucnVubmVycy5t\r\n" +
                 "b2RlbC5GcmFtZXdvcmtNZXRob2QkMXQAFEZyYW1ld29ya01ldGhvZC5qYXZhdAARcnVuUmVmbGVj\r\n" +
                 "dGl2ZUNhbGxzcQB+AAhzcQB+AAwBdAAOanVuaXQtNC4xMS5qYXJxAH4AD3NxAH4AEAAAAAx0ADNv\r\n" +
                 "cmcuanVuaXQuaW50ZXJuYWwucnVubmVycy5tb2RlbC5SZWZsZWN0aXZlQ2FsbGFibGV0ABdSZWZs\r\n" +
                 "ZWN0aXZlQ2FsbGFibGUuamF2YXQAA3J1bnNxAH4ACHNxAH4ADAF0AA5qdW5pdC00LjExLmphcnEA\r\n" +
                 "fgAPc3EAfgAQAAAALHQAJ29yZy5qdW5pdC5ydW5uZXJzLm1vZGVsLkZyYW1ld29ya01ldGhvZHEA\r\n" +
                 "fgAvdAARaW52b2tlRXhwbG9zaXZlbHlzcQB+AAhzcQB+AAwBdAAOanVuaXQtNC4xMS5qYXJxAH4A\r\n" +
                 "D3NxAH4AEAAAABF0ADJvcmcuanVuaXQuaW50ZXJuYWwucnVubmVycy5zdGF0ZW1lbnRzLkludm9r\r\n" +
                 "ZU1ldGhvZHQAEUludm9rZU1ldGhvZC5qYXZhdAAIZXZhbHVhdGVzcQB+AAhzcQB+AAwBdAAOanVu\r\n" +
                 "aXQtNC4xMS5qYXJxAH4AD3NxAH4AEAAAAQ90AB5vcmcuanVuaXQucnVubmVycy5QYXJlbnRSdW5u\r\n" +
                 "ZXJ0ABFQYXJlbnRSdW5uZXIuamF2YXQAB3J1bkxlYWZzcQB+AAhzcQB+AAwBdAAOanVuaXQtNC4x\r\n" +
                 "MS5qYXJxAH4AD3NxAH4AEAAAAEZ0AChvcmcuanVuaXQucnVubmVycy5CbG9ja0pVbml0NENsYXNz\r\n" +
                 "UnVubmVydAAbQmxvY2tKVW5pdDRDbGFzc1J1bm5lci5qYXZhdAAIcnVuQ2hpbGRzcQB+AAhzcQB+\r\n" +
                 "AAwBdAAOanVuaXQtNC4xMS5qYXJxAH4AD3NxAH4AEAAAADJxAH4AUHEAfgBRcQB+AFJzcQB+AAhz\r\n" +
                 "cQB+AAwBdAAOanVuaXQtNC4xMS5qYXJxAH4AD3NxAH4AEAAAAO50ACBvcmcuanVuaXQucnVubmVy\r\n" +
                 "cy5QYXJlbnRSdW5uZXIkM3EAfgBKcQB+ADdzcQB+AAhzcQB+AAwBdAAOanVuaXQtNC4xMS5qYXJx\r\n" +
                 "AH4AD3NxAH4AEAAAAD90ACBvcmcuanVuaXQucnVubmVycy5QYXJlbnRSdW5uZXIkMXEAfgBKdAAI\r\n" +
                 "c2NoZWR1bGVzcQB+AAhzcQB+AAwBdAAOanVuaXQtNC4xMS5qYXJxAH4AD3NxAH4AEAAAAOxxAH4A\r\n" +
                 "SXEAfgBKdAALcnVuQ2hpbGRyZW5zcQB+AAhzcQB+AAwBdAAOanVuaXQtNC4xMS5qYXJxAH4AD3Nx\r\n" +
                 "AH4AEAAAADVxAH4ASXEAfgBKdAAKYWNjZXNzJDAwMHNxAH4ACHNxAH4ADAF0AA5qdW5pdC00LjEx\r\n" +
                 "LmphcnEAfgAPc3EAfgAQAAAA5XQAIG9yZy5qdW5pdC5ydW5uZXJzLlBhcmVudFJ1bm5lciQycQB+\r\n" +
                 "AEpxAH4ARHNxAH4ACHNxAH4ADAF0AA5qdW5pdC00LjExLmphcnEAfgAPc3EAfgAQAAABNXEAfgBJ\r\n" +
                 "cQB+AEpxAH4AN3NxAH4ACHNxAH4ADAF0AAQuY3AvcQB+AA9zcQB+ABAAAAAydAA6b3JnLmVjbGlw\r\n" +
                 "c2UuamR0LmludGVybmFsLmp1bml0NC5ydW5uZXIuSlVuaXQ0VGVzdFJlZmVyZW5jZXQAGEpVbml0\r\n" +
                 "NFRlc3RSZWZlcmVuY2UuamF2YXEAfgA3c3EAfgAIc3EAfgAMAXQABC5jcC9xAH4AD3NxAH4AEAAA\r\n" +
                 "ACZ0ADNvcmcuZWNsaXBzZS5qZHQuaW50ZXJuYWwuanVuaXQucnVubmVyLlRlc3RFeGVjdXRpb250\r\n" +
                 "ABJUZXN0RXhlY3V0aW9uLmphdmFxAH4AN3NxAH4ACHNxAH4ADAF0AAQuY3AvcQB+AA9zcQB+ABAA\r\n" +
                 "AAHTdAA2b3JnLmVjbGlwc2UuamR0LmludGVybmFsLmp1bml0LnJ1bm5lci5SZW1vdGVUZXN0UnVu\r\n" +
                 "bmVydAAVUmVtb3RlVGVzdFJ1bm5lci5qYXZhdAAIcnVuVGVzdHNzcQB+AAhzcQB+AAwBdAAELmNw\r\n" +
                 "L3EAfgAPc3EAfgAQAAACq3EAfgCFcQB+AIZxAH4Ah3NxAH4ACHNxAH4ADAF0AAQuY3AvcQB+AA9z\r\n" +
                 "cQB+ABAAAAGGcQB+AIVxAH4AhnEAfgA3c3EAfgAIc3EAfgAMAXQABC5jcC9xAH4AD3NxAH4AEAAA\r\n" +
                 "AMVxAH4AhXEAfgCGdAAEbWFpbnQAFk9NRyBJJ3ZlIGJlZW4gZGVsZXRlZCFxAH4AlXQARm9yZy5h\r\n" +
                 "cGFjaGUubG9nZ2luZy5sb2c0ai5jb3JlLmltcGwuVGhyb3dhYmxlUHJveHlUZXN0JERlbGV0ZWRF\r\n" +
                 "eGNlcHRpb251cgA0W0xvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLlRocm93YWJs\r\n" +
                 "ZVByb3h5O/rtAeCFous5AgAAeHAAAAAA";


        byte[] binaryDecoded = Base64.decodeBase64(base64);
        final ThrowableProxy proxy2 = deserialize(binaryDecoded);

        assertEquals(this.getClass().getName() + "$DeletedException", proxy2.getName());
        assertEquals(msg, proxy2.getMessage());
    }

    private byte[] serialize(ThrowableProxy proxy) throws IOException {
        final ByteArrayOutputStream arr = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(arr);
        out.writeObject(proxy);
        return arr.toByteArray();
    }

    private ThrowableProxy deserialize(byte[] binary) throws IOException, ClassNotFoundException {
        final ByteArrayInputStream inArr = new ByteArrayInputStream(binary);
        final ObjectInputStream in = new ObjectInputStream(inArr);
        return (ThrowableProxy) in.readObject();
    }
}
