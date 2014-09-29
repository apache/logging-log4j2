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
 * Enables use of {@link java.util.Formatter} strings in message strings.
 * <p>
 * Creates {@link StringFormattedMessage} instances for {@link #newMessage(String, Object...)}.
 * </p>
 * <p>
 * This class is immutable.
 * </p>
 */
public final class StringFormatterMessageFactory extends AbstractMessageFactory {

    private static final long serialVersionUID = 1L;
    
    /**
     * Instance of StringFormatterMessageFactory.
     */
    public static final StringFormatterMessageFactory INSTANCE = new StringFormatterMessageFactory();

    /**
     * Creates {@link StringFormattedMessage} instances.
     *
     * @param message The message pattern.
     * @param params The parameters to the message.
     * @return The Message.
     *
     * @see MessageFactory#newMessage(String, Object...)
     */
    @Override
    public Message newMessage(final String message, final Object... params) {
        return new StringFormattedMessage(message, params);
    }
}
