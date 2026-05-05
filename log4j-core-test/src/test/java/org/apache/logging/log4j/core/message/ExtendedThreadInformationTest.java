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
package org.apache.logging.log4j.core.message;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.management.ThreadInfo;
import org.apache.logging.log4j.message.ThreadDumpMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests that ThreadDumpMessage uses ExtendedThreadInformation when available.
 */
class ExtendedThreadInformationTest {
    @Test
    void testMessage() {
        final ThreadDumpMessage msg = new ThreadDumpMessage("Testing");

        final String message = msg.getFormattedMessage();
        // System.out.print(message);
        assertTrue(message.contains(" Id="), "No header");
    }

    @ParameterizedTest
    @EnumSource(Thread.State.class)
    void testMessageWithNullStackTrace(final Thread.State state) {
        obtainMessageWithMissingStackTrace(state, null);
    }

    @ParameterizedTest
    @EnumSource(Thread.State.class)
    void testMessageWithEmptyStackTrace(final Thread.State state) {
        obtainMessageWithMissingStackTrace(state, new StackTraceElement[0]);
    }

    private void obtainMessageWithMissingStackTrace(final Thread.State state, final StackTraceElement[] stackTrace) {
        // setup
        final String threadName = "the thread name";
        final long threadId = 23523L;

        final ThreadInfo threadInfo = mock(ThreadInfo.class);
        when(threadInfo.getStackTrace()).thenReturn(stackTrace);
        when(threadInfo.getThreadName()).thenReturn(threadName);
        when(threadInfo.getThreadId()).thenReturn(threadId);
        when(threadInfo.isSuspended()).thenReturn(true);
        when(threadInfo.isInNative()).thenReturn(true);
        when(threadInfo.getThreadState()).thenReturn(state);

        // given
        final ExtendedThreadInformation sut = new ExtendedThreadInformation(threadInfo);

        // when
        final StringBuilder result = new StringBuilder();
        sut.printThreadInfo(result);

        // then
        assertThat(result.toString(), containsString(threadName));
        assertThat(result.toString(), containsString(state.name()));
        assertThat(result.toString(), containsString(String.valueOf(threadId)));
    }
}
