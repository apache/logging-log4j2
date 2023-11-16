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
package org.apache.logging.log4j.core.appender;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
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
        return 31 + Objects.hashCode(message);
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
        if (!Objects.equals(message, other.message)) {
            return false;
        }
        return true;
    }
}
