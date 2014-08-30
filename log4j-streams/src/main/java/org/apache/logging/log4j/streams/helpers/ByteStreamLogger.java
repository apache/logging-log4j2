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

package org.apache.logging.log4j.streams.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.spi.ExtendedLogger;

public class ByteStreamLogger {
    private static final int BUFFER_SIZE = 1024;

    private final ExtendedLogger logger;
    private final Level level;
    private final Marker marker;
    private final ByteBufferInputStream in;
    private final InputStreamReader reader;
    private final char[] msgBuf = new char[BUFFER_SIZE];
    private final StringBuilder msg = new StringBuilder();
    private boolean closed;
    private final ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);

    public ByteStreamLogger(final ExtendedLogger logger, final Level level, final Marker marker, final Charset charset) {
        this.logger = logger;
        this.level = level;
        this.marker = marker;
        this.in = new ByteBufferInputStream();
        this.reader = new InputStreamReader(in, charset);
    }

    public void put(final String fqcn, final int b) throws IOException {
        if (b >= 0) {
            synchronized (msg) {
                buf.put((byte) (b & 0xFF));
                extractMessages(fqcn);
            }
        } else {
            logEnd(fqcn);
        }
    }

    public void put(final String fqcn, final byte[] b, int off, int len) throws IOException {
        if (len >= 0) {
            synchronized (msg) {
                while (len > buf.remaining()) {
                    final int remaining = buf.remaining();
                    buf.put(b, off, remaining);
                    len -= remaining;
                    off += remaining;
                    extractMessages(fqcn);
                }
                buf.put(b, off, len);
                extractMessages(fqcn);
            }
        } else {
            logEnd(fqcn);
        }
    }

    public void close(final String fqcn) {
        synchronized (msg) {
            closed = true;
            logEnd(fqcn);
//            in.close();
        }
    }

    private void extractMessages(final String fqcn) throws IOException {
        if (closed) {
            return;
        }
        int read = reader.read(msgBuf);
        while (read > 0) {
            int off = 0;
            for (int pos = 0; pos < read; pos++) {
                switch (msgBuf[pos]) {
                case '\r':
                    msg.append(msgBuf, off, pos - off);
                    off = pos + 1;
                    break;
                case '\n':
                    msg.append(msgBuf, off, pos - off);
                    off = pos + 1;
                    log(fqcn);
                    break;
                }
            }
            msg.append(msgBuf, off, read - off);
            read = reader.read(msgBuf);
        }
    }
    
    private void logEnd(final String fqcn) {
        if (msg.length() > 0) {
            log(fqcn);
        }
    }

    private void log(final String fqcn) {
        // convert to string now so async loggers work
        logger.logIfEnabled(fqcn, level, marker, msg.toString());
        msg.setLength(0);
    }

    private class ByteBufferInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            buf.flip();
            int result = -1;
            if (buf.limit() > 0) {
                result = buf.get() & 0xFF;
            }
            buf.compact();
            return result;
        }

        @Override
        public int read(final byte[] bytes, final int off, final int len) throws IOException {
            buf.flip();
            int result = -1;
            if (buf.limit() > 0) {
                result = Math.min(len, buf.limit());
                buf.get(bytes, off, result);
            }
            buf.compact();
            return result;
        }
    }
}
