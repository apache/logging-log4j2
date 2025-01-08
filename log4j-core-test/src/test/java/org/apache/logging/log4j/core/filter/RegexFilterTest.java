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
package org.apache.logging.log4j.core.filter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RegexFilterTest {
    @BeforeAll
    static void before() {
        StatusLogger.getLogger().setLevel(Level.OFF);
    }

    @Test
    void testRegexFilterDoesNotThrowWithAllTheParametersExceptRegexEqualNull() {
        assertDoesNotThrow(() -> {
            RegexFilter.createFilter(".* test .*", null, null, null, null);
        });
    }

    @Test
    void testThresholds() throws Exception {
        RegexFilter filter = RegexFilter.createFilter(".* test .*", null, false, null, null);
        filter.start();
        assertTrue(filter.isStarted());
        assertSame(
                Filter.Result.NEUTRAL, filter.filter(null, Level.DEBUG, null, (Object) "This is a test message", null));
        assertSame(Filter.Result.DENY, filter.filter(null, Level.ERROR, null, (Object) "This is not a test", null));
        LogEvent event = Log4jLogEvent.newBuilder() //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("Another test message")) //
                .build();
        assertSame(Filter.Result.NEUTRAL, filter.filter(event));
        event = Log4jLogEvent.newBuilder() //
                .setLevel(Level.ERROR) //
                .setMessage(new SimpleMessage("test")) //
                .build();
        assertSame(Filter.Result.DENY, filter.filter(event));
        filter = RegexFilter.createFilter(null, null, false, null, null);
        assertNull(filter);
    }

    @Test
    void testDotAllPattern() throws Exception {
        final String singleLine = "test single line matches";
        final String multiLine = "test multi line matches\nsome more lines";
        final RegexFilter filter = RegexFilter.createFilter(
                ".*line.*", new String[] {"DOTALL", "COMMENTS"}, false, Filter.Result.DENY, Filter.Result.ACCEPT);
        final Result singleLineResult = filter.filter(null, null, null, (Object) singleLine, null);
        final Result multiLineResult = filter.filter(null, null, null, (Object) multiLine, null);
        assertThat(singleLineResult, equalTo(Result.DENY));
        assertThat(multiLineResult, equalTo(Result.DENY));
    }

    @Test
    void testNoMsg() throws Exception {
        final RegexFilter filter = RegexFilter.createFilter(".* test .*", null, false, null, null);
        filter.start();
        assertTrue(filter.isStarted());
        assertSame(Filter.Result.DENY, filter.filter(null, Level.DEBUG, null, (Object) null, null));
        assertSame(Filter.Result.DENY, filter.filter(null, Level.DEBUG, null, (Message) null, null));
        assertSame(Filter.Result.DENY, filter.filter(null, Level.DEBUG, null, null, (Object[]) null));
    }

    @Test
    void testParameterizedMsg() throws Exception {
        final String msg = "params {} {}";
        final Object[] params = {"foo", "bar"};

        // match against raw message
        final RegexFilter rawFilter = RegexFilter.createFilter(
                "params \\{\\} \\{\\}",
                null,
                true, // useRawMsg
                Result.ACCEPT,
                Result.DENY);
        final Result rawResult = rawFilter.filter(null, null, null, msg, params);
        assertThat(rawResult, equalTo(Result.ACCEPT));

        // match against formatted message
        final RegexFilter fmtFilter = RegexFilter.createFilter(
                "params foo bar",
                null,
                false, // useRawMsg
                Result.ACCEPT,
                Result.DENY);
        final Result fmtResult = fmtFilter.filter(null, null, null, msg, params);
        assertThat(fmtResult, equalTo(Result.ACCEPT));
    }
}
