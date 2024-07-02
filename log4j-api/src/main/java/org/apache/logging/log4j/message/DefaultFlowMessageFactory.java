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
package org.apache.logging.log4j.message;

import java.io.Serializable;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.apache.logging.log4j.util.StringBuilders;
import org.apache.logging.log4j.util.Strings;

/**
 * Default factory for flow messages.
 *
 * @since 2.6
 */
public class DefaultFlowMessageFactory implements FlowMessageFactory, Serializable {

    private static final String EXIT_DEFAULT_PREFIX = "Exit";
    private static final String ENTRY_DEFAULT_PREFIX = "Enter";
    private static final long serialVersionUID = 8578655591131397576L;

    public static final FlowMessageFactory INSTANCE = new DefaultFlowMessageFactory();

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
        this.entryText = entryText;
        this.exitText = exitText;
    }

    private static class AbstractFlowMessage implements FlowMessage, StringBuilderFormattable {

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
                return text + " " + message.getFormat();
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

        @Override
        public void formatTo(final StringBuilder buffer) {
            buffer.append(text);
            if (message != null) {
                buffer.append(" ");
                StringBuilders.appendValue(buffer, message);
            }
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
            this(exitText, message.getMessage());
        }

        SimpleExitMessage(final String exitText, final Message message) {
            super(exitText, message);
            this.result = null;
            isVoid = true;
        }

        SimpleExitMessage(final String exitText, final Object result, final EntryMessage message) {
            this(exitText, result, message.getMessage());
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

    @Override
    public EntryMessage newEntryMessage(final String format, final Object... params) {
        final boolean hasFormat = Strings.isNotEmpty(format);
        final Message message;
        if (params == null || params.length == 0) {
            message = hasFormat ? ParameterizedMessageFactory.INSTANCE.newMessage(format) : null;
        } else if (hasFormat) {
            message = ParameterizedMessageFactory.INSTANCE.newMessage(format, params);
        } else {
            final StringBuilder sb = new StringBuilder("params(");
            for (int i = 0; i < params.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("{}");
            }
            sb.append(")");
            message = ParameterizedMessageFactory.INSTANCE.newMessage(sb.toString(), params);
        }
        return newEntryMessage(message);
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
        if (message instanceof ReusableMessage) {
            return ((ReusableMessage) message).memento();
        }
        return message;
    }

    @Override
    public ExitMessage newExitMessage(final String format, final Object result) {
        final boolean hasFormat = Strings.isNotEmpty(format);
        final Message message;
        if (result == null) {
            message = hasFormat ? ParameterizedMessageFactory.INSTANCE.newMessage(format) : null;
        } else {
            message = ParameterizedMessageFactory.INSTANCE.newMessage(hasFormat ? format : "with({})", result);
        }
        return newExitMessage(message);
    }

    @Override
    public ExitMessage newExitMessage(Message message) {
        return new SimpleExitMessage(exitText, message);
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
