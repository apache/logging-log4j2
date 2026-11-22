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
package org.apache.logging.log4j.core.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * OutputStream for UDP connections.
 */
public class DatagramOutputStream extends OutputStream {

    /**
     * Allow subclasses access to the status logger without creating another instance.
     */
    protected static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * IPv4 UDP payload limit (65,535 - 8 UDP - 20 IP). {@link DatagramSocket#send} rejects larger packets.
     * <p>
     *     Longer datagrams are truncated to this length: RFC 5424 receivers must tolerate truncated messages.
     * </p>
     */
    private static final int MAX_DATAGRAM_LENGTH = 65_507;

    private static final int SHIFT_1 = 8;
    private static final int SHIFT_2 = 16;
    private static final int SHIFT_3 = 24;

    private DatagramSocket datagramSocket;
    private final InetAddress inetAddress;
    private final int port;

    private final ByteArrayOutputStream data = new ByteArrayOutputStream();

    /**
     * Number of bytes of the current datagram that did not fit in {@link #MAX_DATAGRAM_LENGTH}.
     */
    private long truncatedLength;

    private final byte[] header;
    private final byte[] footer;

    /**
     * The Constructor.
     * @param host The host to connect to.
     * @param port The port on the host.
     */
    public DatagramOutputStream(final String host, final int port, final byte[] header, final byte[] footer) {
        this.port = port;
        this.header = header;
        this.footer = footer;
        try {
            inetAddress = InetAddress.getByName(host);
        } catch (final UnknownHostException ex) {
            final String msg = "Could not find host " + host;
            LOGGER.error(msg, ex);
            throw new AppenderLoggingException(msg, ex);
        }

        try {
            datagramSocket = new DatagramSocket();
        } catch (final SocketException ex) {
            final String msg = "Could not instantiate DatagramSocket to " + host;
            LOGGER.error(msg, ex);
            throw new AppenderLoggingException(msg, ex);
        }
    }

    @Override
    public synchronized void write(final byte[] bytes, final int offset, final int length) throws IOException {
        checkFromIndexSize(offset, length, bytes.length);
        copy(bytes, offset, length);
    }

    @Override
    public synchronized void write(final int i) throws IOException {
        copy(new byte[] {(byte) (i >>> SHIFT_3), (byte) (i >>> SHIFT_2), (byte) (i >>> SHIFT_1), (byte) i}, 0, 4);
    }

    @Override
    public synchronized void write(final byte[] bytes) throws IOException {
        copy(bytes, 0, bytes.length);
    }

    @Override
    public synchronized void flush() throws IOException {
        try {
            if (data.size() > 0 && datagramSocket != null && inetAddress != null) {
                if (footer != null) {
                    copy(footer, 0, footer.length);
                }
                if (truncatedLength > 0) {
                    LOGGER.warn(
                            "UDP datagram truncated from {} to {} bytes",
                            data.size() + truncatedLength,
                            MAX_DATAGRAM_LENGTH);
                }
                final byte[] payload = data.toByteArray();
                datagramSocket.send(new DatagramPacket(payload, payload.length, inetAddress, port));
            }
        } finally {
            data.reset();
            truncatedLength = 0;
            if (header != null) {
                copy(header, 0, header.length);
            }
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (datagramSocket != null) {
            if (data.size() > 0) {
                flush();
            }
            datagramSocket.close();
            datagramSocket = null;
        }
    }

    /**
     * Java 8 equivalent of {@code Objects.checkFromIndexSize}, with the same exception message.
     */
    private static void checkFromIndexSize(final int fromIndex, final int size, final int length) {
        if ((length | fromIndex | size) < 0 || size > length - fromIndex) {
            throw new IndexOutOfBoundsException(
                    String.format("Range [%s, %<s + %s) out of bounds for length %s", fromIndex, size, length));
        }
    }

    /**
     * Appends as many bytes as still fit in the datagram and counts the remaining ones as truncated.
     */
    private void copy(final byte[] bytes, final int offset, final int length) {
        final int copied = Math.min(length, MAX_DATAGRAM_LENGTH - data.size());
        if (copied > 0) {
            data.write(bytes, offset, copied);
        }
        truncatedLength += length - copied;
    }
}
