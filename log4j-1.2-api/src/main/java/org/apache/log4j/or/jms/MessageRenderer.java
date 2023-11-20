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
package org.apache.log4j.or.jms;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import org.apache.log4j.or.ObjectRenderer;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Log4j 1.x JMS Message Renderer
 */
public class MessageRenderer implements ObjectRenderer {
    private static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * Render a {@link javax.jms.Message}.
     */
    @Override
    public String doRender(final Object obj) {
        if (obj instanceof Message) {
            final StringBuilder sb = new StringBuilder();
            final Message message = (Message) obj;
            try {
                sb.append("DeliveryMode=");
                switch (message.getJMSDeliveryMode()) {
                    case DeliveryMode.NON_PERSISTENT:
                        sb.append("NON_PERSISTENT");
                        break;
                    case DeliveryMode.PERSISTENT:
                        sb.append("PERSISTENT");
                        break;
                    default:
                        sb.append("UNKNOWN");
                }
                sb.append(", CorrelationID=");
                sb.append(message.getJMSCorrelationID());

                sb.append(", Destination=");
                sb.append(message.getJMSDestination());

                sb.append(", Expiration=");
                sb.append(message.getJMSExpiration());

                sb.append(", MessageID=");
                sb.append(message.getJMSMessageID());

                sb.append(", Priority=");
                sb.append(message.getJMSPriority());

                sb.append(", Redelivered=");
                sb.append(message.getJMSRedelivered());

                sb.append(", ReplyTo=");
                sb.append(message.getJMSReplyTo());

                sb.append(", Timestamp=");
                sb.append(message.getJMSTimestamp());

                sb.append(", Type=");
                sb.append(message.getJMSType());

            } catch (JMSException e) {
                LOGGER.error("Could not parse Message.", e);
            }
            return sb.toString();
        }
        return obj.toString();
    }
}
