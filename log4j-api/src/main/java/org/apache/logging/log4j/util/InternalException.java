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
package org.apache.logging.log4j.util;

/**
 * Exception thrown when an error occurs while accessing internal resources. This is generally used to
 * convert checked exceptions to runtime exceptions.
 */
public class InternalException extends RuntimeException {

    private static final long serialVersionUID = 6366395965071580537L;

    /**
     * Construct an exception with a message.
     *
     * @param message The reason for the exception
     */
    public InternalException(final String message) {
        super(message);
    }

    /**
     * Construct an exception with a message and underlying cause.
     *
     * @param message The reason for the exception
     * @param cause The underlying cause of the exception
     */
    public InternalException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Construct an exception with an underlying cause.
     *
     * @param cause The underlying cause of the exception
     */
    public InternalException(final Throwable cause) {
        super(cause);
    }
}
