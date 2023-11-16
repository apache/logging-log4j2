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

import java.nio.charset.StandardCharsets;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.logging.log4j.core.util.Builder;

/**
 * Builder for {@link MimeMessage} instances.
 */
public class MimeMessageBuilder implements Builder<MimeMessage> {
    private final MimeMessage message;

    public MimeMessageBuilder(final Session session) {
        message = new MimeMessage(session);
    }

    public MimeMessageBuilder setFrom(final String from) throws MessagingException {
        final InternetAddress address = parseAddress(from);

        if (null != address) {
            message.setFrom(address);
        } else {
            try {
                message.setFrom();
            } catch (final Exception ex) {
                message.setFrom((InternetAddress) null);
            }
        }
        return this;
    }

    public MimeMessageBuilder setReplyTo(final String replyTo) throws MessagingException {
        final InternetAddress[] addresses = parseAddresses(replyTo);

        if (null != addresses) {
            message.setReplyTo(addresses);
        }
        return this;
    }

    public MimeMessageBuilder setRecipients(final Message.RecipientType recipientType, final String recipients)
            throws MessagingException {
        final InternetAddress[] addresses = parseAddresses(recipients);

        if (null != addresses) {
            message.setRecipients(recipientType, addresses);
        }
        return this;
    }

    public MimeMessageBuilder setSubject(final String subject) throws MessagingException {
        if (subject != null) {
            message.setSubject(subject, StandardCharsets.UTF_8.name());
        }
        return this;
    }

    /**
     * @deprecated Use {@link #build()}.
     */
    @Deprecated
    public MimeMessage getMimeMessage() {
        return build();
    }

    @Override
    public MimeMessage build() {
        return message;
    }

    private static InternetAddress parseAddress(final String address) throws AddressException {
        return address == null ? null : new InternetAddress(address);
    }

    private static InternetAddress[] parseAddresses(final String addresses) throws AddressException {
        return addresses == null ? null : InternetAddress.parse(addresses, true);
    }
}
