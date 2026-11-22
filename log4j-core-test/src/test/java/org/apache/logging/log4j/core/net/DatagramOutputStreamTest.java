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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DatagramOutputStreamTest {

    private static final String LOOPBACK = "127.0.0.1";
    private static final int ENCODER_CHUNK_SIZE = 8 * 1024;
    private static final int MAX_DATAGRAM_PAYLOAD = 65_507;

    /**
     * Returns {@code length} bytes with a repeating pattern, so that truncation at the wrong offset is detected.
     */
    private static byte[] pattern(final int length) {
        final byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) (i % 251);
        }
        return bytes;
    }

    private static DatagramSocket createReceiver() throws Exception {
        final DatagramSocket receiver = new DatagramSocket(0, InetAddress.getByName(LOOPBACK));
        receiver.setSoTimeout(2000);
        return receiver;
    }

    private static byte[] receive(final DatagramSocket receiver) throws Exception {
        // One byte more than the maximum, to detect datagrams that are too long
        final byte[] buffer = new byte[MAX_DATAGRAM_PAYLOAD + 1];
        final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        receiver.receive(packet);
        return Arrays.copyOf(buffer, packet.getLength());
    }

    private static void assertTruncationWarning(final ListStatusListener statusListener) {
        assertThat(statusListener.getStatusData())
                .anyMatch(data -> data.getLevel() == Level.WARN
                        && data.getMessage().getFormattedMessage().contains("truncated"));
    }

    @Test
    @UsingStatusListener
    void flushShouldTruncateChunkedPayloadInsideWrite(final ListStatusListener statusListener) throws Exception {
        // 9 chunks of 8 KiB: the limit falls inside the 8th chunk
        final byte[] event = pattern(9 * ENCODER_CHUNK_SIZE);
        try (DatagramSocket receiver = createReceiver()) {
            try (DatagramOutputStream out = new DatagramOutputStream(LOOPBACK, receiver.getLocalPort(), null, null)) {
                for (int i = 0; i < 9; i++) {
                    out.write(event, i * ENCODER_CHUNK_SIZE, ENCODER_CHUNK_SIZE);
                }
                out.flush();
            }
            assertThat(receive(receiver)).isEqualTo(Arrays.copyOf(event, MAX_DATAGRAM_PAYLOAD));
            assertTruncationWarning(statusListener);
        }
    }

    @Test
    @UsingStatusListener
    void closeShouldTruncateSingleOversizedWrite(final ListStatusListener statusListener) throws Exception {
        final byte[] event = pattern(MAX_DATAGRAM_PAYLOAD + 1);
        try (DatagramSocket receiver = createReceiver()) {
            try (DatagramOutputStream out = new DatagramOutputStream(LOOPBACK, receiver.getLocalPort(), null, null)) {
                out.write(event);
            }
            assertThat(receive(receiver)).isEqualTo(Arrays.copyOf(event, MAX_DATAGRAM_PAYLOAD));
            assertTruncationWarning(statusListener);
        }
    }

    @Test
    @UsingStatusListener
    void flushShouldSendSmallEventAfterTruncatedPayload(final ListStatusListener statusListener) throws Exception {
        final byte[] large = pattern(9 * ENCODER_CHUNK_SIZE);
        final byte[] small = {1, 2, 3, 4};
        try (DatagramSocket receiver = createReceiver()) {
            try (DatagramOutputStream out = new DatagramOutputStream(LOOPBACK, receiver.getLocalPort(), null, null)) {
                out.write(large);
                out.flush();
                out.write(small);
                out.flush();
            }
            assertThat(receive(receiver)).hasSize(MAX_DATAGRAM_PAYLOAD);
            assertThat(receive(receiver)).isEqualTo(small);
            assertTruncationWarning(statusListener);
        }
    }

    @Test
    @UsingStatusListener
    void flushShouldTruncateFooterFirst(final ListStatusListener statusListener) throws Exception {
        final byte[] event = pattern(MAX_DATAGRAM_PAYLOAD - 2);
        final byte[] footer = {'E', 'N', 'D', '\n'};
        try (DatagramSocket receiver = createReceiver()) {
            try (DatagramOutputStream out = new DatagramOutputStream(LOOPBACK, receiver.getLocalPort(), null, footer)) {
                out.write(event);
                out.flush();
            }
            final byte[] received = receive(receiver);
            assertThat(received).hasSize(MAX_DATAGRAM_PAYLOAD);
            assertThat(Arrays.copyOf(received, event.length)).isEqualTo(event);
            assertThat(Arrays.copyOfRange(received, event.length, received.length))
                    .containsExactly('E', 'N');
            assertTruncationWarning(statusListener);
        }
    }

    @Test
    void flushShouldSendConcatenatedChunks() throws Exception {
        final byte[] chunk = pattern(ENCODER_CHUNK_SIZE);
        try (DatagramSocket receiver = createReceiver()) {
            try (DatagramOutputStream out = new DatagramOutputStream(LOOPBACK, receiver.getLocalPort(), null, null)) {
                out.write(chunk);
                out.write(chunk);
                out.flush();
            }
            final byte[] received = receive(receiver);
            assertThat(received).hasSize(ENCODER_CHUNK_SIZE * 2);
            assertThat(received).startsWith(chunk).endsWith(chunk);
        }
    }

    @ParameterizedTest
    @CsvSource({"-1, 2", "0, -1", "4, 5", "9, 0", "1, 2147483647"})
    void writeShouldRejectInvalidRange(final int offset, final int length) throws Exception {
        final byte[] bytes = new byte[8];
        try (DatagramSocket receiver = createReceiver();
                DatagramOutputStream out = new DatagramOutputStream(LOOPBACK, receiver.getLocalPort(), null, null)) {
            assertThatThrownBy(() -> out.write(bytes, offset, length))
                    .isInstanceOf(IndexOutOfBoundsException.class)
                    .hasMessage("Range [%s, %s + %s) out of bounds for length 8", offset, offset, length);
        }
    }
}
