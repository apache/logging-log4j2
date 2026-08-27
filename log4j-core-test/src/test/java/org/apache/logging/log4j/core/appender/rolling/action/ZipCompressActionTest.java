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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.zip.Deflater;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ZipCompressActionTest {

    @Test
    void testRejectsCompressionLevelBelowRange(@TempDir File tempDir) {
        File source = new File(tempDir, "invalid-low.log");
        File dest = new File(tempDir, "invalid-low.log.zip");

        assertThrows(IllegalArgumentException.class, () -> new ZipCompressAction(source, dest, true, -2));
    }

    @Test
    void testRejectsCompressionLevelAboveRange(@TempDir File tempDir) {
        File source = new File(tempDir, "invalid-high.log");
        File dest = new File(tempDir, "invalid-high.log.zip");

        assertThrows(IllegalArgumentException.class, () -> new ZipCompressAction(source, dest, true, 10));
    }

    @Test
    void testAcceptsCompressionLevelRangeBounds(@TempDir File tempDir) {
        File source = new File(tempDir, "valid.log");
        File dest = new File(tempDir, "valid.log.zip");

        new ZipCompressAction(source, dest, true, Deflater.DEFAULT_COMPRESSION);
        new ZipCompressAction(source, dest, true, Deflater.NO_COMPRESSION);
        new ZipCompressAction(source, dest, true, Deflater.BEST_COMPRESSION);
    }

    @Test
    void testCompression(@TempDir File tempDir) throws IOException {
        File source = new File(tempDir, "test.log");
        File dest = new File(tempDir, "test.log.zip");
        writeContent(source, "test data");

        ZipCompressAction action = new ZipCompressAction(source, dest, true, Deflater.DEFAULT_COMPRESSION);

        assertTrue(action.execute());
        assertTrue(dest.exists(), "Compressed file must exist after execute()");
        assertFalse(source.exists(), "Source file must be deleted after compression");
    }

    private static void writeContent(final File file, final String content) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }
}
