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
import java.util.Arrays;

public class TlsSyslogInputStreamReader extends TlsSyslogInputStreamReaderBase {
    private static final char SPACE = ' ';

    private final ByteArrayOutputStream messageBuffer;
    private final byte[] messagePartBuffer;
    private final byte[] lengthBuffer;
    private final int messagePartBufferSize = 8192;
    private final int lengthBufferSize = 8192;
    private int position = 0;
    private int nextMessageLength = 0;

    public TlsSyslogInputStreamReader(final InputStream inputStream) {
        super(inputStream, TlsSyslogMessageFormat.SYSLOG);
        this.messageBuffer = new ByteArrayOutputStream(messagePartBufferSize);
        this.lengthBuffer = new byte[lengthBufferSize];
        this.messagePartBuffer = new byte[messagePartBufferSize];
    }

    @Override
    public String read() throws IOException {
        readMessageLength();
        readMessage();
        final String message =  buildMessage();
        return message;
    }

    private void readMessageLength() throws IOException {
        readBytesUntilNextSpace();
        calculateNextMessageLength();
    }

    private void readMessage() throws IOException {
        int remainder = nextMessageLength;
        while (remainder > 0)  {
            final int bytesToRead = Math.min(remainder, messagePartBufferSize);
            final int n = inputStream.read(messagePartBuffer, 0, bytesToRead);
            messageBuffer.write(messagePartBuffer, 0, n);
            remainder -= n;
        }
    }

    private String buildMessage() {
        final String message = messageBuffer.toString();
        messageBuffer.reset();
        return message;
    }

    private void readBytesUntilNextSpace() throws IOException {
        for (int i = 0; i < lengthBufferSize; i++) {
            final int b = inputStream.read();
            if (b < 0) {
                throw new EOFException("The stream has been closed or the end of stream has been reached");
            }
            final byte currentByte = (byte)(b & 0xff);
            if (currentByte == SPACE) {
                position = i;
                break;
            }
            lengthBuffer[i] = currentByte;
        }
    }

    private void calculateNextMessageLength() {
        final byte[] length = Arrays.copyOfRange(lengthBuffer, 0, position);
        nextMessageLength = Integer.parseInt(new String(length));
    }
}
