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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Locale;
import org.apache.logging.log4j.junit.Mutable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

@ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
public class StringFormattedMessageTest {

    private static final int LOOP_CNT = 500;
    String[] array = new String[LOOP_CNT];

    @Test
    public void testNoArgs() {
        final String testMsg = "Test message %1s";
        StringFormattedMessage msg = new StringFormattedMessage(testMsg, (Object[]) null);
        String result = msg.getFormattedMessage();
        final String expected = "Test message null";
        assertThat(result).isEqualTo(expected);
        final Object[] array = null;
        msg = new StringFormattedMessage(testMsg, array, null);
        result = msg.getFormattedMessage();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void testOneStringArg() {
        final String testMsg = "Test message %1s";
        final StringFormattedMessage msg = new StringFormattedMessage(testMsg, "Apache");
        final String result = msg.getFormattedMessage();
        final String expected = "Test message Apache";
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void testOneIntArgLocaleUs() {
        final String testMsg = "Test e = %+10.4f";
        final StringFormattedMessage msg = new StringFormattedMessage(Locale.US, testMsg, Math.E);
        final String result = msg.getFormattedMessage();
        final String expected = "Test e =    +2.7183";
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void testOneArgLocaleFrance() {
        final String testMsg = "Test e = %+10.4f";
        final StringFormattedMessage msg = new StringFormattedMessage(Locale.FRANCE, testMsg, Math.E);
        final String result = msg.getFormattedMessage();
        final String expected = "Test e =    +2,7183";
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void testException() {
        final String testMsg = "Test message {0}";
        final MessageFormatMessage msg = new MessageFormatMessage(testMsg, "Apache", new NullPointerException("Null"));
        final String result = msg.getFormattedMessage();
        final String expected = "Test message Apache";
        assertThat(result).isEqualTo(expected);
        final Throwable t = msg.getThrowable();
        assertThat(t).describedAs("No Throwable").isNotNull();
    }

    @Test
    public void testUnsafeWithMutableParams() { // LOG4J2-763
        final String testMsg = "Test message %s";
        final Mutable param = new Mutable().set("abc");
        final StringFormattedMessage msg = new StringFormattedMessage(testMsg, param);

        // modify parameter before calling msg.getFormattedMessage
        param.set("XYZ");
        final String actual = msg.getFormattedMessage();
        assertThat(actual).describedAs("Should use initial param value").isEqualTo("Test message XYZ");
    }

    @Test
    public void testSafeAfterGetFormattedMessageIsCalled() { // LOG4J2-763
        final String testMsg = "Test message %s";
        final Mutable param = new Mutable().set("abc");
        final StringFormattedMessage msg = new StringFormattedMessage(testMsg, param);

        // modify parameter after calling msg.getFormattedMessage
        msg.getFormattedMessage();
        param.set("XYZ");
        final String actual = msg.getFormattedMessage();
        assertThat(actual).describedAs("Should use initial param value").isEqualTo("Test message abc");
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        final StringFormattedMessage expected = new StringFormattedMessage("Msg", "a", "b", "c");
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final ObjectOutputStream out = new ObjectOutputStream(baos)) {
            out.writeObject(expected);
        }
        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final ObjectInputStream in = new ObjectInputStream(bais);
        final StringFormattedMessage actual = (StringFormattedMessage) in.readObject();
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.getFormat()).isEqualTo(expected.getFormat());
        assertThat(actual.getFormattedMessage()).isEqualTo(expected.getFormattedMessage());
        assertThat(actual.getParameters()).isEqualTo(expected.getParameters());
    }
}
