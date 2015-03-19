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
package org.apache.logging.log4j.core.appender;

import java.util.Arrays;

import org.apache.logging.log4j.util.Chars;
import org.junit.Assert;
import org.junit.Test;

public class TlsSyslogFrameTest {
    private static final String TESTMESSAGE = "The quick brown fox jumps over the lazy dog";

    @Test
    public void messageSetByConstructor() {
        final TlsSyslogFrame frame = new TlsSyslogFrame(TESTMESSAGE);
        final byte[] representation = frame.getBytes();
        final byte[] expected = getByteRepresentation(TESTMESSAGE);
        Assert.assertTrue(Arrays.equals(representation, expected));
    }

    @Test
    public void messageSetBySetter() {
        final TlsSyslogFrame frame = new TlsSyslogFrame("Some text");
        frame.setMessage(TESTMESSAGE);
        final byte[] representation = frame.getBytes();
        final byte[] expected = getByteRepresentation(TESTMESSAGE);
        Assert.assertTrue(Arrays.equals(representation, expected));
    }

    @Test
    public void checkGetBytes() {
        final TlsSyslogFrame frame = new TlsSyslogFrame(TESTMESSAGE);
        final byte[] representation = frame.getBytes();
        final byte[] expected = getByteRepresentation(TESTMESSAGE);
        Assert.assertTrue(Arrays.equals(representation, expected));
    }

    private byte[] getByteRepresentation(final String message) {
        final String frame = message.length() + Character.toString(Chars.SPACE) + message;
        final byte[] representation = frame.getBytes();
        return representation;
    }

    @Test
    public void equals() {
        final TlsSyslogFrame first = new TlsSyslogFrame("A message");
        final TlsSyslogFrame second = new TlsSyslogFrame("A message");
        Assert.assertTrue(first.equals(second));
    }

    @Test
    public void notEquals() {
        final TlsSyslogFrame first = new TlsSyslogFrame("A message");
        final TlsSyslogFrame second = new TlsSyslogFrame("B message");
        Assert.assertFalse(first.equals(second));
    }
}