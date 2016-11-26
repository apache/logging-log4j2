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
import org.apache.logging.log4j.util.StringBuilderFormattable;

/**
 * An interface for various Message implementations that can be logged. Messages can act as wrappers
 * around Objects so that user can have control over converting Objects to Strings when necessary without
 * requiring complicated formatters and as a way to manipulate the message based on information available
 * at runtime such as the locale of the system.
 * <p>
 * Custom Message implementations should consider implementing the {@link StringBuilderFormattable}
 * interface for more efficient processing. Garbage-free Layouts will call
 * {@link StringBuilderFormattable#formatTo(StringBuilder) formatTo(StringBuilder)} instead of
 * {@link Message#getFormattedMessage()} if the Message implements StringBuilderFormattable.
 * </p>
 * <p>
 * Note: Message objects should not be considered to be thread safe nor should they be assumed to be
 * safely reusable even on the same thread. The logging system may provide information to the Message
 * objects and the Messages might be queued for asynchronous delivery. Thus, any modifications to a
 * Message object by an application should by avoided after the Message has been passed as a parameter on
 * a Logger method.
 * </p>
 *
 * @see StringBuilderFormattable
 */
/*
 * Implementation note: this interface extends Serializable since LogEvents must be serializable.
 */
public interface Message extends Serializable {

    /**
     * Gets the Message formatted as a String. Each Message implementation determines the
     * appropriate way to format the data encapsulated in the Message. Messages that provide
     * more than one way of formatting the Message will implement MultiformatMessage.
     * <p>
     * When configured to log asynchronously, this method is called before the Message is queued, unless this
     * message implements {@link ReusableMessage} or is annotated with {@link AsynchronouslyFormattable}.
     * This gives the Message implementation class a chance to create a formatted message String with the current value
     * of any mutable objects.
     * The intention is that the Message implementation caches this formatted message and returns it on subsequent
     * calls. (See <a href="https://issues.apache.org/jira/browse/LOG4J2-763">LOG4J2-763</a>.)
     * </p>
     * <p>
     * When logging synchronously, this method will not be called for Messages that implement the
     * {@link StringBuilderFormattable} interface: instead, the
     * {@link StringBuilderFormattable#formatTo(StringBuilder) formatTo(StringBuilder)} method will be called so the
     * Message can format its contents without creating intermediate String objects.
     * </p>
     *
     * @return The message String.
     */
    String getFormattedMessage();

    /**
     * Gets the format portion of the Message.
     *
     * @return The message format. Some implementations, such as ParameterizedMessage, will use this as
     * the message "pattern". Other Messages may simply return an empty String.
     * TODO Do all messages have a format?  What syntax?  Using a Formatter object could be cleaner.
     * (RG) In SimpleMessage the format is identical to the formatted message. In ParameterizedMessage and
     * StructuredDataMessage it is not. It is up to the Message implementer to determine what this
     * method will return. A Formatter is inappropriate as this is very specific to the Message
     * implementation so it isn't clear to me how having a Formatter separate from the Message would be cleaner.
     */
    String getFormat();

    /**
     * Gets parameter values, if any.
     *
     * @return An array of parameter values or null.
     */
    Object[] getParameters();

    /**
     * Gets the throwable, if any.
     *
     * @return the throwable or null.
     */
    Throwable getThrowable();
}
