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
package org.apache.logging.log4j.core.lookup;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class StructuredDataLookupTest {

    private static final String TESTKEY = "type";
    private static final String TESTVAL = "Audit";

    @Test
    public void testLookup() {
        final Message msg = new StructuredDataMessage("Test", "This is a test", "Audit");
        final LogEvent event = Log4jLogEvent.newBuilder().setLevel(Level.DEBUG).setMessage(msg).build();
        final StrLookup lookup = new StructuredDataLookup();
        String value = lookup.lookup(event, TESTKEY);
        assertEquals(TESTVAL, value);
        value = lookup.lookup("BadKey");
        assertNull(value);
    }
}
