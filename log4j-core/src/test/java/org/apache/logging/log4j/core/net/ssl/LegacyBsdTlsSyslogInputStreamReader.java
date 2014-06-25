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
package org.apache.logging.log4j.core.net.ssl;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.util.Strings;

public class LegacyBsdTlsSyslogInputStreamReader extends TlsSyslogInputStreamReaderBase {
    private final ByteArrayOutputStream buffer;

    public LegacyBsdTlsSyslogInputStreamReader(final InputStream inputStream) {
        super(inputStream, TlsSyslogMessageFormat.LEGACY_BSD);
        buffer = new ByteArrayOutputStream();
    }

    @Override
    public String read() throws IOException {
        String message = Strings.EMPTY;
        try {
            while (true) {
                final int b = inputStream.read();
                if (b == -1) {
                    throw new EOFException("The stream has been closed or the end of stream has been reached");
                }
                buffer.write(b);
                if (b == '\n') {
                    break;
                }
            }
        }
        catch (final EOFException e) {
            if (buffer.size() > 0) {
                message = buffer.toString();
                buffer.reset();
                return message;
            }
            throw e;
        }
        message = buffer.toString();
        buffer.reset();
        return message;
    }
}
