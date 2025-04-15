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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Locale;
import org.apache.logging.log4j.test.junit.Mutable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

@ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
class StringFormattedMessageTest {

    private static final int LOOP_CNT = 500;
    String[] array = new String[LOOP_CNT];

    @Test
    void testNoArgs() {
        final String testMsg = "Test message %1s";
        StringFormattedMessage msg = new StringFormattedMessage(testMsg, (Object[]) null);
        String result = msg.getFormattedMessage();
        final String expected = "Test message null";
        assertEquals(expected, result);
        final Object[] array = null;
        msg = new StringFormattedMessage(testMsg, array, null);
        result = msg.getFormattedMessage();
        assertEquals(expected, result);
    }

    @Test
    void testOneStringArg() {
        final String testMsg = "Test message %1s";
        final StringFormattedMessage msg = new StringFormattedMessage(testMsg, "Apache");
        final String result = msg.getFormattedMessage();
        final String expected = "Test message Apache";
        assertEquals(expected, result);
    }

    @Test
    void testOneIntArgLocaleUs() {
        final String testMsg = "Test e = %+10.4f";
        final StringFormattedMessage msg = new StringFormattedMessage(Locale.US, testMsg, Math.E);
        final String result = msg.getFormattedMessage();
        final String expected = "Test e =    +2.7183";
        assertEquals(expected, result);
    }

    @Test
    void testOneArgLocaleFrance() {
        final String testMsg = "Test e = %+10.4f";
        final StringFormattedMessage msg = new StringFormattedMessage(Locale.FRANCE, testMsg, Math.E);
        final String result = msg.getFormattedMessage();
        final String expected = "Test e =    +2,7183";
        assertEquals(expected, result);
    }

    @Test
    void testException() {
        final String testMsg = "Test message {0}";
        final MessageFormatMessage msg = new MessageFormatMessage(testMsg, "Apache", new NullPointerException("Null"));
        final String result = msg.getFormattedMessage();
        final String expected = "Test message Apache";
        assertEquals(expected, result);
        final Throwable t = msg.getThrowable();
        assertNotNull(t, "No Throwable");
    }

    @Test
    void testUnsafeWithMutableParams() { // LOG4J2-763
        final String testMsg = "Test message %s";
        final Mutable param = new Mutable().set("abc");
        final StringFormattedMessage msg = new StringFormattedMessage(testMsg, param);

        // modify parameter before calling msg.getFormattedMessage
        param.set("XYZ");
        final String actual = msg.getFormattedMessage();
        assertEquals("Test message XYZ", actual, "Should use initial param value");
    }

    @Test
    void testSafeAfterGetFormattedMessageIsCalled() { // LOG4J2-763
        final String testMsg = "Test message %s";
        final Mutable param = new Mutable().set("abc");
        final StringFormattedMessage msg = new StringFormattedMessage(testMsg, param);

        // modify parameter after calling msg.getFormattedMessage
        msg.getFormattedMessage();
        param.set("XYZ");
        final String actual = msg.getFormattedMessage();
        assertEquals("Test message abc", actual, "Should use initial param value");
    }

    @Test
    void testSerialization() throws IOException, ClassNotFoundException {
        final StringFormattedMessage expected = new StringFormattedMessage("Msg", "a", "b", "c");
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final ObjectOutputStream out = new ObjectOutputStream(baos)) {
            out.writeObject(expected);
        }
        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final ObjectInputStream in = new ObjectInputStream(bais);
        final StringFormattedMessage actual = (StringFormattedMessage) in.readObject();
        assertEquals(expected, actual);
        assertEquals(expected.getFormat(), actual.getFormat());
        assertEquals(expected.getFormattedMessage(), actual.getFormattedMessage());
        assertArrayEquals(expected.getParameters(), actual.getParameters());
    }

    @Test
    void testPercentInMessageNoArgs() {
        // LOG4J2-3458 LocalizedMessage causes a lot of noise on the console
        //
        // ERROR StatusLogger Unable to format msg: C:/Program%20Files/Some%20Company/Some%20Product%20Name/
        // java.util.UnknownFormatConversionException: Conversion = 'F'
        // at java.util.Formatter$FormatSpecifier.conversion(Formatter.java:2691)
        // at java.util.Formatter$FormatSpecifier.<init>(Formatter.java:2720)
        // at java.util.Formatter.parse(Formatter.java:2560)
        // at java.util.Formatter.format(Formatter.java:2501)
        // at java.util.Formatter.format(Formatter.java:2455)
        // at java.lang.String.format(String.java:2981)
        // at org.apache.logging.log4j.message.StringFormattedMessage.formatMessage(StringFormattedMessage.java:120)
        // at
        // org.apache.logging.log4j.message.StringFormattedMessage.getFormattedMessage(StringFormattedMessage.java:88)
        // at
        // org.apache.logging.log4j.message.StringFormattedMessageTest.testPercentInMessageNoArgs(StringFormattedMessageTest.java:153)
        final StringFormattedMessage msg =
                new StringFormattedMessage("C:/Program%20Files/Some%20Company/Some%20Product%20Name/", new Object[] {});
        assertEquals("C:/Program%20Files/Some%20Company/Some%20Product%20Name/", msg.getFormattedMessage());
    }
}
