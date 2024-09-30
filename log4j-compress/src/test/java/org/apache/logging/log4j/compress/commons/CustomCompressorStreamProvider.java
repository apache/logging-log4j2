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
package org.apache.logging.log4j.compress.commons;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamProvider;

public class CustomCompressorStreamProvider implements CompressorStreamProvider {

    static final String ALGORITHM = "custom";

    @Override
    public CompressorInputStream createCompressorInputStream(String name, InputStream in, boolean decompressUntilEOF)
            throws CompressorException {
        if (ALGORITHM.equalsIgnoreCase(name)) {
            return new CustomCompressorInputStream(in);
        }
        throw new CompressorException("Compressor: " + name + " not found.");
    }

    @Override
    public CompressorOutputStream<OutputStream> createCompressorOutputStream(String name, OutputStream out)
            throws CompressorException {
        if (ALGORITHM.equalsIgnoreCase(name)) {
            return new CustomCompressorOutputStream(out);
        }
        throw new CompressorException("Compressor: " + name + " not found.");
    }

    @Override
    public Set<String> getInputStreamCompressorNames() {
        return Set.of(ALGORITHM);
    }

    @Override
    public Set<String> getOutputStreamCompressorNames() {
        return Set.of(ALGORITHM);
    }

    private static class CustomCompressorInputStream extends CompressorInputStream {

        private final InputStream in;

        public CustomCompressorInputStream(InputStream in) {
            this.in = in;
        }

        @Override
        public int read() throws IOException {
            return in.read();
        }
    }

    private static class CustomCompressorOutputStream extends CompressorOutputStream<OutputStream> {

        public CustomCompressorOutputStream(OutputStream out) {
            super(out);
        }
    }
}
