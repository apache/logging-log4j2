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
package org.apache.log4j.config;

/**
 * Thrown when an error is encountered whilst attempting to set a property
 * using the {@link PropertySetter} utility class.
 *
 * @since 1.1
 */
public class PropertySetterException extends Exception {
    private static final long serialVersionUID = -1352613734254235861L;

    /**
     * The root cause.
     */
    protected Throwable rootCause;

    /**
     * Construct the exception with the given message.
     *
     * @param msg The message
     */
    public PropertySetterException(final String msg) {
        super(msg);
    }

    /**
     * Construct the exception with the given root cause.
     *
     * @param rootCause The root cause
     */
    public PropertySetterException(final Throwable rootCause) {
        super();
        this.rootCause = rootCause;
    }

    /**
     * Returns descriptive text on the cause of this exception.
     *
     * @return the descriptive text.
     */
    @Override
    public String getMessage() {
        String msg = super.getMessage();
        if (msg == null && rootCause != null) {
            msg = rootCause.getMessage();
        }
        return msg;
    }
}
