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
package org.apache.logging.log4j.io;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.spi.ExtendedLogger;

/**
 * Indicates that a provided {@link org.apache.logging.log4j.Logger} does not implement {@link ExtendedLogger}.
 *
 * @since 2.1
 */
public class IncompatibleLoggerException extends LoggingException {

    private static final long serialVersionUID = 6861427446876787666L;

    /**
     * Constructs a new IncompatibleLoggerException using the type of the provided Logger. If {@code logger} is
     * {@code null}, then the type is printed as "null". Note that this exception should only be thrown in situations
     * where a Logger was provided but did not implement ExtendedLogger.
     *
     * @param logger the provided Logger that was not an ExtendedLogger
     */
    public IncompatibleLoggerException(final Logger logger) {
        super(
            "Incompatible Logger class. Expected to implement " + ExtendedLogger.class.getName() + ". Got: "
                + (logger == null ? "null" : logger.getClass().getName())
        );
    }

}
