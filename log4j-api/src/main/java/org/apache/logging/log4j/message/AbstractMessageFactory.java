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

    private static class AbstactFlowMessage extends AbstractMessage implements FlowMessage {

        private static final long serialVersionUID = 1L;
        private final Message message;
        private final String text;

        AbstactFlowMessage(final String text, final Message message) {
            this.message = message;
            this.text = text;
        }

        @Override
        public String getFormattedMessage() {
            if (message != null) {
                return text + " " + message.getFormattedMessage();
            }
            return text;
        }

        @Override
        public String getFormat() {
            if (message != null) {
                return text + ": " + message.getFormat();
            }
            return text;
        }

        @Override
        public Object[] getParameters() {
            if (message != null) {
                return message.getParameters();
            }
            return null;
        }

        @Override
        public Throwable getThrowable() {
            if (message != null) {
                return message.getThrowable();
            }
            return null;
        }

        @Override
        public Message getMessage() {
            return message;
        }

        @Override
        public String getText() {
            return text;
        }
    }

    private static final class SimpleEntryMessage extends AbstactFlowMessage implements EntryMessage {

        private static final String DEAULT_TEXT = "entry";
        private static final long serialVersionUID = 1L;

        SimpleEntryMessage(final Message message) {
            super(DEAULT_TEXT, message);
        }

    }

    private static final class SimpleExitMessage extends AbstactFlowMessage implements ExitMessage {

        private static final String DEAULT_TEXT = "exit";
        private static final long serialVersionUID = 1L;

        private final Object result;
        private final boolean isVoid;

        SimpleExitMessage(final Object result, final EntryMessage message) {
            super(DEAULT_TEXT, message.getMessage());
            this.result = result;
            isVoid = false;
        }

        SimpleExitMessage(final Object result, final Message message) {
            super(DEAULT_TEXT, message);
            this.result = result;
            isVoid = false;
        }

        SimpleExitMessage(final Message message) {
            super(DEAULT_TEXT, message);
            this.result = null;
            isVoid = true;
        }

        @Override
        public String getFormattedMessage() {
            final String formattedMessage = super.getFormattedMessage();
            if (isVoid) {
                return formattedMessage;
            }
            return formattedMessage + ": " + result;
        }
    }
    
    private static final long serialVersionUID = 1L;

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.message.MessageFactory#newEntryMessage(org.apache.logging.log4j.message.Message)
     */
    @Override
    public EntryMessage newEntryMessage(Message message) {
        return new SimpleEntryMessage(message);
    }
    
    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.message.MessageFactory#newEntryMessage(java.lang.Object, org.apache.logging.log4j.message.EntryMessage)
     */
    @Override
    public ExitMessage newExitMessage(Object object, EntryMessage message) {
        return new SimpleExitMessage(object, message);
    }
    
    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.message.MessageFactory#newEntryMessage(java.lang.Object, org.apache.logging.log4j.message.Message)
     */
    @Override
    public ExitMessage newExitMessage(Object object, Message message) {
        return new SimpleExitMessage(object, message);
    }
    
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
