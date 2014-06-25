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
package org.apache.logging.log4j.core.appender;

import java.nio.charset.Charset;

/**
 * Wrapper for messages that are formatted according to RFC 5425.
 */
public class TlsSyslogFrame {
    public static final char SPACE = ' ';

    private String message;
    private int messageLengthInBytes;

    public TlsSyslogFrame(final String message) {
        setMessage(message);
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(final String message) {
        this.message = message;
        setLengthInBytes();
    }

    private void setLengthInBytes() {
        messageLengthInBytes = message.length();
    }

    public byte[] getBytes() {
        final String frame = toString();
        return frame.getBytes(Charset.defaultCharset());
    }

    @Override
    public String toString() {
        final String length = Integer.toString(messageLengthInBytes);
        return length + SPACE + message;
    }

    @Override
    public boolean equals(final Object frame) {
        return super.equals(frame);
    }

    public boolean equals(final TlsSyslogFrame frame) {
        return isLengthEquals(frame) && isMessageEquals(frame);
    }

    private boolean isLengthEquals(final TlsSyslogFrame frame) {
        return this.messageLengthInBytes == frame.messageLengthInBytes;
    }

    private boolean isMessageEquals(final TlsSyslogFrame frame) {
        return this.message.equals(frame.message);
    }
}
