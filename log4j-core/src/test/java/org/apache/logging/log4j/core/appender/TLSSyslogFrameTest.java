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

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class TLSSyslogFrameTest {
    private static final String TESTMESSAGE = "The quick brown fox jumps over the lazy dog";

    @Test
    public void messageSetByConstructor() {
        TLSSyslogFrame frame = new TLSSyslogFrame(TESTMESSAGE);
        byte[] representation = frame.getBytes();
        byte[] expected = getByteRepresentation(TESTMESSAGE);
        Assert.assertTrue(Arrays.equals(representation, expected));
    }

    @Test
    public void messageSetBySetter() {
        TLSSyslogFrame frame = new TLSSyslogFrame("Some text");
        frame.setMessage(TESTMESSAGE);
        byte[] representation = frame.getBytes();
        byte[] expected = getByteRepresentation(TESTMESSAGE);
        Assert.assertTrue(Arrays.equals(representation, expected));
    }

    @Test
    public void checkGetBytes() {
        TLSSyslogFrame frame = new TLSSyslogFrame(TESTMESSAGE);
        byte[] representation = frame.getBytes();
        byte[] expected = getByteRepresentation(TESTMESSAGE);
        Assert.assertTrue(Arrays.equals(representation, expected));
    }

    private byte[] getByteRepresentation(String message) {
        String frame = message.length() + Character.toString(TLSSyslogFrame.SPACE) + message;
        byte[] representation = frame.getBytes();
        return representation;
    }

    @Test
    public void equals() {
        TLSSyslogFrame first = new TLSSyslogFrame("A message");
        TLSSyslogFrame second = new TLSSyslogFrame("A message");
        Assert.assertTrue(first.equals(second));
    }

    @Test
    public void notEquals() {
        TLSSyslogFrame first = new TLSSyslogFrame("A message");
        TLSSyslogFrame second = new TLSSyslogFrame("B message");
        Assert.assertFalse(first.equals(second));
    }
}