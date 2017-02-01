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
package org.apache.logging.log4j.message;

import org.apache.logging.log4j.junit.Mutable;
import org.apache.logging.log4j.util.Constants;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.Locale;

/**
 *
 */
public class FormattedMessageTest {

    private static final String SPACE = Constants.JAVA_MAJOR_VERSION < 9 ? " " : "\u00a0";

    private static final int LOOP_CNT = 500;
    String[] array = new String[LOOP_CNT];

    @Test
    public void testStringNoArgs() {
        final String testMsg = "Test message %1s";
        FormattedMessage msg = new FormattedMessage(testMsg, (Object[]) null);
        String result = msg.getFormattedMessage();
        final String expected = "Test message null";
        assertEquals(expected, result);
        final Object[] array = null;
        msg = new FormattedMessage(testMsg, array, null);
        result = msg.getFormattedMessage();
        assertEquals(expected, result);
    }

    @Test
    public void tesStringOneStringArg() {
        final String testMsg = "Test message %1s";
        final FormattedMessage msg = new FormattedMessage(testMsg, "Apache");
        final String result = msg.getFormattedMessage();
        final String expected = "Test message Apache";
        assertEquals(expected, result);
    }

    @Test
    public void tesStringOneArgLocaleFrance_StringFormattedMessage() {
        final String testMsg = "Test message e = %+10.4f";
        final FormattedMessage msg = new FormattedMessage(Locale.FRANCE, testMsg, Math.E);
        final String result = msg.getFormattedMessage();
        final String expected = "Test message e =    +2,7183";
        assertEquals(expected, result);
    }

    @Test
    public void tesStringOneArgLocaleFrance_MessageFormatMessage() {
        final String testMsg = "Test message {0,number,currency}";
        final FormattedMessage msg = new FormattedMessage(Locale.FRANCE, testMsg, 12);
        final String result = msg.getFormattedMessage();
        final String expected = "Test message 12,00" + SPACE +"€";
        assertEquals(expected, result);
    }

    @Test
    public void tesStringOneArgLocaleUs_MessageFormatMessage() {
        final String testMsg = "Test message {0,number,currency}";
        final FormattedMessage msg = new FormattedMessage(Locale.US, testMsg, 12);
        final String result = msg.getFormattedMessage();
        final String expected = "Test message $12.00";
        assertEquals(expected, result);
    }

    @Test
    public void tesStringOneArgLocaleUs() {
        final String testMsg = "Test message e = %+10.4f";
        final FormattedMessage msg = new FormattedMessage(Locale.US, testMsg, Math.E);
        final String result = msg.getFormattedMessage();
        final String expected = "Test message e =    +2.7183";
        assertEquals(expected, result);
    }

    @Test
    public void testNoArgs() {
        final String testMsg = "Test message {0}";
        FormattedMessage msg = new FormattedMessage(testMsg, (Object[]) null);
        String result = msg.getFormattedMessage();
        final String expected = "Test message {0}";
        assertEquals(expected, result);
        final Object[] array = null;
        msg = new FormattedMessage(testMsg, array, null);
        result = msg.getFormattedMessage();
        assertEquals(expected, result);
    }

    @Test
    public void testOneArg() {
        final String testMsg = "Test message {0}";
        final FormattedMessage msg = new FormattedMessage(testMsg, "Apache");
        final String result = msg.getFormattedMessage();
        final String expected = "Test message Apache";
        assertEquals(expected, result);
    }

    @Test
    public void testParamNoArgs() {
        final String testMsg = "Test message {}";
        FormattedMessage msg = new FormattedMessage(testMsg, null);
        String result = msg.getFormattedMessage();
        assertEquals(testMsg, result);
        final Object[] array = null;
        msg = new FormattedMessage(testMsg, array, null);
        result = msg.getFormattedMessage();
        assertEquals(testMsg, result);
    }

    @Test
    public void testUnsafeWithMutableParams() { // LOG4J2-763
        final String testMsg = "Test message %s";
        final Mutable param = new Mutable().set("abc");
        final FormattedMessage msg = new FormattedMessage(testMsg, param);

        // modify parameter before calling msg.getFormattedMessage
        param.set("XYZ");
        final String actual = msg.getFormattedMessage();
        assertEquals("Expected most recent param value", "Test message XYZ", actual);
    }

    @Test
    public void testSafeAfterGetFormattedMessageIsCalled() { // LOG4J2-763
        final String testMsg = "Test message %s";
        final Mutable param = new Mutable().set("abc");
        final FormattedMessage msg = new FormattedMessage(testMsg, param);

        // modify parameter after calling msg.getFormattedMessage
        msg.getFormattedMessage(); // freeze the formatted message
        param.set("XYZ");
        final String actual = msg.getFormattedMessage();
        assertEquals("Should use initial param value", "Test message abc", actual);
    }
}
