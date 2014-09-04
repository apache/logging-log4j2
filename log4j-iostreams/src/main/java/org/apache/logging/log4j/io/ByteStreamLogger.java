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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.spi.ExtendedLogger;

/**
 * 
 * @since 2.1
 */
public class ByteStreamLogger {
    private class ByteBufferInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            ByteStreamLogger.this.buf.flip();
            int result = -1;
            if (ByteStreamLogger.this.buf.limit() > 0) {
                result = ByteStreamLogger.this.buf.get() & 0xFF;
            }
            ByteStreamLogger.this.buf.compact();
            return result;
        }

        @Override
        public int read(final byte[] bytes, final int off, final int len) throws IOException {
            ByteStreamLogger.this.buf.flip();
            int result = -1;
            if (ByteStreamLogger.this.buf.limit() > 0) {
                result = Math.min(len, ByteStreamLogger.this.buf.limit());
                ByteStreamLogger.this.buf.get(bytes, off, result);
            }
            ByteStreamLogger.this.buf.compact();
            return result;
        }
    }

    private static final int BUFFER_SIZE = 1024;
    private final ExtendedLogger logger;
    private final Level level;
    private final Marker marker;
    private final InputStreamReader reader;
    private final char[] msgBuf = new char[BUFFER_SIZE];
    private final StringBuilder msg = new StringBuilder();
    private boolean closed;

    private final ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);

    public ByteStreamLogger(final ExtendedLogger logger, final Level level, final Marker marker, final Charset charset) {
        this.logger = logger;
        this.level = level == null ? logger.getLevel() : level;
        this.marker = marker;
        this.reader = new InputStreamReader(new ByteBufferInputStream(),
            charset == null ? Charset.defaultCharset() : charset);
    }

    public void close(final String fqcn) {
        synchronized (this.msg) {
            this.closed = true;
            logEnd(fqcn);
        }
    }

    private void extractMessages(final String fqcn) throws IOException {
        if (this.closed) {
            return;
        }
        int read = this.reader.read(this.msgBuf);
        while (read > 0) {
            int off = 0;
            for (int pos = 0; pos < read; pos++) {
                switch (this.msgBuf[pos]) {
                case '\r':
                    this.msg.append(this.msgBuf, off, pos - off);
                    off = pos + 1;
                    break;
                case '\n':
                    this.msg.append(this.msgBuf, off, pos - off);
                    off = pos + 1;
                    log(fqcn);
                    break;
                }
            }
            this.msg.append(this.msgBuf, off, read - off);
            read = this.reader.read(this.msgBuf);
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

    public void put(final String fqcn, final byte[] b, final int off, final int len) throws IOException {
        int curOff = off;
        int curLen = len;
        if (curLen >= 0) {
            synchronized (this.msg) {
                while (curLen > this.buf.remaining()) {
                    final int remaining = this.buf.remaining();
                    this.buf.put(b, curOff, remaining);
                    curLen -= remaining;
                    curOff += remaining;
                    extractMessages(fqcn);
                }
                this.buf.put(b, curOff, curLen);
                extractMessages(fqcn);
            }
        } else {
            logEnd(fqcn);
        }
    }

    public void put(final String fqcn, final int b) throws IOException {
        if (b >= 0) {
            synchronized (this.msg) {
                this.buf.put((byte) (b & 0xFF));
                extractMessages(fqcn);
            }
        } else {
            logEnd(fqcn);
        }
    }
}
