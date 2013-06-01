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
package org.apache.logging.log4j;

/**
 * Exception thrown when a exception occurs while logging.  In most cases exceptions will be handled
 * within Log4j but certain Appenders may be configured to allow exceptions to propagate to the
 * application. This is a RuntimeException so that the exception may be thrown in those cases without
 * requiring all Logger methods be contained with try/catch blocks.
 *
 */
public class LoggingException extends RuntimeException {

    private static final long serialVersionUID = 6366395965071580537L;

    /**
     * Constructs a LoggingException with a message.
     * @param msg The message.
     */
    public LoggingException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a LoggingException with a message and a chained Exception.
     * @param msg The message.
     * @param ex The chained Exception.
     */
    public LoggingException(final String msg, final Exception ex) {
        super(msg, ex);
    }

    /**
     * Constructs a Logging Exception with a chained Exception and no message.
     * @param ex The chained Exception.
     */
    public LoggingException(final Exception ex) {
        super(ex);
    }
}
