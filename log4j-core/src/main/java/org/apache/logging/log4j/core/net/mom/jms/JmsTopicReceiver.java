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

package org.apache.logging.log4j.core.net.mom.jms;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Receives Topic messages that contain LogEvents. This implementation expects that all messages
 * are serialized log events.
 */
public class JmsTopicReceiver extends AbstractJmsReceiver {

    /**
     * Constructor.
     * @param tcfBindingName The TopicConnectionFactory binding name.
     * @param topicBindingName The Topic binding name.
     * @param username The userid to connect to the topic.
     * @param password The password to connect to the topic.
     */
    public JmsTopicReceiver(final String tcfBindingName, final String topicBindingName, final String username,
                            final String password) {
        try {
            final Context ctx = new InitialContext();
            TopicConnectionFactory topicConnectionFactory;
            topicConnectionFactory = (TopicConnectionFactory) lookup(ctx, tcfBindingName);
            final TopicConnection topicConnection = topicConnectionFactory.createTopicConnection(username, password);
            topicConnection.start();
            final TopicSession topicSession = topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            final Topic topic = (Topic) ctx.lookup(topicBindingName);
            final TopicSubscriber topicSubscriber = topicSession.createSubscriber(topic);
            topicSubscriber.setMessageListener(this);
        } catch (final JMSException e) {
            logger.error("Could not read JMS message.", e);
        } catch (final NamingException e) {
            logger.error("Could not read JMS message.", e);
        } catch (final RuntimeException e) {
            logger.error("Could not read JMS message.", e);
        }
    }

    /**
     * Main startup for the receiver.
     * @param args The command line arguments.
     * @throws Exception if an error occurs.
     */
    public static void main(final String[] args) throws Exception {
        if (args.length != 4) {
            usage("Wrong number of arguments.");
        }

        final String tcfBindingName = args[0];
        final String topicBindingName = args[1];
        final String username = args[2];
        final String password = args[3];

        new JmsTopicReceiver(tcfBindingName, topicBindingName, username, password);

        final Charset enc = Charset.defaultCharset();
        final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in, enc));
        // Loop until the word "exit" is typed
        System.out.println("Type \"exit\" to quit JmsTopicReceiver.");
        while (true) {
            final String line = stdin.readLine();
            if (line == null || line.equalsIgnoreCase("exit")) {
                System.out.println("Exiting. Kill the application if it does not exit "
                    + "due to daemon threads.");
                return;
            }
        }
    }

    private static void usage(final String msg) {
        System.err.println(msg);
        System.err.println("Usage: java " + JmsTopicReceiver.class.getName()
            + " TopicConnectionFactoryBindingName TopicBindingName username password");
        System.exit(1);
    }
}
