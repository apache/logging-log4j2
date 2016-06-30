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
 * Default factory for flow messages.
 *
 * @since 2.6
 */
public class DefaultFlowMessageFactory implements FlowMessageFactory, Serializable {

    private static final String EXIT_DEFAULT_PREFIX = "Exit";
    private static final String ENTRY_DEFAULT_PREFIX = "Enter";
    private static final long serialVersionUID = 8578655591131397576L;

    private final String entryText;
    private final String exitText;

    /**
     * Constructs a message factory with {@code "Enter"} and {@code "Exit"} as the default flow strings.
     */
    public DefaultFlowMessageFactory() {
        this(ENTRY_DEFAULT_PREFIX, EXIT_DEFAULT_PREFIX);
    }

    /**
     * Constructs a message factory with the given entry and exit strings.
     * @param entryText the text to use for trace entry, like {@code "Enter"}.
     * @param exitText the text to use for trace exit, like {@code "Exit"}.
     */
    public DefaultFlowMessageFactory(final String entryText, final String exitText) {
        super();
        this.entryText = entryText;
        this.exitText = exitText;
    }

    private static class AbstractFlowMessage implements FlowMessage {

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

        SimpleExitMessage(final String exitText, final EntryMessage message) {
            super(exitText, message.getMessage());
            this.result = null;
            isVoid = true;
        }

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

    /**
     * Gets the entry text.
     * @return the entry text.
     */
    public String getEntryText() {
        return entryText;
    }

    /**
     * Gets the exit text.
     * @return the exit text.
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
        return new SimpleEntryMessage(entryText, makeImmutable(message));
    }

    private Message makeImmutable(final Message message) {
        if (!(message instanceof ReusableMessage)) {
            return message;
        }
        return new SimpleMessage(message.getFormattedMessage());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.message.FlowMessageFactory#newExitMessage(org.apache.logging.log4j.message.EntryMessage)
     */
    @Override
    public ExitMessage newExitMessage(final EntryMessage message) {
        return new SimpleExitMessage(exitText, message);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.message.FlowMessageFactory#newExitMessage(java.lang.Object, org.apache.logging.log4j.message.EntryMessage)
     */
    @Override
    public ExitMessage newExitMessage(final Object result, final EntryMessage message) {
        return new SimpleExitMessage(exitText, result, message);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.message.FlowMessageFactory#newExitMessage(java.lang.Object, org.apache.logging.log4j.message.Message)
     */
    @Override
    public ExitMessage newExitMessage(final Object result, final Message message) {
        return new SimpleExitMessage(exitText, result, message);
    }
}
