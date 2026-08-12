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
package org.apache.logging.log4j.common.util;

/**
 * Log4j shared constants.
 */
public final class Constants {

    /**
     * Name of the system property to use to identify the LogEvent factory.
     */
    public static final String LOG4J_LOG_EVENT_FACTORY = "Log4jLogEventFactory";

    /**
     * Name of the system property to use to identify the ContextSelector Class.
     */
    public static final String LOG4J_CONTEXT_SELECTOR = "Log4jContextSelector";

    /**
     * Property name for the default status (internal log4j logging) level to use if not specified in configuration.
     *
     * @deprecated since 2.24.0 use
     * {@link org.apache.logging.log4j.status.StatusLogger#DEFAULT_STATUS_LISTENER_LEVEL} instead.
     */
    @Deprecated
    public static final String LOG4J_DEFAULT_STATUS_LEVEL = "Log4jDefaultStatusLevel";

    /**
     * JNDI context name string literal.
     */
    public static final String JNDI_CONTEXT_NAME = "java:comp/env/log4j/context-name";

    /**
     * Control which script languages are allowed, if any.
     */
    public static final String SCRIPT_LANGUAGES = "log4j2.Script.enableLanguages";

    /**
     * Number of milliseconds in a second.
     */
    public static final int MILLIS_IN_SECONDS = 1000;

    /**
     * Default initial StringBuilder size used in RingBuffer LogEvents to store the contents of reusable Messages.
     */
    public static final int INITIAL_REUSABLE_MESSAGE_SIZE = 128;

    /**
     * Default maximum size of the StringBuilders used in RingBuffer LogEvents to store the contents of reusable
     * Messages.
     */
    public static final int MAX_REUSABLE_MESSAGE_SIZE = (128 * 2 + 2) * 2 + 2;

    /**
     * Default size of CharBuffers used by text encoders.
     */
    public static final int ENCODER_CHAR_BUFFER_SIZE = 2048;

    /**
     * Default size of ByteBuffers used to encode LogEvents without allocating temporary objects.
     */
    public static final int ENCODER_BYTE_BUFFER_SIZE = 8 * 1024;

    private Constants() {}
}
