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
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertyKey;

/**
 * Log4j Constants.
 */
public final class Constants {

    private static final String JNDI_MANAGER_CLASS = "org.apache.logging.log4j.jndi.JndiManager";

    /**
     * Check to determine if the JNDI feature is available.
     * @param key The feature to check.
     * @return true if the feature is available.
     */
    private static boolean isJndiEnabled(final PropertyKey key) {
        return PropertiesUtil.getProperties().getBooleanProperty(key, false)
                && isClassAvailable(JNDI_MANAGER_CLASS);
    }

    public static boolean JNDI_CONTEXT_SELECTOR_ENABLED = isJndiEnabled(Log4jPropertyKey.JNDI_CONTEXT_SELECTOR);

    public static boolean JNDI_JMS_ENABLED = isJndiEnabled(Log4jPropertyKey.JNDI_ENABLE_JMS);

    public static boolean JNDI_LOOKUP_ENABLED = isJndiEnabled(Log4jPropertyKey.JNDI_ENABLE_LOOKUP);

    public static boolean JNDI_JDBC_ENABLED = isJndiEnabled(Log4jPropertyKey.JNDI_ENABLE_JDBC);

    public static final Key<Level> DEFAULT_STATUS_LEVEL_KEY = new @Named("StatusLogger") Key<>() {};

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
    public static final boolean FORMAT_MESSAGES_IN_BACKGROUND = PropertiesUtil.getProperties().getBooleanProperty(
            Log4jPropertyKey.ASYNC_LOGGER_FORMAT_MESSAGES_IN_BACKGROUND, false);

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
    public static final boolean ENABLE_DIRECT_ENCODERS = PropertiesUtil.getProperties().getBooleanProperty(
            Log4jPropertyKey.GC_ENABLE_DIRECT_ENCODERS, true); // enable GC-free text encoding by default
            // the alternative is to enable GC-free encoding only by default only when using all-async loggers:
            //AsyncLoggerContextSelector.class.getName().equals(PropertiesUtil.getProperties().getStringProperty(LOG4J_CONTEXT_SELECTOR)));

    /**
     * Initial StringBuilder size used in RingBuffer LogEvents to store the contents of reusable Messages.
     * <p>
     * The default value is {@value}, users can override with system property "log4j.initialReusableMsgSize".
     * </p>
     * @since 2.6
     */
    public static final int INITIAL_REUSABLE_MESSAGE_SIZE = size(Log4jPropertyKey.GC_INITIAL_REUSABLE_MESSAGE_SIZE, 128);

    /**
     * Maximum size of the StringBuilders used in RingBuffer LogEvents to store the contents of reusable Messages.
     * After a large message has been delivered to the appenders, the StringBuilder is trimmed to this size.
     * <p>
     * The default value is {@value}, which allows the StringBuilder to resize three times from its initial size.
     * Users can override with system property "log4j.maxReusableMsgSize".
     * </p>
     * @since 2.6
     */
    public static final int MAX_REUSABLE_MESSAGE_SIZE = size(Log4jPropertyKey.GC_REUSABLE_MESSAGE_MAX_SIZE, (128 * 2 + 2) * 2 + 2);

    /**
     * Size of CharBuffers used by text encoders.
     * <p>
     * The default value is {@value}, users can override with system property "log4j.encoder.charBufferSize".
     * </p>
     * @since 2.6
     */
    public static final int ENCODER_CHAR_BUFFER_SIZE = size(Log4jPropertyKey.GC_ENCODER_CHAR_BUFFER_SIZE, 2048);

    /**
     * Default size of ByteBuffers used to encode LogEvents without allocating temporary objects.
     * <p>
     * The default value is {@value}, users can override with system property "log4j.encoder.byteBufferSize".
     * </p>
     * @see org.apache.logging.log4j.core.layout.ByteBufferDestination
     * @since 2.6
     */
    public static final int ENCODER_BYTE_BUFFER_SIZE = size(Log4jPropertyKey.GC_ENCODER_BYTE_BUFFER_SIZE, 8 * 1024);


    private static int size(final PropertyKey property, final int defaultValue) {
        return PropertiesUtil.getProperties().getIntegerProperty(property, defaultValue);
    }

    /**
     * Determines if a named Class can be loaded or not.
     *
     * @param className The class name.
     * @return {@code true} if the class could be found or {@code false} otherwise.
     */
    private static boolean isClassAvailable(final String className) {
        try {
            return LoaderUtil.loadClass(className) != null;
        } catch (final Throwable e) {
            return false;
        }
    }

    /**
     * Prevent class instantiation.
     */
    private Constants() {
    }
}
