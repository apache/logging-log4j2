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
package org.apache.logging.log4j.core.lookup;

import static org.apache.logging.log4j.util.Strings.toRootUpperCase;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StructuredDataLookupTest {

    private StructuredDataLookup dataLookup;

    @BeforeEach
    public void setUp() {
        dataLookup = new StructuredDataLookup();
    }

    @Test
    public void testCorrectEvent() {
        final Message msg = new StructuredDataMessage("TestId", "This is a test", "Audit");
        final LogEvent event =
                Log4jLogEvent.newBuilder().setLevel(Level.DEBUG).setMessage(msg).build();

        assertEquals("Audit", dataLookup.lookup(event, StructuredDataLookup.TYPE_KEY));
        assertEquals("Audit", dataLookup.lookup(event, toRootUpperCase(StructuredDataLookup.TYPE_KEY)));
        assertEquals("TestId", dataLookup.lookup(event, StructuredDataLookup.ID_KEY));
        assertEquals("TestId", dataLookup.lookup(event, toRootUpperCase(StructuredDataLookup.ID_KEY)));
        assertNull(dataLookup.lookup(event, "BadKey"));
        assertNull(dataLookup.lookup(event, null));
    }

    @Test
    public void testNullLookup() {
        assertNull(dataLookup.lookup(null, null));
        assertNull(dataLookup.lookup(null));
    }

    @Test
    void testWrongEvent() {
        final LogEvent mockEvent = mock(LogEvent.class);
        // ensure message is not a StructuredDataMessage
        when(mockEvent.getMessage()).thenReturn(mock(Message.class));
        assertNull(dataLookup.lookup(mockEvent, "ignored"));
    }
}
