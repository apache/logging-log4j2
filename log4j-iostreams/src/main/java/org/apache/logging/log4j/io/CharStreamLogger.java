/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.logging.log4j.io;

import java.nio.CharBuffer;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.spi.ExtendedLogger;

/**
 * 
 * @since 2.1
 */
public class CharStreamLogger {
    private final ExtendedLogger logger;
    private final Level level;
    private final Marker marker;
    private final StringBuilder msg = new StringBuilder();
    private boolean closed = false;

    public CharStreamLogger(final ExtendedLogger logger, final Level level, final Marker marker) {
        this.logger = logger;
        this.level = level == null ? logger.getLevel() : level;
        this.marker = marker;
    }

    public void close(final String fqcn) {
        synchronized (this.msg) {
            this.closed = true;
            logEnd(fqcn);
        }
    }

    private void log(final String fqcn) {
        // convert to string now so async loggers work
        this.logger.logIfEnabled(fqcn, this.level, this.marker, this.msg.toString());
        this.msg.setLength(0);
    }

    private void logEnd(final String fqcn) {
        if (this.msg.length() > 0) {
            log(fqcn);
        }
    }

    public void put(final String fqcn, final char[] cbuf, final int off, final int len) {
        put(fqcn, CharBuffer.wrap(cbuf), off, len);
    }

    public void put(final String fqcn, final CharSequence str, final int off, final int len) {
        if (len >= 0) {
            synchronized (this.msg) {
                if (this.closed) {
                    return;
                }
                int start = off;
                final int end = off + len;
                for (int pos = off; pos < end; pos++) {
                    final char c = str.charAt(pos);
                    switch (c) {
                    case '\r':
                    case '\n':
                        this.msg.append(str, start, pos);
                        start = pos + 1;
                        if (c == '\n') {
                            log(fqcn);
                        }
                        break;
                    }
                }
                this.msg.append(str, start, end);
            }
        } else {
            logEnd(fqcn);
        }
    }

    public void put(final String fqcn, final int c) {
        if (c >= 0) {
            synchronized (this.msg) {
                if (this.closed) {
                    return;
                }
                switch (c) {
                case '\n':
                    log(fqcn);
                    break;
                case '\r':
                    break;
                default:
                    this.msg.append((char) c);
                }
            }
        } else {
            logEnd(fqcn);
        }
    }
}
