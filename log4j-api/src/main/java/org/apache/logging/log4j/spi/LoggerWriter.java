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
package org.apache.logging.log4j.spi;

import java.io.IOException;
import java.io.Writer;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;

public class LoggerWriter extends Writer {
    private static final String FQCN = LoggerWriter.class.getName();
    private final AbstractLogger logger;
    private final Level level;
    private final Marker marker;
    private final StringBuilder buf = new StringBuilder();

    public LoggerWriter(AbstractLogger logger, Marker marker, Level level) {
        this.logger = logger;
        this.marker = marker;
        this.level = level;
    }

    @Override
    public void close() throws IOException {
        // don't log a blank message if the last character was a newline
        if (buf.length() > 0) {
            log();
        }
    }

    @Override
    public void flush() throws IOException {
        // flushing automatically happens when a newline is encountered
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        int currOff = off;
        synchronized (buf) {
            for (int pos = off; pos < off + len; pos++) {
                switch (cbuf[pos]) {
                case '\r':
                    buf.append(cbuf, currOff, pos - currOff);
                    currOff = pos + 1;
                    break;
                case '\n':
                    buf.append(cbuf, currOff, pos - currOff);
                    currOff = pos + 1;
                    log();
                    break;
                }
            }
            buf.append(cbuf, currOff, len - (currOff - off));
        }
    }

    private void log() {
        final Message message = logger.getMessageFactory().newMessage(buf.toString());
        buf.setLength(0);
        logger.logIfEnabled(FQCN, level, marker, message, null);
    }
}
