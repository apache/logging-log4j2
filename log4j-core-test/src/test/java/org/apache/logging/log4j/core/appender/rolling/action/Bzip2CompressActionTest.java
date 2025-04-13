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
package org.apache.logging.log4j.core.appender.rolling.action;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests Bzip2CompressAction.
 */
class Bzip2CompressActionTest {

    @Test
    void testConstructorDisallowsNullSource() {
        assertThrows(NullPointerException.class, () -> new CommonsCompressAction("bzip2", null, new File("any"), true));
    }

    @Test
    void testConstructorDisallowsNullDestination() {
        assertThrows(NullPointerException.class, () -> new CommonsCompressAction("bzip2", new File("any"), null, true));
    }

    @Test
    void testExecuteReturnsFalseIfSourceDoesNotExist() throws IOException {
        File source = new File("any");
        while (source.exists()) {
            source = new File(source.getName() + Math.random());
        }
        final boolean actual = CommonsCompressAction.execute("bzip2", source, new File("any2"), true);
        assertFalse(actual, "Cannot compress non-existing file");
    }

    @Test
    void testExecuteCompressesSourceFileToDestinationFile(@TempDir final File tempDir) throws IOException {
        final String LINE1 = "Here is line 1. Random text: ABCDEFGHIJKLMNOPQRSTUVWXYZ\r\n";
        final String LINE2 = "Here is line 2. Random text: ABCDEFGHIJKLMNOPQRSTUVWXYZ\r\n";
        final String LINE3 = "Here is line 3. Random text: ABCDEFGHIJKLMNOPQRSTUVWXYZ\r\n";
        final File source = new File(tempDir, "compressme");
        try (final FileWriter fw = new FileWriter(source, false)) {
            fw.write(LINE1);
            fw.write(LINE2);
            fw.write(LINE3);
            fw.flush();
        }
        final File destination = new File(tempDir, "compressme.bz2");
        assertFalse(destination.exists(), "Destination should not exist yet");

        final boolean actual = CommonsCompressAction.execute("bzip2", source, destination, true);
        assertTrue(actual, "Bzip2CompressAction should have succeeded");
        assertTrue(destination.exists(), "Destination should exist after Bzip2CompressAction");
        assertFalse(source.exists(), "Source should have been deleted");

        final byte[] bz2 = new byte[] {
            (byte) 0x42,
            (byte) 0x5A,
            (byte) 0x68,
            (byte) 0x39,
            (byte) 0x31,
            (byte) 0x41,
            (byte) 0x59,
            (byte) 0x26,
            (byte) 0x53,
            (byte) 0x59,
            (byte) 0x9C,
            (byte) 0xE1,
            (byte) 0xE8,
            (byte) 0x2D,
            (byte) 0x00,
            (byte) 0x00,
            (byte) 0x1C,
            (byte) 0xDF,
            (byte) 0x80,
            (byte) 0x00,
            (byte) 0x12,
            (byte) 0x40,
            (byte) 0x01,
            (byte) 0x38,
            (byte) 0x10,
            (byte) 0x3F,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xF0,
            (byte) 0x26,
            (byte) 0x27,
            (byte) 0x9C,
            (byte) 0x40,
            (byte) 0x20,
            (byte) 0x00,
            (byte) 0x70,
            (byte) 0x63,
            (byte) 0x4D,
            (byte) 0x06,
            (byte) 0x80,
            (byte) 0x19,
            (byte) 0x34,
            (byte) 0x06,
            (byte) 0x46,
            (byte) 0x9A,
            (byte) 0x18,
            (byte) 0x9A,
            (byte) 0x30,
            (byte) 0xCF,
            (byte) 0xFD,
            (byte) 0x55,
            (byte) 0x4D,
            (byte) 0x0D,
            (byte) 0x06,
            (byte) 0x9A,
            (byte) 0x0C,
            (byte) 0x40,
            (byte) 0x1A,
            (byte) 0x1A,
            (byte) 0x34,
            (byte) 0x34,
            (byte) 0xCD,
            (byte) 0x46,
            (byte) 0x05,
            (byte) 0x6B,
            (byte) 0x19,
            (byte) 0x92,
            (byte) 0x23,
            (byte) 0x5E,
            (byte) 0xB5,
            (byte) 0x2E,
            (byte) 0x79,
            (byte) 0x65,
            (byte) 0x41,
            (byte) 0x81,
            (byte) 0x33,
            (byte) 0x4B,
            (byte) 0x53,
            (byte) 0x5B,
            (byte) 0x62,
            (byte) 0x75,
            (byte) 0x0A,
            (byte) 0x14,
            (byte) 0xB6,
            (byte) 0xB7,
            (byte) 0x37,
            (byte) 0xB8,
            (byte) 0x38,
            (byte) 0xB9,
            (byte) 0x39,
            (byte) 0xBA,
            (byte) 0x2A,
            (byte) 0x4E,
            (byte) 0xEA,
            (byte) 0xEC,
            (byte) 0xEE,
            (byte) 0xAD,
            (byte) 0xE1,
            (byte) 0xE5,
            (byte) 0x63,
            (byte) 0xD3,
            (byte) 0x22,
            (byte) 0xE8,
            (byte) 0x90,
            (byte) 0x52,
            (byte) 0xA9,
            (byte) 0x7A,
            (byte) 0x68,
            (byte) 0x90,
            (byte) 0x5C,
            (byte) 0x82,
            (byte) 0x0B,
            (byte) 0x51,
            (byte) 0xBF,
            (byte) 0x24,
            (byte) 0x61,
            (byte) 0x7F,
            (byte) 0x17,
            (byte) 0x72,
            (byte) 0x45,
            (byte) 0x38,
            (byte) 0x50,
            (byte) 0x90,
            (byte) 0x9C,
            (byte) 0xE1,
            (byte) 0xE8,
            (byte) 0x2D
        };
        assertEquals(bz2.length, destination.length());

        // check the compressed contents
        try (final FileInputStream fis = new FileInputStream(destination)) {
            final byte[] actualBz2 = new byte[bz2.length];
            int n = 0;
            int offset = 0;
            do {
                n = fis.read(actualBz2, offset, actualBz2.length - offset);
                offset += n;
            } while (offset < actualBz2.length);
            assertArrayEquals(bz2, actualBz2, "Compressed data corrupt");
        }

        // uncompress
        try (final BZip2CompressorInputStream bzin = new BZip2CompressorInputStream(new ByteArrayInputStream(bz2))) {
            final StringBuilder sb = new StringBuilder();
            final byte[] buf = new byte[1024];
            int n = 0;
            while ((n = bzin.read(buf, 0, buf.length)) > -1) {
                sb.append(new String(buf, 0, n));
            }
            assertEquals(LINE1 + LINE2 + LINE3, sb.toString());
        }
    }
}
