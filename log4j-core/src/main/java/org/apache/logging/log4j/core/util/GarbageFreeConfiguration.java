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
package org.apache.logging.log4j.core.util;

import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.apache.logging.log4j.spi.LoggingSystem;
import org.apache.logging.log4j.spi.LoggingSystemProperties;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.PropertyResolver;

public class GarbageFreeConfiguration {
    private static final Lazy<GarbageFreeConfiguration> DEFAULT_INSTANCE =
            Lazy.relaxed(() -> new GarbageFreeConfiguration(LoggingSystem.getPropertyResolver()));

    public static GarbageFreeConfiguration getDefaultConfiguration() {
        return DEFAULT_INSTANCE.value();
    }

    private final PropertyResolver propertyResolver;

    public GarbageFreeConfiguration(final PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
    }

    /**
     * Kill switch for garbage-free Layout behaviour that encodes LogEvents directly into
     * {@link org.apache.logging.log4j.core.layout.ByteBufferDestination}s without creating intermediate temporary
     * Objects.
     * <p>
     * {@code True} by default iff all loggers are asynchronous because system property
     * {@value Log4jProperties#CONTEXT_SELECTOR_CLASS_NAME} is set to {@code org.apache.logging.log4j.core.async.AsyncLoggerContextSelector}.
     * Disable by setting system property {@value Log4jProperties#GC_ENABLE_DIRECT_ENCODERS} to "false".
     *
     * @since 3.0.0
     */
    public boolean isDirectEncodersEnabled() {
        return propertyResolver.getBoolean(Log4jProperties.GC_ENABLE_DIRECT_ENCODERS, true);
        // enable GC-free text encoding by default
        // the alternative is to enable GC-free encoding only by default only when using all-async loggers:
        //AsyncLoggerContextSelector.class.getName().equals(LoggingSystem.getPropertyResolver().getString(Log4jProperties.CONTEXT_SELECTOR_CLASS_NAME)));
    }

    /**
     * Initial StringBuilder size used in RingBuffer LogEvents to store the contents of reusable Messages.
     * <p>
     * The default value is 128, users can override with system property {@value Log4jProperties#GC_INITIAL_REUSABLE_MESSAGE_SIZE}.
     * </p>
     * @since 2.6
     */
    public int getInitialReusableMessageSize() {
        return propertyResolver.getInt(Log4jProperties.GC_INITIAL_REUSABLE_MESSAGE_SIZE).orElse(128);
    }

    /**
     * Maximum size of the StringBuilders used in RingBuffer LogEvents to store the contents of reusable Messages.
     * After a large message has been delivered to the appenders, the StringBuilder is trimmed to this size.
     * <p>
     * The default value is 518, which allows the StringBuilder to resize three times from its initial size.
     * Users can override with system property {@value LoggingSystemProperties#GC_REUSABLE_MESSAGE_MAX_SIZE}.
     * </p>
     * @since 2.6
     */
    public int getMaxReusableMessageSize() {
        return propertyResolver.getInt(LoggingSystemProperties.GC_REUSABLE_MESSAGE_MAX_SIZE).orElse((128 * 2 + 2) * 2 + 2);
    }

    /**
     * Size of CharBuffers used by text encoders.
     * <p>
     * The default value is 2048, users can override with system property {@value Log4jProperties#GC_ENCODER_CHAR_BUFFER_SIZE}.
     * </p>
     * @since 2.6
     */
    public int getEncoderCharBufferSize() {
        return propertyResolver.getInt(Log4jProperties.GC_ENCODER_CHAR_BUFFER_SIZE).orElse(2048);
    }

    /**
     * Default size of ByteBuffers used to encode LogEvents without allocating temporary objects.
     * <p>
     * The default value is 8192, users can override with system property {@value Log4jProperties#GC_ENCODER_BYTE_BUFFER_SIZE}.
     * </p>
     * @see org.apache.logging.log4j.core.layout.ByteBufferDestination
     * @since 2.6
     */
    public int getEncoderByteBufferSize() {
        return propertyResolver.getInt(Log4jProperties.GC_ENCODER_BYTE_BUFFER_SIZE).orElse(8 * 1024);
    }

    public int getLayoutStringBuilderMaxSize() {
        return propertyResolver.getInt(Log4jProperties.GC_LAYOUT_STRING_BUILDER_MAX_SIZE).orElse(2048);
    }

    /**
     * The precise clock is not enabled by default, since access to it is not garbage free.
     */
    public boolean isPreciseClockEnabled() {
        return propertyResolver.getBoolean(Log4jProperties.GC_USE_PRECISE_CLOCK);
    }
}
