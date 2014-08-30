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
package org.apache.logging.log4j.mom.jms.receiver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Receives Log Events over a JMS Queue. This implementation expects that all messages will
 * contain a serialized LogEvent.
 */
public class JmsQueueReceiver extends AbstractJmsReceiver {

    /**
     * Constructor.
     * @param qcfBindingName The QueueConnectionFactory binding name.
     * @param queueBindingName The Queue binding name.
     * @param username The userid to connect to the queue.
     * @param password The password to connect to the queue.
     */
    public JmsQueueReceiver(final String qcfBindingName, final String queueBindingName, final String username,
                            final String password) {

        try {
            final Context ctx = new InitialContext();
            QueueConnectionFactory queueConnectionFactory;
            queueConnectionFactory = (QueueConnectionFactory) lookup(ctx, qcfBindingName);
            final QueueConnection queueConnection = queueConnectionFactory.createQueueConnection(username, password);
            queueConnection.start();
            final QueueSession queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            final Queue queue = (Queue) ctx.lookup(queueBindingName);
            final QueueReceiver queueReceiver = queueSession.createReceiver(queue);
            queueReceiver.setMessageListener(this);
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

        final String qcfBindingName = args[0];
        final String queueBindingName = args[1];
        final String username = args[2];
        final String password = args[3];

        new JmsQueueReceiver(qcfBindingName, queueBindingName, username, password);

        final Charset enc = Charset.defaultCharset();
        final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in, enc));
        // Loop until the word "exit" is typed
        System.out.println("Type \"exit\" to quit JmsQueueReceiver.");
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
        System.err.println("Usage: java " + JmsQueueReceiver.class.getName()
            + " QueueConnectionFactoryBindingName QueueBindingName username password");
        System.exit(1);
    }
}
