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
package org.apache.logging.log4j.core.net;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import javax.mail.MessagingException;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.SmtpAppender;
import org.apache.logging.log4j.core.async.RingBufferLogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.util.ClockFactory;
import org.apache.logging.log4j.core.util.DummyNanoClock;
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.message.ReusableSimpleMessage;
import org.junit.jupiter.api.Test;

class SmtpManagerTest {

    @Test
    void testCreateManagerName() {
        final String managerName = SmtpManager.createManagerName(
                "to",
                "cc",
                null,
                "from",
                null,
                "LOG4J2-3107",
                "proto",
                "smtp.log4j.com",
                4711,
                "username",
                false,
                "filter");
        assertEquals("SMTP:to:cc::from::LOG4J2-3107:proto:smtp.log4j.com:4711:username::filter", managerName);
    }

    @Test
    void testCreateManagerNameDistinguishesHeaders() {
        assertThat(managerNameWithHeaders(Property.createProperty("X-Tag", "a")))
                .isNotEqualTo(managerNameWithHeaders(Property.createProperty("X-Tag", "b")));
    }

    private static String managerNameWithHeaders(final Property... headers) {
        return SmtpManager.createManagerName(
                "to",
                "cc",
                null,
                "from",
                null,
                "LOG4J2-3107",
                "proto",
                "smtp.log4j.com",
                4711,
                "username",
                false,
                "filter",
                headers);
    }

    @Test
    void testEncodeHeaderValueLeavesPlainAsciiAlone() throws MessagingException {
        assertEquals("plain value", SmtpManager.encodeHeaderValue("X-Test", "plain value"));
    }

    @Test
    void testEncodeHeaderValueNeutralizesControlCharacters() throws MessagingException {
        assertEquals("safe  X-Evil: injected", SmtpManager.encodeHeaderValue("X-Test", "safe\r\nX-Evil: injected"));
    }

    @Test
    void testEncodeHeaderValueIsAsciiOnly() throws MessagingException {
        final String encoded = SmtpManager.encodeHeaderValue("X-Test", "Jos\u00e9 \u20b9500");
        assertThat(encoded.chars().allMatch(c -> c < 128)).isTrue();
    }

    @Test
    void testEncodeHeaderValueRespectsLineLengthLimit() throws MessagingException {
        final char[] chars = new char[5_000];
        Arrays.fill(chars, 'x');
        assertLineLengthLimit(SmtpManager.encodeHeaderValue("X-Test", new String(chars)));
        Arrays.fill(chars, '\u00e9');
        assertLineLengthLimit(SmtpManager.encodeHeaderValue("X-Test", new String(chars)));
    }

    private static void assertLineLengthLimit(final String encoded) {
        int used = "X-Test".length() + 2;
        for (final String line : encoded.split("\r\n", -1)) {
            assertThat(used + line.length()).isLessThanOrEqualTo(998);
            used = 0;
        }
    }

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
