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
package org.apache.log4j.layout;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.DefaultThreadContextStack;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TTCCLayoutTest {

    @Test
    public void test0() {
        DefaultThreadContextStack contextStack = new DefaultThreadContextStack(true);
        contextStack.clear();
        test(contextStack, "");
    }

    @Test
    public void test1() {
        DefaultThreadContextStack contextStack = new DefaultThreadContextStack(true);
        contextStack.clear();
        contextStack.push("foo");
        test(contextStack, "foo ");
    }

    @Test
    public void test2() {
        DefaultThreadContextStack contextStack = new DefaultThreadContextStack(true);
        contextStack.clear();
        contextStack.push("foo");
        contextStack.push("bar");
        test(contextStack, "foo bar ");
    }

    private void test(ThreadContext.ContextStack contextStack, String stackOutput) {
        TTCCLayout layout = TTCCLayout.createLayout();

        Log4jLogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName("a.B")
                .setLevel(org.apache.logging.log4j.Level.DEBUG)
                .setMessage(new SimpleMessage("Msg"))
                .setContextStack(contextStack)
                .setTimeMillis(System.currentTimeMillis() + 17).build();

        String result = layout.toSerializable(event);

        String expected = String.valueOf(event.getTimeMillis() - layout.startTime) +
                ' ' +
                '[' +
                event.getThreadName() +
                "] " +
                event.getLevel().toString() +
                ' ' +
                event.getLoggerName() +
                ' ' +
                stackOutput +
                "- " +
                event.getMessage() +
                System.getProperty("line.separator");

        assertEquals(expected, result);
    }

}
