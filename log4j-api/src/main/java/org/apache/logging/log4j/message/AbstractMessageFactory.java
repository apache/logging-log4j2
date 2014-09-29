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

import java.io.Serializable;

/**
 * Provides an abstract superclass for MessageFactory implementations with default implementations.
 * <p>
 * This class is immutable.
 * </p>
 */
public abstract class AbstractMessageFactory implements MessageFactory, Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.message.MessageFactory#newMessage(java.lang.Object)
     */
    @Override
    public Message newMessage(final Object message) {
        return new ObjectMessage(message);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.message.MessageFactory#newMessage(java.lang.String)
     */
    @Override
    public Message newMessage(final String message) {
        return new SimpleMessage(message);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.message.MessageFactory#newMessage(java.lang.String, java.lang.Object)
     */
    @Override
    public abstract Message newMessage(String message, Object... params);
}
