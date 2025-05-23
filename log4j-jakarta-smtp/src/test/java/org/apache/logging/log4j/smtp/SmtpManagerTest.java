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
package org.apache.logging.log4j.smtp;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.SmtpAppender;
import org.apache.logging.log4j.core.async.RingBufferLogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.net.MailManager;
import org.apache.logging.log4j.core.util.ClockFactory;
import org.apache.logging.log4j.core.util.DummyNanoClock;
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.message.ReusableSimpleMessage;
import org.junit.jupiter.api.Test;

class SmtpManagerTest {

    private void testAdd(final LogEvent event) {
        final SmtpAppender appender = SmtpAppender.newBuilder()
                .setName("smtp")
                .setTo("to")
                .setCc("cc")
                .setBcc("bcc")
                .setFrom("from")
                .setReplyTo("replyTo")
                .setSubject("subject")
                .setSmtpProtocol("smtp")
                .setSmtpHost("host")
                .setSmtpPort(0)
                .setSmtpUsername("username")
                .setSmtpPassword("password")
                .setSmtpDebug(false)
                .setFilter(null)
                .setBufferSize(10)
                .build();
        final MailManager mailManager = appender.getManager();
        assertThat(mailManager).isInstanceOf(SmtpManager.class);
        final SmtpManager smtpManager = (SmtpManager) mailManager;
        smtpManager.removeAllBufferedEvents(); // in case this smtpManager is reused
        smtpManager.add(event);

        final LogEvent[] bufferedEvents = smtpManager.removeAllBufferedEvents();
        assertThat(bufferedEvents).as("Buffered events").hasSize(1);
        assertThat(bufferedEvents[0].getMessage()).as("Immutable message").isNotInstanceOf(ReusableMessage.class);
    }

    // LOG4J2-3172: make sure existing protections are not violated
    @Test
    void testAdd_WhereLog4jLogEventWithReusableMessage() {
        final LogEvent event = new Log4jLogEvent.Builder()
                .setMessage(getReusableMessage("test message"))
                .build();
        testAdd(event);
    }

    // LOG4J2-3172: make sure existing protections are not violated
    @Test
    void testAdd_WhereMutableLogEvent() {
        final MutableLogEvent event = new MutableLogEvent(new StringBuilder("test message"), null);
        testAdd(event);
    }

    // LOG4J2-3172
    @Test
    void testAdd_WhereRingBufferLogEvent() {
        final RingBufferLogEvent event = new RingBufferLogEvent();
        event.setValues(
                null,
                null,
                null,
                null,
                null,
                getReusableMessage("test message"),
                null,
                null,
                null,
                0,
                null,
                0,
                null,
                ClockFactory.getClock(),
                new DummyNanoClock());
        testAdd(event);
    }

    private ReusableMessage getReusableMessage(final String text) {
        final ReusableSimpleMessage message = new ReusableSimpleMessage();
        message.set(text);
        return message;
    }
}
