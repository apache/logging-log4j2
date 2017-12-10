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
 * Creates {@link FormattedMessage} instances for {@link MessageFactory2} methods (and {@link MessageFactory} by
 * extension.)
 * 
 * <h4>Note to implementors</h4>
 * <p>
 * This class implements all {@link MessageFactory2} methods.
 * </p>
 */
public class MessageFormatMessageFactory extends AbstractMessageFactory {
    private static final long serialVersionUID = 3584821740584192453L;

    /**
     * Constructs a message factory with default flow strings.
     */
    public MessageFormatMessageFactory() {
        super();
    }

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

    /**
     * Creates {@link org.apache.logging.log4j.message.StringFormattedMessage} instances, when the location
     * of the log statement might be known at compile time.
     *
     * @param source the location of the log statement, or null
     * @param message The message pattern.
     * @param params Parameters to the message.
     * @return The Message.
     *
     * @see org.apache.logging.log4j.message.MessageFactory#newMessage(String, Object...)
     */
    @Override
    public Message newMessage(SourceLocation source, String message, Object... params) {
        return new MessageFormatMessage(source, message, params);
    }

}
