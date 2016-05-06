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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link CloseableThreadContext}.
 *
 * @since 2.6
 */
public class CloseableThreadContextTest {

    private final String key = "key";
    private final String value = "value";

    @Before
    public void setUp() throws Exception {
        ThreadContext.clearAll();
    }

    @Test
    public void shouldAddAnEntryToTheMap() throws Exception {
        try (final CloseableThreadContext ignored = CloseableThreadContext.put(key, value)) {
            assertThat(ThreadContext.get(key), is(value));
        }
    }

    @Test
    public void shouldAddTwoEntriesToTheMap() throws Exception {
        final String key2 = "key2";
        final String value2 = "value2";
        try (final CloseableThreadContext ignored = CloseableThreadContext.put(key, value, key2, value2)) {
            assertThat(ThreadContext.get(key), is(value));
            assertThat(ThreadContext.get(key2), is(value2));
        }
    }

    @Test
    public void shouldNestEntries() throws Exception {
        final String oldValue = "oldValue";
        final String innerValue = "innerValue";
        ThreadContext.put(key, oldValue);
        try (final CloseableThreadContext ignored = CloseableThreadContext.put(key, value)) {
            assertThat(ThreadContext.get(key), is(value));
            try (final CloseableThreadContext ignored2 = CloseableThreadContext.put(key, innerValue)) {
                assertThat(ThreadContext.get(key), is(innerValue));
            }
            assertThat(ThreadContext.get(key), is(value));
        }
        assertThat(ThreadContext.get(key), is(oldValue));
    }

    @Test
    public void shouldPreserveOldEntriesFromTheMapWhenAutoClosed() throws Exception {
        final String oldValue = "oldValue";
        ThreadContext.put(key, oldValue);
        try (final CloseableThreadContext ignored = CloseableThreadContext.put(key, value)) {
            assertThat(ThreadContext.get(key), is(value));
        }
        assertThat(ThreadContext.get(key), is(oldValue));
    }

    @Test
    public void shouldPushAndPopAnEntryToTheStack() throws Exception {
        final String message = "message";
        try (final CloseableThreadContext ignored = CloseableThreadContext.push(message)) {
            assertThat(ThreadContext.peek(), is(message));
        }
        assertThat(ThreadContext.peek(), is(""));
    }

    @Test
    public void shouldPushAndPopAParameterizedEntryToTheStack() throws Exception {
        final String parameterizedMessage = "message {}";
        final String parameterizedMessageParameter = "param";
        final String formattedMessage = parameterizedMessage.replace("{}", parameterizedMessageParameter);
        try (final CloseableThreadContext ignored = CloseableThreadContext.push(parameterizedMessage,
                parameterizedMessageParameter)) {
            assertThat(ThreadContext.peek(), is(formattedMessage));
        }
        assertThat(ThreadContext.peek(), is(""));
    }

    @Test
    public void shouldRemoveAnEntryFromTheMapWhenAutoClosed() throws Exception {
        try (final CloseableThreadContext ignored = CloseableThreadContext.put(key, value)) {
            assertThat(ThreadContext.get(key), is(value));
        }
        assertThat(ThreadContext.containsKey(key), is(false));
    }

    @Test
    public void shouldUseAnEmptyStringIfNoValueIsSupplied() throws Exception {
        final String key2 = "key2";
        try (final CloseableThreadContext ignored = CloseableThreadContext.put(key, value, key2)) {
            assertThat(ThreadContext.get(key), is(value));
            assertThat(ThreadContext.get(key2), is(""));
        }
    }

}