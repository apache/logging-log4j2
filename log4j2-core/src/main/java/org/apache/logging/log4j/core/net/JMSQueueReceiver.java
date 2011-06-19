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
package org.apache.logging.log4j.core.net;

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
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 *
 */
public class JMSQueueReceiver extends AbstractJMSReceiver {

    static public void main(String[] args) throws Exception {
        if (args.length != 4) {
            usage("Wrong number of arguments.");
        }

        String qcfBindingName = args[0];
        String queueBindingName = args[1];
        String username = args[2];
        String password = args[3];

        new JMSQueueReceiver(qcfBindingName, queueBindingName, username, password);

        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        // Loop until the word "exit" is typed
        System.out.println("Type \"exit\" to quit JMSQueueReceiver.");
        while (true) {
            String s = stdin.readLine();
            if (s.equalsIgnoreCase("exit")) {
                System.out.println("Exiting. Kill the application if it does not exit "
                    + "due to daemon threads.");
                return;
            }
        }
    }

    public JMSQueueReceiver(String qcfBindingName, String queueBindingName, String username, String password) {

        try {
            Context ctx = new InitialContext();
            QueueConnectionFactory queueConnectionFactory;
            queueConnectionFactory = (QueueConnectionFactory) lookup(ctx,qcfBindingName);
            QueueConnection queueConnection = queueConnectionFactory.createQueueConnection(username, password);
            queueConnection.start();
            QueueSession queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = (Queue) ctx.lookup(queueBindingName);
            QueueReceiver queueReceiver = queueSession.createReceiver(queue);
            queueReceiver.setMessageListener(this);
        } catch (JMSException e) {
            logger.error("Could not read JMS message.", e);
        } catch (NamingException e) {
            logger.error("Could not read JMS message.", e);
        } catch (RuntimeException e) {
            logger.error("Could not read JMS message.", e);
        }
    }

    static void usage(String msg) {
        System.err.println(msg);
        System.err.println("Usage: java " + JMSQueueReceiver.class.getName()
            + " QueueConnectionFactoryBindingName QueueBindingName username password");
        System.exit(1);
    }
}
