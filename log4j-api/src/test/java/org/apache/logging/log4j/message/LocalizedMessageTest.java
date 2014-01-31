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

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;

/**
 * Tests LocalizedMessage.
 */
public class LocalizedMessageTest {

    @Test
    public void testMessageFormat() {
        final LocalizedMessage msg = new LocalizedMessage("MF", new Locale("en", "US"), "msg1", new Object[] { "1", "Test" });
        assertEquals("This is test number 1 with string argument Test.", msg.getFormattedMessage());
    }

    @Test
    public void testStringFormat() {
        final LocalizedMessage msg = new LocalizedMessage("SF", new Locale("en", "US"), "msg1", new Object[] { "1", "Test" });
        assertEquals("This is test number 1 with string argument Test.", msg.getFormattedMessage());
    }
}
