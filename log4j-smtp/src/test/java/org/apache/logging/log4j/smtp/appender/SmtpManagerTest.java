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
package org.apache.logging.log4j.smtp.appender;

import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.async.RingBufferLogEvent;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.impl.DefaultContextDataFactory;
import org.apache.logging.log4j.core.impl.MementoMessage;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.impl.ThreadContextDataInjector;
import org.apache.logging.log4j.core.time.internal.DummyNanoClock;
import org.apache.logging.log4j.core.time.internal.SystemClock;
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.message.ReusableSimpleMessage;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link SmtpManager}.
 */
public class SmtpManagerTest {

    @Test
    void testCreateManagerName() {
        String managerName = SmtpManager.createManagerName("to", "cc", null, "from", null, "LOG4J2-3107",
                "proto", "smtp.log4j.com", 4711, "username", false, "filter");
        assertEquals("SMTP:to:cc::from::LOG4J2-3107:proto:smtp.log4j.com:4711:username::filter", managerName);
    }

    private void testAdd(LogEvent event) {
        SmtpManager smtpManager = SmtpManager.getSmtpManager(null, "to", "cc", "bcc", "from", "replyTo", "subject", "protocol", "host", 0, "username", "password", false, "filterName", 10, null);
        smtpManager.removeAllBufferedEvents(); // in case this smtpManager is reused
        smtpManager.add(event);

        LogEvent[] bufferedEvents = smtpManager.removeAllBufferedEvents();
        assertThat("unexpected number of buffered events", bufferedEvents.length, is(1));
        assertThat("expected the immutable version of the event to be buffered", bufferedEvents[0].getMessage(), is(instanceOf(MementoMessage.class)));
    }

    // LOG4J2-3172: make sure existing protections are not violated
    @Test
    void testAdd_WhereImmutableLogEventWithReusableMessage() {
        LogEvent event = LogEvent.builder().setMessage(getReusableMessage("test message")).get();
        testAdd(event);
    }

    // LOG4J2-3172: make sure existing protections are not violated
    @Test
    void testAdd_WhereMutableLogEvent() {
        MutableLogEvent event = new MutableLogEvent(new DefaultContextDataFactory(), new StringBuilder("test message"), null);
        testAdd(event);
    }

    // LOG4J2-3172
    @Test
    void testAdd_WhereRingBufferLogEvent() {
        RingBufferLogEvent event = new RingBufferLogEvent();
        ContextDataFactory contextDataFactory = new DefaultContextDataFactory();
        ContextDataInjector contextDataInjector = ThreadContextDataInjector.create(contextDataFactory);
        event.setValues(null, null, null, null, null, getReusableMessage("test message"), null, null, null, 0, null, 0, null, new SystemClock(), new DummyNanoClock(), contextDataFactory, contextDataInjector);
        testAdd(event);
    }

    private ReusableMessage getReusableMessage(String text) {
        ReusableSimpleMessage message = new ReusableSimpleMessage();
        message.set(text);
        return message;
    }

}
