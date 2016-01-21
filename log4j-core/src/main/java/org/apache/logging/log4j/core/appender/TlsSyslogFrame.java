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

import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.util.Chars;

/**
 * Wraps messages that are formatted according to RFC 5425.
 * 
 * @see <a href="https://tools.ietf.org/html/rfc5425">RFC 5425</a>
 */
public class TlsSyslogFrame {
    private final String message;
    private final int byteLength;

    public TlsSyslogFrame(final String message) {
        this.message = message;
        final byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        byteLength = messageBytes.length;
    }

    public String getMessage() {
        return this.message;
    }

    @Override
    public String toString() {
        return Integer.toString(byteLength) + Chars.SPACE + message;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TlsSyslogFrame)) {
            return false;
        }
        final TlsSyslogFrame other = (TlsSyslogFrame) obj;
        if (message == null) {
            if (other.message != null) {
                return false;
            }
        } else if (!message.equals(other.message)) {
            return false;
        }
        return true;
    }

}
