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
package org.apache.log4j.spi;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;


/**
 * Appenders may delegate their error handling to
 * <code>ErrorHandlers</code>.
 * <p>
 * Error handling is a particularly tedious to get right because by
 * definition errors are hard to predict and to reproduce.
 * </p>
 * <p>
 * Please take the time to contact the author in case you discover
 * that errors are not properly handled. You are most welcome to
 * suggest new error handling policies or criticize existing policies.
 * </p>
 */
public interface ErrorHandler {

    /**
     * Add a reference to a logger to which the failing appender might
     * be attached to. The failing appender will be searched and
     * replaced only in the loggers you add through this method.
     *
     * @param logger One of the loggers that will be searched for the failing
     *               appender in view of replacement.
     * @since 1.2
     */
    void setLogger(Logger logger);


    /**
     * Equivalent to the {@link #error(String, Exception, int,
     * LoggingEvent)} with the the event parameter set to
     * <code>null</code>.
     *
     * @param message   The message associated with the error.
     * @param e         The Exception that was thrown when the error occurred.
     * @param errorCode The error code associated with the error.
     */
    void error(String message, Exception e, int errorCode);

    /**
     * This method is normally used to just print the error message
     * passed as a parameter.
     *
     * @param message   The message associated with the error.
     */
    void error(String message);

    /**
     * This method is invoked to handle the error.
     *
     * @param message   The message associated with the error.
     * @param e         The Exception that was thrown when the error occurred.
     * @param errorCode The error code associated with the error.
     * @param event     The logging event that the failing appender is asked
     *                  to log.
     * @since 1.2
     */
    void error(String message, Exception e, int errorCode, LoggingEvent event);

    /**
     * Set the appender for which errors are handled. This method is
     * usually called when the error handler is configured.
     *
     * @param appender The appender
     * @since 1.2
     */
    void setAppender(Appender appender);

    /**
     * Set the appender to fallback upon in case of failure.
     *
     * @param appender The backup appender
     * @since 1.2
     */
    void setBackupAppender(Appender appender);
}

