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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.layout.Rfc5424Layout;
import org.apache.logging.log4j.core.layout.SyslogLayout;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Priority;
import org.apache.logging.log4j.core.net.mock.MockSyslogServer;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.util.Strings;
import org.junit.Assert;
import org.junit.BeforeClass;

import static org.junit.Assert.*;

public abstract class SyslogAppenderTestBase {
    protected static final String line1 =
            "TestApp - Audit [Transfer@18060 Amount=\"200.00\" FromAccount=\"123457\" ToAccount=\"123456\"]" +
                    "[RequestContext@18060 ipAddress=\"192.168.0.120\" loginId=\"JohnDoe\"] Transfer Complete";
    protected LoggerContext ctx = LoggerContext.getContext();
    protected static final int DEFAULT_TIMEOUT_IN_MS = 100;
    protected static final int PORTNUM = 8199;
    protected MockSyslogServer syslogServer;
    protected SyslogAppender appender;
    protected Logger root = ctx.getLogger("SyslogAppenderTest");
    protected List<String> sentMessages = new ArrayList<>();
    protected boolean includeNewLine = true;

    @BeforeClass
    public static void setupClass() throws Exception {
        LoggerContext.getContext().reconfigure();
    }

    protected void sendAndCheckLegacyBsdMessages(final List<String> messagesToSend) throws InterruptedException {
        for (final String message : messagesToSend) {
            sendDebugLegacyBsdMessage(message);
        }
        checkTheNumberOfSentAndReceivedMessages();
        checkTheEqualityOfSentAndReceivedMessages(Level.DEBUG);
    }

    protected void sendAndCheckLegacyBsdMessage(final String message) throws InterruptedException {
        sendDebugLegacyBsdMessage(message);
        checkTheNumberOfSentAndReceivedMessages();
        checkTheEqualityOfSentAndReceivedMessages(Level.DEBUG);
    }

    protected void sendDebugLegacyBsdMessage(final String message) {
        sentMessages.add(message);
        root.debug(message);
    }

    protected void sendAndCheckStructuredMessages(final int numberOfMessages) throws InterruptedException {
        for (int i = 0; i < numberOfMessages; i++) {
            sendInfoStructuredMessage();
        }
        checkTheNumberOfSentAndReceivedMessages();
        checkTheEqualityOfSentAndReceivedMessages(Level.INFO);
    }

    protected void sendAndCheckStructuredMessage() throws InterruptedException {
        sendInfoStructuredMessage();
        checkTheNumberOfSentAndReceivedMessages();
        checkTheEqualityOfSentAndReceivedMessages(Level.INFO);
    }

    protected void sendInfoStructuredMessage() {
        ThreadContext.put("loginId", "JohnDoe");
        ThreadContext.put("ipAddress", "192.168.0.120");
        ThreadContext.put("locale", Locale.US.getDisplayName());
        final StructuredDataMessage msg = new StructuredDataMessage("Transfer@18060", "Transfer Complete", "Audit");
        msg.put("ToAccount", "123456");
        msg.put("FromAccount", "123457");
        msg.put("Amount", "200.00");
        // the msg.toString() doesn't contain the parameters of the ThreadContext, so we must use the line1 string
        final String str = msg.asString(null, null);
        sentMessages.add(str);
        root.info(MarkerManager.getMarker("EVENT"), msg);
    }

    protected void checkTheNumberOfSentAndReceivedMessages() throws InterruptedException {
        assertEquals("The number of received messages should be equal with the number of sent messages",
                sentMessages.size(), getReceivedMessages(DEFAULT_TIMEOUT_IN_MS).size());
    }

    protected void checkTheEqualityOfSentAndReceivedMessages(Level expectedLevel) throws InterruptedException {
        final List<String> receivedMessages = getReceivedMessages(DEFAULT_TIMEOUT_IN_MS);

        assertNotNull("No messages received", receivedMessages);
        for (int i = 0; i < receivedMessages.size(); i++) {
            final String receivedMessage = receivedMessages.get(i);
            final String sentMessage = sentMessages.get(i);
            final String suffix = includeNewLine ? "\n" : Strings.EMPTY;
            assertTrue("Incorrect message received: " + receivedMessage,
                    receivedMessage.endsWith(sentMessage + suffix) || receivedMessage.contains(sentMessage));
            final int expectedPriority = Priority.getPriority(getExpectedFacility(), expectedLevel);
            assertTrue("Expected facility " + expectedPriority + " in message " + receivedMessage,
                    receivedMessage.startsWith("<" + expectedPriority + ">"));
        }
    }

    protected void removeAppenders() {
        final Map<String,Appender> map = root.getAppenders();
        for (final Map.Entry<String, Appender> entry : map.entrySet()) {
            final Appender app = entry.getValue();
            root.removeAppender(app);
            app.stop();
        }
    }

    protected void initRootLogger(final Appender appender) {
        root.addAppender(appender);
        root.setAdditive(false);
        root.setLevel(Level.DEBUG);
    }

    protected List<String> getReceivedMessages(final int timeOutInMs) throws InterruptedException {
        synchronized (syslogServer) {
            syslogServer.wait(timeOutInMs);
        }
        return syslogServer.getMessageList();
    }

    protected Facility getExpectedFacility() {
        return Facility.LOCAL0;
    }

    protected void validate(SyslogAppender syslogAppender) {
        final Layout<? extends Serializable> layout = syslogAppender.getLayout();
        if (layout instanceof SyslogLayout) {
            validate((SyslogLayout) layout);
        } else if (layout instanceof Rfc5424Layout) {
            validate((Rfc5424Layout) layout);
        } else {
            Assert.fail("Unexpected layout: " + layout);
        }
    }

    protected void validate(Rfc5424Layout layout) {
        Assert.assertEquals(getExpectedFacility(), layout.getFacility());
    }

    protected void validate(SyslogLayout layout) {
        Assert.assertEquals(getExpectedFacility(), layout.getFacility());
    }
}
