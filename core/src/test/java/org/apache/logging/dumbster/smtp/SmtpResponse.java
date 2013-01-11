/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.dumbster.smtp;

/**
 * SMTP response container.
 */
public class SmtpResponse {
    /**
     * Response code - see RFC-2821.
     */
    private final int code;
    /**
     * Response message.
     */
    private final String message;
    /**
     * New state of the SMTP server once the request has been executed.
     */
    private final SmtpState nextState;

    /**
     * Constructor.
     *
     * @param code    response code
     * @param message response message
     * @param next    next state of the SMTP server
     */
    public SmtpResponse(final int code, final String message, final SmtpState next) {
        this.code = code;
        this.message = message;
        this.nextState = next;
    }

    /**
     * Get the response code.
     *
     * @return response code
     */
    public int getCode() {
        return code;
    }

    /**
     * Get the response message.
     *
     * @return response message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get the next SMTP server state.
     *
     * @return state
     */
    public SmtpState getNextState() {
        return nextState;
    }
}
