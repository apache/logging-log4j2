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

import org.apache.logging.log4j.util.Strings;

/**
 * Contains an SMTP client request. Handles state transitions using the following state transition table.
 * <PRE>
 * -----------+-------------------------------------------------------------------------------------------------
 * |                                 State
 * Action    +-------------+-----------+-----------+--------------+---------------+---------------+------------
 * | CONNECT     | GREET     | MAIL      | RCPT         | DATA_HDR      | DATA_BODY     | QUIT
 * -----------+-------------+-----------+-----------+--------------+---------------+---------------+------------
 * connect    | 220/GREET   | 503/GREET | 503/MAIL  | 503/RCPT     | 503/DATA_HDR  | 503/DATA_BODY | 503/QUIT
 * ehlo       | 503/CONNECT | 250/MAIL  | 503/MAIL  | 503/RCPT     | 503/DATA_HDR  | 503/DATA_BODY | 503/QUIT
 * mail       | 503/CONNECT | 503/GREET | 250/RCPT  | 503/RCPT     | 503/DATA_HDR  | 503/DATA_BODY | 250/RCPT
 * rcpt       | 503/CONNECT | 503/GREET | 503/MAIL  | 250/RCPT     | 503/DATA_HDR  | 503/DATA_BODY | 503/QUIT
 * data       | 503/CONNECT | 503/GREET | 503/MAIL  | 354/DATA_HDR | 503/DATA_HDR  | 503/DATA_BODY | 503/QUIT
 * data_end   | 503/CONNECT | 503/GREET | 503/MAIL  | 503/RCPT     | 250/QUIT      | 250/QUIT      | 503/QUIT
 * unrecog    | 500/CONNECT | 500/GREET | 500/MAIL  | 500/RCPT     | ---/DATA_HDR  | ---/DATA_BODY | 500/QUIT
 * quit       | 503/CONNECT | 503/GREET | 503/MAIL  | 503/RCPT     | 503/DATA_HDR  | 503/DATA_BODY | 250/CONNECT
 * blank_line | 503/CONNECT | 503/GREET | 503/MAIL  | 503/RCPT     | ---/DATA_BODY | ---/DATA_BODY | 503/QUIT
 * rset       | 250/GREET   | 250/GREET | 250/GREET | 250/GREET    | 250/GREET     | 250/GREET     | 250/GREET
 * vrfy       | 252/CONNECT | 252/GREET | 252/MAIL  | 252/RCPT     | 252/DATA_HDR  | 252/DATA_BODY | 252/QUIT
 * expn       | 252/CONNECT | 252/GREET | 252/MAIL  | 252/RCPT     | 252/DATA_HDR  | 252/DATA_BODY | 252/QUIT
 * help       | 211/CONNECT | 211/GREET | 211/MAIL  | 211/RCPT     | 211/DATA_HDR  | 211/DATA_BODY | 211/QUIT
 * noop       | 250/CONNECT | 250/GREET | 250/MAIL  | 250/RCPT     | 250|DATA_HDR  | 250/DATA_BODY | 250/QUIT
 * </PRE>
 */
public class SmtpRequest {
    /**
     * SMTP action received from client.
     */
    private final SmtpActionType action;
    /**
     * Current state of the SMTP state table.
     */
    private final SmtpState state;
    /**
     * Additional information passed from the client with the SMTP action.
     */
    private final String params;

    /**
     * Create a new SMTP client request.
     *
     * @param actionType type of action/command
     * @param params     remainder of command line once command is removed
     * @param state      current SMTP server state
     */
    public SmtpRequest(final SmtpActionType actionType, final String params, final SmtpState state) {
        this.action = actionType;
        this.state = state;
        this.params = params;
    }

