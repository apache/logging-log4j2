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
package org.apache.logging.log4j.message;

/**
 * Creates {@link org.apache.logging.log4j.message.MessageFormatMessage} instances for
 * {@link #newMessage(String, Object...)}.
 */
public class MessageFormatMessageFactory extends AbstractMessageFactory {

    private static final long serialVersionUID = 1L;

    /**
     * Creates {@link org.apache.logging.log4j.message.StringFormattedMessage} instances.
     * @param message The message pattern.
     * @param params Parameters to the message.
     * @return The Message.
     *
     * @see org.apache.logging.log4j.message.MessageFactory#newMessage(String, Object...)
     */
    @Override
    public Message newMessage(final String message, final Object... params) {
        return new MessageFormatMessage(message, params);
    }
}
