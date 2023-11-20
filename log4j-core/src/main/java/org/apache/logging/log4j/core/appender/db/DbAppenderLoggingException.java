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
package org.apache.logging.log4j.core.appender.db;

import org.apache.logging.log4j.core.appender.AppenderLoggingException;

/**
 * Wraps a database exception like a JDBC SQLException. Use this class to distinguish exceptions specifically caught
 * from database layers like JDBC.
 */
public class DbAppenderLoggingException extends AppenderLoggingException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs an exception with a message.
     *
     * @param format The reason format for the exception, see {@link String#format(String, Object...)}.
     * @param args The reason arguments for the exception, see {@link String#format(String, Object...)}.
     * @since 2.12.1
     */
    public DbAppenderLoggingException(final String format, final Object... args) {
        super(format, args);
    }

    /**
     * Constructs an exception with a message and underlying cause.
     *
     * @param message The reason for the exception
     * @param cause The underlying cause of the exception
     */
    public DbAppenderLoggingException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs an exception with a message.
     *
     * @param cause The underlying cause of the exception
     * @param format The reason format for the exception, see {@link String#format(String, Object...)}.
     * @param args The reason arguments for the exception, see {@link String#format(String, Object...)}.
     * @since 2.12.1
     */
    public DbAppenderLoggingException(final Throwable cause, final String format, final Object... args) {
        super(cause, format, args);
    }
}