    /**
     * Execute the SMTP request returning a response. This method models the state transition table for the SMTP server.
     *
     * @return reponse to the request
     */
    public SmtpResponse execute() {
        SmtpResponse response = null;
        if (action.isStateless()) {
            if (SmtpActionType.EXPN == action || SmtpActionType.VRFY == action) {
                response = new SmtpResponse(252, "Not supported", this.state);
            } else if (SmtpActionType.HELP == action) {
                response = new SmtpResponse(211, "No help available", this.state);
            } else if (SmtpActionType.NOOP == action) {
                response = new SmtpResponse(250, "OK", this.state);
            } else if (SmtpActionType.VRFY == action) {
                response = new SmtpResponse(252, "Not supported", this.state);
            } else if (SmtpActionType.RSET == action) {
                response = new SmtpResponse(250, "OK", SmtpState.GREET);
            } else {
                response = new SmtpResponse(500, "Command not recognized", this.state);
            }
        } else { // Stateful commands
            if (SmtpActionType.CONNECT == action) {
                if (SmtpState.CONNECT == state) {
                    response = new SmtpResponse(220, "localhost Dumbster SMTP service ready", SmtpState.GREET);
                } else {
                    response = new SmtpResponse(503, "Bad sequence of commands: " + action, this.state);
                }
            } else if (SmtpActionType.EHLO == action) {
                if (SmtpState.GREET == state) {
                    response = new SmtpResponse(250, "OK", SmtpState.MAIL);
                } else {
                    response = new SmtpResponse(503, "Bad sequence of commands: " + action, this.state);
                }
            } else if (SmtpActionType.MAIL == action) {
                if (SmtpState.MAIL == state || SmtpState.QUIT == state) {
                    response = new SmtpResponse(250, "OK", SmtpState.RCPT);
                } else {
                    response = new SmtpResponse(503, "Bad sequence of commands: " + action, this.state);
                }
            } else if (SmtpActionType.RCPT == action) {
                if (SmtpState.RCPT == state) {
                    response = new SmtpResponse(250, "OK", this.state);
                } else {
                    response = new SmtpResponse(503, "Bad sequence of commands: " + action, this.state);
                }
            } else if (SmtpActionType.DATA == action) {
                if (SmtpState.RCPT == state) {
                    response = new SmtpResponse(354, "Start mail input; end with <CRLF>.<CRLF>", SmtpState.DATA_HDR);
                } else {
                    response = new SmtpResponse(503, "Bad sequence of commands: " + action, this.state);
                }
            } else if (SmtpActionType.UNRECOG == action) {
                if (SmtpState.DATA_HDR == state || SmtpState.DATA_BODY == state) {
                    response = new SmtpResponse(-1, Strings.EMPTY, this.state);
                } else {
                    response = new SmtpResponse(500, "Command not recognized", this.state);
                }
            } else if (SmtpActionType.DATA_END == action) {
                if (SmtpState.DATA_HDR == state || SmtpState.DATA_BODY == state) {
                    response = new SmtpResponse(250, "OK", SmtpState.QUIT);
                } else {
                    response = new SmtpResponse(503, "Bad sequence of commands: " + action, this.state);
                }
            } else if (SmtpActionType.BLANK_LINE == action) {
                if (SmtpState.DATA_HDR == state) {
                    response = new SmtpResponse(-1, Strings.EMPTY, SmtpState.DATA_BODY);
                } else if (SmtpState.DATA_BODY == state) {
                    response = new SmtpResponse(-1, Strings.EMPTY, this.state);
                } else {
                    response = new SmtpResponse(503, "Bad sequence of commands: " + action, this.state);
                }
            } else if (SmtpActionType.QUIT == action) {
                if (SmtpState.QUIT == state) {
                    response = new SmtpResponse(221, "localhost Dumbster service closing transmission channel",
                        SmtpState.CONNECT);
                } else {
                    response = new SmtpResponse(503, "Bad sequence of commands: " + action, this.state);
                }
            } else {
                response = new SmtpResponse(500, "Command not recognized", this.state);
            }
        }
        return response;
    }

    /**
     * Create an SMTP request object given a line of the input stream from the client and the current internal state.
     *
     * @param s     line of input
     * @param state current state
     * @return a populated SmtpRequest object
     */
    public static SmtpRequest createRequest(final String s, final SmtpState state) {
        SmtpActionType action = null;
        String params = null;

        if (state == SmtpState.DATA_HDR) {
            if (s.equals(".")) {
                action = SmtpActionType.DATA_END;
            } else if (s.length() < 1) {
                action = SmtpActionType.BLANK_LINE;
            } else {
                action = SmtpActionType.UNRECOG;
                params = s;
            }
        } else if (state == SmtpState.DATA_BODY) {
            if (s.equals(".")) {
                action = SmtpActionType.DATA_END;
            } else {
                action = SmtpActionType.UNRECOG;
                if (s.length() < 1) {
                    params = "\n";
                } else {
                    params = s;
                }
            }
        } else {
            final String su = s.toUpperCase();
            if (su.startsWith("EHLO ") || su.startsWith("HELO")) {
                action = SmtpActionType.EHLO;
                params = s.substring(5);
            } else if (su.startsWith("MAIL FROM:")) {
                action = SmtpActionType.MAIL;
                params = s.substring(10);
            } else if (su.startsWith("RCPT TO:")) {
                action = SmtpActionType.RCPT;
                params = s.substring(8);
            } else if (su.startsWith("DATA")) {
                action = SmtpActionType.DATA;
            } else if (su.startsWith("QUIT")) {
                action = SmtpActionType.QUIT;
            } else if (su.startsWith("RSET")) {
                action = SmtpActionType.RSET;
            } else if (su.startsWith("NOOP")) {
                action = SmtpActionType.NOOP;
            } else if (su.startsWith("EXPN")) {
                action = SmtpActionType.EXPN;
            } else if (su.startsWith("VRFY")) {
                action = SmtpActionType.VRFY;
            } else if (su.startsWith("HELP")) {
                action = SmtpActionType.HELP;
            } else {
                action = SmtpActionType.UNRECOG;
            }
        }

        final SmtpRequest req = new SmtpRequest(action, params, state);
        return req;
    }

    /**
     * Get the parameters of this request (remainder of command line once the command is removed.
     *
     * @return parameters
     */
    public String getParams() {
        return params;
    }
}
