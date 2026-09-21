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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;

class DatagramOutputStreamTest {

    private static final String LOOPBACK = "127.0.0.1";
    private static final int ENCODER_CHUNK_SIZE = 8 * 1024;
    private static final int MAX_DATAGRAM_PAYLOAD = 65_507;

    @Test
    @UsingStatusListener
    void flushShouldWarnAndDropWhenChunkedPayloadExceedsMaxDatagramSize(final ListStatusListener statusListener)
            throws Exception {
        final InetAddress loopback = InetAddress.getByName(LOOPBACK);
        try (DatagramSocket receiver = new DatagramSocket(0, loopback)) {
            receiver.setSoTimeout(500);
            final int port = receiver.getLocalPort();
            try (DatagramOutputStream out = new DatagramOutputStream(LOOPBACK, port, null, null)) {
                final byte[] chunk = new byte[ENCODER_CHUNK_SIZE];
                for (int i = 0; i < 9; i++) {
                    out.write(chunk);
                }
                assertDoesNotThrow(out::flush);
            }
            final DatagramPacket packet = new DatagramPacket(new byte[64], 64);
            assertThrows(SocketTimeoutException.class, () -> receiver.receive(packet));
            assertThat(statusListener.getStatusData())
                    .anyMatch(data -> data.getLevel() == Level.WARN
                            && data.getMessage().getFormattedMessage().contains("exceeds"));
        }
    }

    @Test
    @UsingStatusListener
    void closeShouldWarnAndDropWhenSingleWriteExceedsMaxDatagramSize(final ListStatusListener statusListener)
            throws Exception {
        final InetAddress loopback = InetAddress.getByName(LOOPBACK);
        try (DatagramSocket receiver = new DatagramSocket(0, loopback)) {
            receiver.setSoTimeout(500);
            final int port = receiver.getLocalPort();
            try (DatagramOutputStream out = new DatagramOutputStream(LOOPBACK, port, null, null)) {
                out.write(new byte[MAX_DATAGRAM_PAYLOAD + 1]);
            }
            final DatagramPacket packet = new DatagramPacket(new byte[64], 64);
            assertThrows(SocketTimeoutException.class, () -> receiver.receive(packet));
            assertThat(statusListener.getStatusData())
                    .anyMatch(data -> data.getLevel() == Level.WARN
                            && data.getMessage().getFormattedMessage().contains("exceeds"));
        }
    }

    @Test
    @UsingStatusListener
    void flushShouldSendSmallEventAfterDroppingOversizedPayload(final ListStatusListener statusListener)
            throws Exception {
        final InetAddress loopback = InetAddress.getByName(LOOPBACK);
        try (DatagramSocket receiver = new DatagramSocket(0, loopback)) {
            receiver.setSoTimeout(2000);
            final int port = receiver.getLocalPort();
            final byte[] small = {1, 2, 3, 4};
            try (DatagramOutputStream out = new DatagramOutputStream(LOOPBACK, port, null, null)) {
                final byte[] chunk = new byte[ENCODER_CHUNK_SIZE];
                for (int i = 0; i < 9; i++) {
                    out.write(chunk);
                }
                assertDoesNotThrow(out::flush);
                out.write(small);
                out.flush();
            }
            final byte[] received = new byte[MAX_DATAGRAM_PAYLOAD];
            final DatagramPacket packet = new DatagramPacket(received, received.length);
            receiver.receive(packet);
            assertThat(packet.getLength()).isEqualTo(small.length);
            assertThat(received).startsWith(small);
            assertThat(statusListener.getStatusData())
                    .anyMatch(data -> data.getLevel() == Level.WARN
                            && data.getMessage().getFormattedMessage().contains("exceeds"));
        }
    }

    @Test
    void flushShouldSendConcatenatedChunks() throws Exception {
        final InetAddress loopback = InetAddress.getByName(LOOPBACK);
        try (DatagramSocket receiver = new DatagramSocket(0, loopback)) {
            receiver.setSoTimeout(2000);
            final int port = receiver.getLocalPort();
            final byte[] chunk = new byte[ENCODER_CHUNK_SIZE];
            for (int i = 0; i < chunk.length; i++) {
                chunk[i] = (byte) i;
            }
            try (DatagramOutputStream out = new DatagramOutputStream(LOOPBACK, port, null, null)) {
                out.write(chunk);
                out.write(chunk);
                out.flush();
            }
            final byte[] received = new byte[MAX_DATAGRAM_PAYLOAD];
            final DatagramPacket packet = new DatagramPacket(received, received.length);
            receiver.receive(packet);
            assertThat(packet.getLength()).isEqualTo(ENCODER_CHUNK_SIZE * 2);
            assertThat(received).startsWith(chunk);
        }
    }
}
