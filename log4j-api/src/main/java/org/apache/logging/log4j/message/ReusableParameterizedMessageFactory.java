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
 * Reuses a ThreadLocal {@link ReusableParameterizedMessage} instance for {@link #newMessage(String, Object...)}.
 * </p>
 * <p>
 * This class is immutable.
 * </p>
 */
public final class ReusableParameterizedMessageFactory extends AbstractMessageFactory {

    /**
     * Instance of ReusableParameterizedMessageFactory.
     */
    public static final ReusableParameterizedMessageFactory INSTANCE = new ReusableParameterizedMessageFactory();

    private static final long serialVersionUID = -8970940216592525651L;
    private static ThreadLocal<ReusableParameterizedMessage> threadLocalMessage = new ThreadLocal<>();

    /**
     * Constructs a message factory.
     */
    public ReusableParameterizedMessageFactory() {
        super();
    }

    /**
     * Creates {@link ReusableParameterizedMessage} instances.
     *
     * @param message The message pattern.
     * @param params The message parameters.
     * @return The Message.
     *
     * @see MessageFactory#newMessage(String, Object...)
     */
    @Override
    public Message newMessage(final String message, final Object... params) {
        ReusableParameterizedMessage result = threadLocalMessage.get();
        if (result == null) {
            result = new ReusableParameterizedMessage();
            threadLocalMessage.set(result);
        } else {
            result.set(message, params);
        }
        return result;
    }
}
