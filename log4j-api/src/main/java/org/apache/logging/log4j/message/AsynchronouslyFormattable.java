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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that signals to asynchronous logging components that messages of this type can safely be passed to
 * a background thread without calling {@link Message#getFormattedMessage()} first.
 * <p>
 * Generally, logging mutable objects asynchronously always has the risk that the object is modified between the time
 * the logger is called and the time the log message is formatted and written to disk. Strictly speaking it is the
 * responsibility of the application to ensure that mutable objects are not modified after they have been logged,
 * but this is not always possible.
 * </p><p>
 * Log4j prevents the above race condition as follows:
 * </p><ol>
 * <li>If the Message implements {@link ReusableMessage}, asynchronous logging components in the Log4j implementation
 * will copy the message content (formatted message, parameters) onto the queue rather than passing the
 * {@code Message} instance itself. This ensures that the formatted message will not change
 * when the mutable object is modified.
 * </li>
 * <li>If the Message is annotated with {@link AsynchronouslyFormattable}, it can be passed to another thread as is.</li>
 * <li>Otherwise, asynchronous logging components in the Log4j implementation will call
 * {@link Message#getFormattedMessage()} before passing the Message object to another thread.
 * This gives the Message implementation class a chance to create a formatted message String with the current value
 * of the mutable object. The intention is that the Message implementation caches this formatted message and returns
 * it on subsequent calls.
 * (See <a href="https://issues.apache.org/jira/browse/LOG4J2-763">LOG4J2-763</a>.)
 * </li>
 * </ol>
 *
 * @see Message
 * @see ReusableMessage
 * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-763">LOG4J2-763</a>
 * @since 2.8
 */
@Documented // This annotation is part of the public API of annotated elements.
@Target(ElementType.TYPE) // Only applies to types.
@Retention(RetentionPolicy.RUNTIME) // Needs to be reflectively discoverable runtime.
public @interface AsynchronouslyFormattable {}
