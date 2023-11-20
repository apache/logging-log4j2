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
package org.apache.logging.log4j.core.config;

/**
 * This exception is thrown when an error occurs reading from, parsing, using, or initializing the Log4j 2
 * configuration. It is also thrown if an appender cannot be created based on the configuration provided.
 */
public class ConfigurationException extends RuntimeException {

    private static final long serialVersionUID = -2413951820300775294L;

    /**
     * Constructs an exception with a message.
     *
     * @param message The reason for the exception
     */
    public ConfigurationException(final String message) {
        super(message);
    }

    /**
     * Constructs an exception with a message and underlying cause.
     *
     * @param message The reason for the exception
     * @param cause The underlying cause of the exception
     */
    public ConfigurationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs an exception with a message and underlying cause.
     *
     * @param cause The underlying cause of the exception
     */
    public ConfigurationException(final Throwable cause) {
        super(cause);
    }
}
