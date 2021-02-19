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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

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
            assertThat(ignored).isNotNull();
            assertThat(ThreadContext.get(key)).isEqualTo(value);
        }
    }

    @Test
    public void shouldAddTwoEntriesToTheMap() {
        final String key2 = "key2";
        final String value2 = "value2";
        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.put(key, value).put(key2, value2)) {
            assertThat(ignored).isNotNull();
            assertThat(ThreadContext.get(key)).isEqualTo(value);
            assertThat(ThreadContext.get(key2)).isEqualTo(value2);
        }
    }

    @Test
    public void shouldNestEntries() {
        final String oldValue = "oldValue";
        final String innerValue = "innerValue";
        ThreadContext.put(key, oldValue);
        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.put(key, value)) {
            assertThat(ignored).isNotNull();
            assertThat(ThreadContext.get(key)).isEqualTo(value);
            try (final CloseableThreadContext.Instance ignored2 = CloseableThreadContext.put(key, innerValue)) {
                assertThat(ignored2).isNotNull();
                assertThat(ThreadContext.get(key)).isEqualTo(innerValue);
            }
            assertThat(ThreadContext.get(key)).isEqualTo(value);
        }
        assertThat(ThreadContext.get(key)).isEqualTo(oldValue);
    }

    @Test
    public void shouldPreserveOldEntriesFromTheMapWhenAutoClosed() {
        final String oldValue = "oldValue";
        ThreadContext.put(key, oldValue);
        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.put(key, value)) {
            assertThat(ignored).isNotNull();
            assertThat(ThreadContext.get(key)).isEqualTo(value);
        }
        assertThat(ThreadContext.get(key)).isEqualTo(oldValue);
    }

    @Test
    public void ifTheSameKeyIsAddedTwiceTheOriginalShouldBeUsed() {
        final String oldValue = "oldValue";
        final String secondValue = "innerValue";
        ThreadContext.put(key, oldValue);
        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.put(key, value).put(key, secondValue)) {
            assertThat(ignored).isNotNull();
            assertThat(ThreadContext.get(key)).isEqualTo(secondValue);
        }
        assertThat(ThreadContext.get(key)).isEqualTo(oldValue);
    }

    @Test
    public void shouldPushAndPopAnEntryToTheStack() {
        final String message = "message";
        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.push(message)) {
            assertThat(ignored).isNotNull();
            assertThat(ThreadContext.peek()).isEqualTo(message);
        }
        assertThat(ThreadContext.peek()).isEmpty();
    }

    @Test
    public void shouldPushAndPopTwoEntriesToTheStack() {
        final String message1 = "message1";
        final String message2 = "message2";
        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.push(message1).push(message2)) {
            assertThat(ignored).isNotNull();
            assertThat(ThreadContext.peek()).isEqualTo(message2);
        }
        assertThat(ThreadContext.peek()).isEmpty();
    }

    @Test
    public void shouldPushAndPopAParameterizedEntryToTheStack() {
        final String parameterizedMessage = "message {}";
        final String parameterizedMessageParameter = "param";
        final String formattedMessage = parameterizedMessage.replace("{}", parameterizedMessageParameter);
        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.push(parameterizedMessage,
                parameterizedMessageParameter)) {
            assertThat(ignored).isNotNull();
            assertThat(ThreadContext.peek()).isEqualTo(formattedMessage);
        }
        assertThat(ThreadContext.peek()).isEmpty();
    }

    @Test
    public void shouldRemoveAnEntryFromTheMapWhenAutoClosed() {
        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.put(key, value)) {
            assertThat(ignored).isNotNull();
            assertThat(ThreadContext.get(key)).isEqualTo(value);
        }
        assertThat(ThreadContext.containsKey(key)).isFalse();
    }

    @Test
    public void shouldAddEntriesToBothStackAndMap() {
        final String stackValue = "something";
        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.put(key, value).push(stackValue)) {
            assertThat(ignored).isNotNull();
            assertThat(ThreadContext.get(key)).isEqualTo(value);
            assertThat(ThreadContext.peek()).isEqualTo(stackValue);
        }
        assertThat(ThreadContext.containsKey(key)).isFalse();
        assertThat(ThreadContext.peek()).isEmpty();
    }

    @Test
    public void canReuseCloseableThreadContext() {
        final String stackValue = "something";
        // Create a ctc and close it
        final CloseableThreadContext.Instance ctc = CloseableThreadContext.push(stackValue).put(key, value);
        assertThat(ctc).isNotNull();
        assertThat(ThreadContext.get(key)).isEqualTo(value);
        assertThat(ThreadContext.peek()).isEqualTo(stackValue);
        ctc.close();

        assertThat(ThreadContext.containsKey(key)).isFalse();
        assertThat(ThreadContext.peek()).isEmpty();

        final String anotherKey = "key2";
        final String anotherValue = "value2";
        final String anotherStackValue = "something else";
        // Use it again
        ctc.push(anotherStackValue).put(anotherKey, anotherValue);
        assertThat(ThreadContext.get(anotherKey)).isEqualTo(anotherValue);
        assertThat(ThreadContext.peek()).isEqualTo(anotherStackValue);
        ctc.close();

        assertThat(ThreadContext.containsKey(anotherKey)).isFalse();
        assertThat(ThreadContext.peek()).isEmpty();
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
        assertThat(ctc).isNotNull();

        ctc.close();
        assertThat(ThreadContext.get(key)).isEqualTo(originalMapValue);
        assertThat(ThreadContext.peek()).isEqualTo(originalStackValue);

        ctc.close();
        assertThat(ThreadContext.get(key)).isEqualTo(originalMapValue);
        assertThat(ThreadContext.peek()).isEqualTo(originalStackValue);
    }

    @Test
    public void putAllWillPutAllValues() {

        final String oldValue = "oldValue";
        ThreadContext.put(key, oldValue);

        final Map<String, String> valuesToPut = new HashMap<>();
        valuesToPut.put(key, value);

        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.putAll(valuesToPut)) {
            assertThat(ignored).isNotNull();
            assertThat(ThreadContext.get(key)).isEqualTo(value);
        }
        assertThat(ThreadContext.get(key)).isEqualTo(oldValue);

    }

    @Test
    public void pushAllWillPushAllValues() {

        ThreadContext.push(key);
        final List<String> messages = ThreadContext.getImmutableStack().asList();
        ThreadContext.pop();

        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.pushAll(messages)) {
            assertThat(ignored).isNotNull();
            assertThat(ThreadContext.peek()).isEqualTo(key);
        }
        assertThat(ThreadContext.peek()).isEmpty();

    }

}
