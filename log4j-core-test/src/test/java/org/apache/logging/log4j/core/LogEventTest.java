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
package org.apache.logging.log4j.core;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 */
public class LogEventTest {

    private static final Message MESSAGE = new SimpleMessage("This is a test");
    private static final TestClass TESTER = new TestClass();

    @Test
    @Disabled
    public void testEquals() {
        final LogEvent event1 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName()) //
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world!")) //
                .build();
        final LogEvent event2 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName()) //
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world!")) //
                .build();
        final LogEvent event3 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName()) //
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world!")) //
                .build();
        assertNotEquals(event1, event2, "Events should not be equal");
        assertEquals(event2, event3, "Events should be equal");
    }

    @Test
    public void testLocation() {
        final StackTraceElement ste = TESTER.getEventSource(this.getClass().getName());
        assertNotNull(ste, "No StackTraceElement");
        assertEquals(this.getClass().getName(), ste.getClassName(), "Incorrect event");
    }

    private static class TestClass {
        private static final String FQCN = TestClass.class.getName();

        public StackTraceElement getEventSource(final String loggerName) {
            final LogEvent event = Log4jLogEvent.newBuilder().setLoggerName(loggerName)
                    .setLoggerFqcn(FQCN).setLevel(Level.INFO).setMessage(MESSAGE).build();
            event.setIncludeLocation(true);
            return event.getSource();
        }
    }
}
