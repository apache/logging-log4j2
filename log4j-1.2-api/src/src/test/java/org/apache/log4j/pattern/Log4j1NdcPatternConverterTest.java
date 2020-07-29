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
package org.apache.log4j.pattern;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.junit.ThreadContextStackRule;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Log4j1NdcPatternConverterTest {

    @Rule
    public final ThreadContextStackRule threadContextRule = new ThreadContextStackRule();

    @Test
    public void testEmpty() {
        testConverter("");
    }

    @Test
    public void test1() {
        ThreadContext.push("foo");
        testConverter("foo");
    }

    @Test
    public void test2() {
        ThreadContext.push("foo");
        ThreadContext.push("bar");
        testConverter("foo bar");
    }

    @Test
    public void test3() {
        ThreadContext.push("foo");
        ThreadContext.push("bar");
        ThreadContext.push("baz");
        testConverter("foo bar baz");
    }

    private void testConverter(final String expected) {
        final Log4j1NdcPatternConverter converter = Log4j1NdcPatternConverter.newInstance(null);
        final LogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName("MyLogger")
                .setLevel(Level.DEBUG)
                .setMessage(new SimpleMessage("Hello"))
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        assertEquals(expected, sb.toString());
    }

}

