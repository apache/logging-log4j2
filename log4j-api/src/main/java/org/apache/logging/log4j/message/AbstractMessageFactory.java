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

    /**
     * Constructs a message factory with {@code "entry"} and {@code "exit"} as the default flow strings.
     */
    public AbstractMessageFactory() {
        this("entry", "exit");
    }

    /**
     * Constructs a message factory with the given entry and exit strings.
     * @param entryText the text to use for trace entry, like {@code "entry"} or {@code "Enter"}.
     * @param exitText the text to use for trace exit, like {@code "exit"} or {@code "Exit"}.
     * @since 2.6
     */
    public AbstractMessageFactory(final String entryText, final String exitText) {
        super();
        this.entryText = entryText;
        this.exitText = exitText;
    }

    private final String entryText;
    private final String exitText;
    
    private static class AbstractFlowMessage extends AbstractMessage implements FlowMessage {

        private static final long serialVersionUID = 1L;
        private final Message message;
        private final String text;

        AbstractFlowMessage(final String text, final Message message) {
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

    private static final class SimpleEntryMessage extends AbstractFlowMessage implements EntryMessage {

        private static final long serialVersionUID = 1L;

        SimpleEntryMessage(final String entryText, final Message message) {
            super(entryText, message);
        }

    }

    private static final class SimpleExitMessage extends AbstractFlowMessage implements ExitMessage {

        private static final long serialVersionUID = 1L;

        private final Object result;
        private final boolean isVoid;

        SimpleExitMessage(final String exitText, final Object result, final EntryMessage message) {
            super(exitText, message.getMessage());
            this.result = result;
            isVoid = false;
        }

        SimpleExitMessage(final String exitText, final Object result, final Message message) {
            super(exitText, message);
            this.result = result;
            isVoid = false;
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

    /**
     * Gets the entry text.
     * @return the entry text.
     * @since 2.6
     */
    public String getEntryText() {
        return entryText;
    }

    /**
     * Gets the exit text.
     * @return the exit text.
     * @since 2.6
     */
    public String getExitText() {
        return exitText;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.message.MessageFactory#newEntryMessage(org.apache.logging.log4j.message.Message)
     */
    @Override
    public EntryMessage newEntryMessage(final Message message) {
        return new SimpleEntryMessage(entryText, message);
    }
    
    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.message.MessageFactory#newEntryMessage(java.lang.Object, org.apache.logging.log4j.message.EntryMessage)
     */
    @Override
    public ExitMessage newExitMessage(final Object object, final EntryMessage message) {
        return new SimpleExitMessage(exitText, object, message);
    }
    
    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.message.MessageFactory#newEntryMessage(java.lang.Object, org.apache.logging.log4j.message.Message)
     */
    @Override
    public ExitMessage newExitMessage(final Object object, final Message message) {
        return new SimpleExitMessage(exitText, object, message);
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
