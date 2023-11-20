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
package org.apache.logging.log4j.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.test.junit.Mutable;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ObjectMessage}.
 */
public class ObjectMessageTest {

    @Test
    public void testNull() {
        final ObjectMessage msg = new ObjectMessage(null);
        final String result = msg.getFormattedMessage();
        assertEquals("null", result);
    }

    @Test
    public void testNotNull() {
        final String testMsg = "Test message {}";
        final ObjectMessage msg = new ObjectMessage(testMsg);
        final String result = msg.getFormattedMessage();
        assertEquals(testMsg, result);
    }

    @Test
    public void testUnsafeWithMutableParams() { // LOG4J2-763
        final Mutable param = new Mutable().set("abc");
        final ObjectMessage msg = new ObjectMessage(param);

        // modify parameter before calling msg.getFormattedMessage
        param.set("XYZ");
        final String actual = msg.getFormattedMessage();
        assertEquals("XYZ", actual, "Expected most recent param value");
    }

    @Test
    public void testSafeAfterGetFormattedMessageIsCalled() { // LOG4J2-763
        final Mutable param = new Mutable().set("abc");
        final ObjectMessage msg = new ObjectMessage(param);

        // modify parameter after calling msg.getFormattedMessage
        msg.getFormattedMessage();
        param.set("XYZ");
        final String actual = msg.getFormattedMessage();
        assertEquals("abc", actual, "Should use initial param value");
    }

    @Test
    public void formatTo_usesCachedMessageString() throws Exception {
        final StringBuilder charSequence = new StringBuilder("initial value");
        final ObjectMessage message = new ObjectMessage(charSequence);
        assertEquals("initial value", message.getFormattedMessage());

        charSequence.setLength(0);
        charSequence.append("different value");

        final StringBuilder result = new StringBuilder();
        message.formatTo(result);
        assertEquals("initial value", result.toString());
    }
}
