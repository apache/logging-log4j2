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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.net.mock.MockSyslogServer;
import org.apache.logging.log4j.core.net.mock.MockSyslogServerFactory;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.junit.BeforeClass;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SyslogAppenderTestBase {
    protected static final String line1 =
            "TestApp - Audit [Transfer@18060 Amount=\"200.00\" FromAccount=\"123457\" ToAccount=\"123456\"]" +
                    "[RequestContext@18060 ipAddress=\"192.168.0.120\" loginId=\"JohnDoe\"] Transfer Complete";
    protected LoggerContext ctx = (LoggerContext) LogManager.getContext();
    protected static final int DEFAULT_TIMEOUT_IN_MS = 100;
    protected static final String PORT = "8199";
    protected static final int PORTNUM = Integer.parseInt(PORT);
    protected MockSyslogServer syslogServer;
    protected SyslogAppender appender;
    protected Logger root = ctx.getLogger("SyslogAppenderTest");
    protected List<String> sentMessages = new ArrayList<String>();
    protected String includeNewLine = "true";

    @BeforeClass
    public static void setupClass() throws Exception {
        ((LoggerContext) LogManager.getContext()).reconfigure();
    }

    protected void sendAndCheckLegacyBSDMessages(List<String> messagesToSend) throws InterruptedException {
        for (String message : messagesToSend)
            sendLegacyBSDMessage(message);
        checkTheNumberOfSentAndReceivedMessages();
        checkTheEqualityOfSentAndReceivedMessages();
    }

    protected void sendAndCheckLegacyBSDMessage(String message) throws InterruptedException {
        sendLegacyBSDMessage(message);
        checkTheNumberOfSentAndReceivedMessages();
        checkTheEqualityOfSentAndReceivedMessages();
    }

    protected void sendLegacyBSDMessage(String message) {
        sentMessages.add(message);
        root.debug(message);
    }

    protected void sendAndCheckStructuredMessages(int numberOfMessages) throws InterruptedException {
        for (int i = 0; i < numberOfMessages; i++)
            sendStructuredMessage();
        checkTheNumberOfSentAndReceivedMessages();
        checkTheEqualityOfSentAndReceivedMessages();
    }

    protected void sendAndCheckStructuredMessage() throws InterruptedException {
        sendStructuredMessage();
        checkTheNumberOfSentAndReceivedMessages();
        checkTheEqualityOfSentAndReceivedMessages();
    }

    protected void sendStructuredMessage() {
        ThreadContext.put("loginId", "JohnDoe");
        ThreadContext.put("ipAddress", "192.168.0.120");
        ThreadContext.put("locale", Locale.US.getDisplayName());
        final StructuredDataMessage msg = new StructuredDataMessage("Transfer@18060", "Transfer Complete", "Audit");
        msg.put("ToAccount", "123456");
        msg.put("FromAccount", "123457");
        msg.put("Amount", "200.00");
        // the msg.toString() doesn't contain the parameters of the ThreadContext, so we must use the line1 string
        sentMessages.add(line1);
        root.info(MarkerManager.getMarker("EVENT"), msg);
    }

    protected void checkTheNumberOfSentAndReceivedMessages() throws InterruptedException {
        assertEquals("The number of received messages should be equal with the number of sent messages",
                sentMessages.size(), getReceivedMessages(DEFAULT_TIMEOUT_IN_MS).size());
    }

    protected void checkTheEqualityOfSentAndReceivedMessages() throws InterruptedException {
        List<String> receivedMessages = getReceivedMessages(DEFAULT_TIMEOUT_IN_MS);

        assertNotNull("No messages received", receivedMessages);
        for (int i = 0; i < receivedMessages.size(); i++) {
            String receivedMessage = receivedMessages.get(i);
            String sentMessage = sentMessages.get(i);
            String suffix =  "true".equalsIgnoreCase(includeNewLine) ? "\n" : "";
            assertTrue("Incorrect message received: " + receivedMessage, receivedMessage.endsWith(sentMessage + suffix));
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

    protected void initRootLogger(Appender appender) {
        root.addAppender(appender);
        root.setAdditive(false);
        root.setLevel(Level.DEBUG);
    }

    protected List<String> getReceivedMessages(int timeOutInMs) throws InterruptedException {
        synchronized (syslogServer) {
            syslogServer.wait(timeOutInMs);
        }
        return syslogServer.getMessageList();
    }
}
