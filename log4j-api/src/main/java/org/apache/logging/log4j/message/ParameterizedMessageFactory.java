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
 * Enables use of <code>{}</code> parameter markers in message strings.
 * <p>
 * Creates {@link ParameterizedMessage} instances for {@link #newMessage(String, Object...)}.
 * </p>
 * <p>
 * This class is immutable.
 * </p>
 */
public final class ParameterizedMessageFactory extends AbstractMessageFactory {

    /**
     * Constructs a message factory with default flow strings.
     */
    public ParameterizedMessageFactory() {
        super();
    }

    /**
     * Constructs a message factory with the given entry and exit strings.
     * @param entryText the text to use for trace entry, like {@code "entry"} or {@code "Enter"}.
     * @param exitText the text to use for trace exit, like {@code "exit"} or {@code "Exit"}.
     * @since 2.6
     */
    public ParameterizedMessageFactory(final String entryText, final String exitText) {
        super(entryText, exitText);
    }

    /**
     * Instance of StringFormatterMessageFactory.
     */
    public static final ParameterizedMessageFactory INSTANCE = new ParameterizedMessageFactory();

    private static final long serialVersionUID = 1L;

    /**
     * Creates {@link ParameterizedMessage} instances.
     * 
     * @param message The message pattern.
     * @param params The message parameters.
     * @return The Message.
     *
     * @see MessageFactory#newMessage(String, Object...)
     */
    @Override
    public Message newMessage(final String message, final Object... params) {
        return new ParameterizedMessage(message, params);
    }
}
