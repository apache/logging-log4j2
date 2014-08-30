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

import java.io.Serializable;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LogEventListener;

/**
 * Abstract base class for receiving LogEvents over JMS. This class expects all messages to be serialized log events.
 */
public abstract class AbstractJmsReceiver extends LogEventListener implements javax.jms.MessageListener {

    /**
     * Logger to capture diagnostics.
     */
    protected Logger logger = LogManager.getLogger(this.getClass().getName());

    /**
     * Listener that receives the event.
     * @param message The received message.
     */
    @Override
    public void onMessage(final javax.jms.Message message) {
        try {
            if (message instanceof ObjectMessage) {
                final ObjectMessage objectMessage = (ObjectMessage) message;
                final Serializable object = objectMessage.getObject();
                if (object instanceof LogEvent) {
                    log((LogEvent) object);
                } else {
                    logger.warn("Received message is of type " + object.getClass().getName() + ", was expecting LogEvent.");
                }
            } else {
                logger.warn("Received message is of type " + message.getJMSType()
                    + ", was expecting ObjectMessage.");
            }
        } catch (final JMSException jmse) {
            logger.error("Exception thrown while processing incoming message.",
                jmse);
        }
    }

    /**
     * Looks up an object from the Context.
     * @param ctx The Context.
     * @param name The name of the object to locate.
     * @return The object.
     * @throws NamingException if an error occurs.
     */
    protected Object lookup(final Context ctx, final String name) throws NamingException {
        try {
            return ctx.lookup(name);
        } catch (final NameNotFoundException e) {
            logger.error("Could not find name [" + name + "].");
            throw e;
        }
    }

}
