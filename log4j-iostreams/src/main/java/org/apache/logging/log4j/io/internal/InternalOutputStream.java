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
package org.apache.logging.log4j.io.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.io.ByteStreamLogger;
import org.apache.logging.log4j.spi.ExtendedLogger;

/**
 * Internal class that exists primarily to allow location calculation to work.
 *
 * @since 2.12
 */
public class InternalOutputStream extends OutputStream {

    private final ByteStreamLogger logger;
    private final String fqcn;

    public InternalOutputStream(
            final ExtendedLogger logger,
            final Level level,
            final Marker marker,
            final Charset charset,
            final String fqcn) {
        this.logger = new ByteStreamLogger(logger, level, marker, charset);
        this.fqcn = fqcn;
    }

    @Override
    public void close() throws IOException {
        this.logger.close(this.fqcn);
    }

    @Override
    public void flush() throws IOException {
        // do nothing
    }

    @Override
    public void write(final byte[] b) throws IOException {
        this.logger.put(this.fqcn, b, 0, b.length);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        this.logger.put(this.fqcn, b, off, len);
    }

    @Override
    public void write(final int b) throws IOException {
        this.logger.put(this.fqcn, (byte) (b & 0xFF));
    }
}
