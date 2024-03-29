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
package org.apache.logging.log4j.core.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.CoreProperties.AsyncProperties;
import org.apache.logging.log4j.core.impl.CoreProperties.GarbageCollectionProperties;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.di.Key;

/**
 * Log4j Constants.
 */
public final class Constants {

    private static final PropertyEnvironment ENV = PropertyEnvironment.getGlobal();

    public static final Key<Level> DEFAULT_STATUS_LEVEL_KEY = Key.builder(Level.class)
            .setName("StatusLogger")
            .setQualifierType(Named.class)
            .get();

    public static final Key<Logger> DEFAULT_STATUS_LOGGER_KEY = Key.builder(Logger.class)
            .setName("StatusLogger")
            .setQualifierType(Named.class)
            .get();

    /**
     * JNDI context name string literal.
     */
    public static final String JNDI_CONTEXT_NAME = "java:comp/env/log4j/context-name";

    /**
     * Number of milliseconds in a second.
     */
    public static final int MILLIS_IN_SECONDS = 1000;

    /**
     * Supports user request LOG4J2-898 to have the option to format a message in the background thread.
     */
    public static final boolean FORMAT_MESSAGES_IN_BACKGROUND =
            ENV.getProperty(AsyncProperties.class).formatMessagesInBackground();

    private static final GarbageCollectionProperties GC = ENV.getProperty(GarbageCollectionProperties.class);

    /**
     * Kill switch for garbage-free Layout behaviour that encodes LogEvents directly into
     * {@link org.apache.logging.log4j.core.layout.ByteBufferDestination}s without creating intermediate temporary
     * Objects.
     * <p>
     * {@code True} by default iff all loggers are asynchronous because system property
     * {@code Log4jContextSelector} is set to {@code org.apache.logging.log4j.core.async.AsyncLoggerContextSelector}.
     * Disable by setting system property "log4j2.enable.direct.encoders" to "false".
     *
     * @since 2.6
     */
    public static final boolean ENABLE_DIRECT_ENCODERS = GC.enableDirectEncoders();

    /**
     * Initial StringBuilder size used in RingBuffer LogEvents to store the contents of reusable Messages.
     * <p>
     * The default value is {@literal 128}, users can override with system property "log4j.initialReusableMsgSize".
     * </p>
     * @since 2.6
     */
    public static final int INITIAL_REUSABLE_MESSAGE_SIZE = GC.initialReusableMessageSize();

    /**
     * Maximum size of the StringBuilders used in RingBuffer LogEvents to store the contents of reusable Messages.
     * After a large message has been delivered to the appenders, the StringBuilder is trimmed to this size.
     * <p>
     * The default value is {@literal 518}, which allows the StringBuilder to resize three times from its initial size.
     * Users can override with system property "log4j.maxReusableMsgSize".
     * </p>
     * @since 2.6
     */
    public static final int MAX_REUSABLE_MESSAGE_SIZE = GC.maxReusableMessageSize();

    /**
     * Size of CharBuffers used by text encoders.
     * <p>
     * The default value is {@literal 2048}, users can override with system property "log4j.encoder.charBufferSize".
     * </p>
     * @since 2.6
     */
    public static final int ENCODER_CHAR_BUFFER_SIZE = GC.encoderCharBufferSize();

    /**
     * Default size of ByteBuffers used to encode LogEvents without allocating temporary objects.
     * <p>
     * The default value is {@literal 8192}, users can override with system property "log4j.encoder.byteBufferSize".
     * </p>
     * @see org.apache.logging.log4j.core.layout.ByteBufferDestination
     * @since 2.6
     */
    public static final int ENCODER_BYTE_BUFFER_SIZE = GC.encoderByteBufferSize();

    /**
     * Prevent class instantiation.
     */
    private Constants() {}
}
