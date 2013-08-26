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
package org.apache.logging.log4j.core;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.Assert.fail;

/**
 *
 */
public class LogEventTest {

    @Test
    public void testSerialization() throws Exception {
        final LogEvent event1 = new Log4jLogEvent(this.getClass().getName(), null, "org.apache.logging.log4j.core.Logger",
            Level.INFO, new SimpleMessage("Hello, world!"), null);
        final Exception parent = new IllegalStateException("Test");
        final Throwable child = new LoggingException("This is a test", parent);
        final LogEvent event2 = new Log4jLogEvent(this.getClass().getName(), null, "org.apache.logging.log4j.core.Logger",
            Level.INFO, new SimpleMessage("Hello, world!"), child);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(event1);
        oos.writeObject(event2);

        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final ObjectInputStream ois = new ObjectInputStream(bais);
        LogEvent returned;
        try {
            returned = (LogEvent) ois.readObject();
        } catch (final IOException ioe) {
            fail("Exception processing event1");
        }
        try {
            returned = (LogEvent) ois.readObject();
        } catch (final IOException ioe) {
            fail("Exception processing event2");
        }
    }
}
