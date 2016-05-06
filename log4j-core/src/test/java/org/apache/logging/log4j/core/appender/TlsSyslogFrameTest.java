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

import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.util.Chars;
import org.junit.Assert;
import org.junit.Test;

public class TlsSyslogFrameTest {
    private static final String TEST_MESSAGE = "The quick brown fox jumps over the lazy dog";

    @Test
    public void equals() {
        final TlsSyslogFrame first = new TlsSyslogFrame(TEST_MESSAGE);
        final TlsSyslogFrame second = new TlsSyslogFrame(TEST_MESSAGE);
        Assert.assertEquals(first, second);
        Assert.assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void notEquals() {
        final TlsSyslogFrame first = new TlsSyslogFrame("A message");
        final TlsSyslogFrame second = new TlsSyslogFrame("B message");
        Assert.assertNotEquals(first, second);
        Assert.assertNotEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void testToString() {
        final TlsSyslogFrame frame = new TlsSyslogFrame(TEST_MESSAGE);
        final int length = TEST_MESSAGE.getBytes(StandardCharsets.UTF_8).length;
        final String expected = Integer.toString(length) + Chars.SPACE + TEST_MESSAGE;
        Assert.assertEquals(expected, frame.toString());
    }
}