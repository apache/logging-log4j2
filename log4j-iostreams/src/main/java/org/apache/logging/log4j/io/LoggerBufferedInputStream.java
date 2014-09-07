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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.spi.ExtendedLogger;

/**
 * 
 * @since 2.1
 */
public class LoggerBufferedInputStream extends BufferedInputStream {
    private static final String FQCN = LoggerBufferedInputStream.class.getName();

    protected LoggerBufferedInputStream(final InputStream in, final Charset charset, final ExtendedLogger logger,
                                        final String fqcn, final Level level, final Marker marker) {
        super(new LoggerInputStream(in, charset, logger, fqcn == null ? FQCN : fqcn, level, marker));
    }

    protected LoggerBufferedInputStream(final InputStream in, final Charset charset, final int size,
                                        final ExtendedLogger logger, final String fqcn, final Level level,
                                        final Marker marker) {
        super(new LoggerInputStream(in, charset, logger, fqcn == null ? FQCN : fqcn, level, marker), size);
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
    
    @Override
    public synchronized int read() throws IOException {
        return super.read();
    }
    
    @Override
    public int read(final byte[] b) throws IOException {
        return super.read(b, 0, b.length);
    }
    
    @Override
    public synchronized int read(final byte[] b, final int off, final int len) throws IOException {
        return super.read(b, off, len);
    }

    @Override
    public String toString() {
        return LoggerBufferedInputStream.class.getSimpleName() + "{stream=" + this.in + '}';
    }
}
