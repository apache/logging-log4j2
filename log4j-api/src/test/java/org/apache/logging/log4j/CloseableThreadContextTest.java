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
package org.apache.logging.log4j;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link CloseableThreadContext}.
 *
 * @since 2.6
 */
@ResourceLock(value = Resources.SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ)
public class CloseableThreadContextTest {

    private final String key = "key";
    private final String value = "value";

    @BeforeEach
    @AfterEach
    void clearThreadContext() {
        ThreadContext.clearAll();
    }

    @Test
    public void shouldAddAnEntryToTheMap() {
        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.put(key, value)) {
            assertNotNull(ignored);
            assertEquals(value, ThreadContext.get(key));
        }
    }

    @Test
    public void shouldAddTwoEntriesToTheMap() {
        final String key2 = "key2";
        final String value2 = "value2";
        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.put(key, value).put(key2, value2)) {
            assertNotNull(ignored);
            assertEquals(value, ThreadContext.get(key));
            assertEquals(value2, ThreadContext.get(key2));
        }
    }

    @Test
    public void shouldNestEntries() {
        final String oldValue = "oldValue";
        final String innerValue = "innerValue";
        ThreadContext.put(key, oldValue);
        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.put(key, value)) {
            assertNotNull(ignored);
            assertEquals(value, ThreadContext.get(key));
            try (final CloseableThreadContext.Instance ignored2 = CloseableThreadContext.put(key, innerValue)) {
                assertNotNull(ignored2);
                assertEquals(innerValue, ThreadContext.get(key));
            }
            assertEquals(value, ThreadContext.get(key));
        }
        assertEquals(oldValue, ThreadContext.get(key));
    }

    @Test
    public void shouldPreserveOldEntriesFromTheMapWhenAutoClosed() {
        final String oldValue = "oldValue";
        ThreadContext.put(key, oldValue);
        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.put(key, value)) {
            assertNotNull(ignored);
            assertEquals(value, ThreadContext.get(key));
        }
        assertEquals(oldValue, ThreadContext.get(key));
    }

    @Test
    public void ifTheSameKeyIsAddedTwiceTheOriginalShouldBeUsed() {
        final String oldValue = "oldValue";
        final String secondValue = "innerValue";
        ThreadContext.put(key, oldValue);
        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.put(key, value).put(key, secondValue)) {
            assertNotNull(ignored);
            assertEquals(secondValue, ThreadContext.get(key));
        }
        assertEquals(oldValue, ThreadContext.get(key));
    }

    @Test
    public void shouldPushAndPopAnEntryToTheStack() {
        final String message = "message";
        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.push(message)) {
            assertNotNull(ignored);
            assertEquals(message, ThreadContext.peek());
        }
        assertEquals("", ThreadContext.peek());
    }

    @Test
    public void shouldPushAndPopTwoEntriesToTheStack() {
        final String message1 = "message1";
        final String message2 = "message2";
        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.push(message1).push(message2)) {
            assertNotNull(ignored);
            assertEquals(message2, ThreadContext.peek());
        }
        assertEquals("", ThreadContext.peek());
    }

    @Test
    public void shouldPushAndPopAParameterizedEntryToTheStack() {
        final String parameterizedMessage = "message {}";
        final String parameterizedMessageParameter = "param";
        final String formattedMessage = parameterizedMessage.replace("{}", parameterizedMessageParameter);
        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.push(parameterizedMessage,
                parameterizedMessageParameter)) {
            assertNotNull(ignored);
            assertEquals(formattedMessage, ThreadContext.peek());
        }
        assertEquals("", ThreadContext.peek());
    }

    @Test
    public void shouldRemoveAnEntryFromTheMapWhenAutoClosed() {
        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.put(key, value)) {
            assertNotNull(ignored);
            assertEquals(value, ThreadContext.get(key));
        }
        assertFalse(ThreadContext.containsKey(key));
    }

    @Test
    public void shouldAddEntriesToBothStackAndMap() {
        final String stackValue = "something";
        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.put(key, value).push(stackValue)) {
            assertNotNull(ignored);
            assertEquals(value, ThreadContext.get(key));
            assertEquals(stackValue, ThreadContext.peek());
        }
        assertFalse(ThreadContext.containsKey(key));
        assertEquals("", ThreadContext.peek());
    }

    @Test
    public void canReuseCloseableThreadContext() {
        final String stackValue = "something";
        // Create a ctc and close it
        final CloseableThreadContext.Instance ctc = CloseableThreadContext.push(stackValue).put(key, value);
        assertNotNull(ctc);
        assertEquals(value, ThreadContext.get(key));
        assertEquals(stackValue, ThreadContext.peek());
        ctc.close();

        assertFalse(ThreadContext.containsKey(key));
        assertEquals("", ThreadContext.peek());

        final String anotherKey = "key2";
        final String anotherValue = "value2";
        final String anotherStackValue = "something else";
        // Use it again
        ctc.push(anotherStackValue).put(anotherKey, anotherValue);
        assertEquals(anotherValue, ThreadContext.get(anotherKey));
        assertEquals(anotherStackValue, ThreadContext.peek());
        ctc.close();

        assertFalse(ThreadContext.containsKey(anotherKey));
        assertEquals("", ThreadContext.peek());
    }

    @Test
    public void closeIsIdempotent() {

        final String originalMapValue = "map to keep";
        final String originalStackValue = "stack to keep";
        ThreadContext.put(key, originalMapValue);
        ThreadContext.push(originalStackValue);

        final String newMapValue = "temp map value";
        final String newStackValue = "temp stack to keep";
        final CloseableThreadContext.Instance ctc = CloseableThreadContext.push(newStackValue).put(key, newMapValue);
        assertNotNull(ctc);

        ctc.close();
        assertEquals(originalMapValue, ThreadContext.get(key));
        assertEquals(originalStackValue, ThreadContext.peek());

        ctc.close();
        assertEquals(originalMapValue, ThreadContext.get(key));
        assertEquals(originalStackValue, ThreadContext.peek());
    }

    @Test
    public void putAllWillPutAllValues() {

        final String oldValue = "oldValue";
        ThreadContext.put(key, oldValue);

        final Map<String, String> valuesToPut = new HashMap<>();
        valuesToPut.put(key, value);

        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.putAll(valuesToPut)) {
            assertNotNull(ignored);
            assertEquals(value, ThreadContext.get(key));
        }
        assertEquals(oldValue, ThreadContext.get(key));

    }

    @Test
    public void pushAllWillPushAllValues() {

        ThreadContext.push(key);
        final List<String> messages = ThreadContext.getImmutableStack().asList();
        ThreadContext.pop();

        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.pushAll(messages)) {
            assertNotNull(ignored);
            assertEquals(key, ThreadContext.peek());
        }
        assertEquals("", ThreadContext.peek());

    }

}
