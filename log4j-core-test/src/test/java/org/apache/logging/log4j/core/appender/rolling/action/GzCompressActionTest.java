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

class GzCompressActionTest {

    @Test
    void testRejectsCompressionLevelLowerThanDefault(@TempDir File tempDir) {
        File source = new File(tempDir, "invalid-low.log");
        File dest = new File(tempDir, "invalid-low.log.gz");

        assertThrows(IllegalArgumentException.class, () -> new GzCompressAction(source, dest, true, -2, 0));
    }

    @Test
    void testRejectsCompressionLevelHigherThanBest(@TempDir File tempDir) {
        File source = new File(tempDir, "invalid-high.log");
        File dest = new File(tempDir, "invalid-high.log.gz");

        assertThrows(IllegalArgumentException.class, () -> new GzCompressAction(source, dest, true, 10, 0));
    }

    @Test
    void testAcceptsDeflaterRangeBounds(@TempDir File tempDir) {
        File source = new File(tempDir, "valid.log");
        File dest = new File(tempDir, "valid.log.gz");

        new GzCompressAction(source, dest, true, Deflater.DEFAULT_COMPRESSION, 0);
        new GzCompressAction(source, dest, true, Deflater.BEST_COMPRESSION, 0);
    }

    /** Issue #4012 — when maxDelaySeconds > 0, compression must be deferred by a random 0..max seconds. */
    @Test
    void testRandomDelayBeforeCompression(@TempDir File tempDir) throws IOException {
        File source = new File(tempDir, "test.log");
        File dest = new File(tempDir, "test.log.gz");
        try (FileWriter writer = new FileWriter(source)) {
            writer.write("test data");
        }
        int maxDelay = 2; // seconds
        GzCompressAction action = new GzCompressAction(source, dest, true, 0, maxDelay);
        long start = System.currentTimeMillis();
        action.execute();
        long elapsed = System.currentTimeMillis() - start;

        // Must complete within maxDelay + small margin
        assertTrue(
                elapsed <= (maxDelay * 1000L) + 500,
                "Compression should not exceed maxDelay=" + maxDelay + "s, but took " + elapsed + "ms");
        // Destination must be created
        assertTrue(dest.exists(), "Compressed file must exist after execute()");
        // Source must be deleted (deleteSource=true)
        assertFalse(source.exists(), "Source file must be deleted after compression");
    }

    /**
     * Issue #4012 — when maxDelaySeconds=0, no delay is applied (backward compatibility).
     * Compression must complete well under 500ms.
     */
    @Test
    void testNoDelayWhenMaxDelayIsZero(@TempDir File tempDir) throws IOException {
        File source = new File(tempDir, "test-nodelay.log");
        File dest = new File(tempDir, "test-nodelay.log.gz");
        try (FileWriter writer = new FileWriter(source)) {
            writer.write("test data no delay");
        }
        GzCompressAction action = new GzCompressAction(source, dest, true, 0, 0);
        long start = System.currentTimeMillis();
        action.execute();
        long elapsed = System.currentTimeMillis() - start;

        // No delay: must complete in well under 500ms
        assertTrue(elapsed < 500, "Compression with maxDelay=0 should be instant, but took " + elapsed + "ms");
        assertTrue(dest.exists(), "Compressed file must exist after execute()");
        assertFalse(source.exists(), "Source file must be deleted after compression");
    }

    /** Legacy 4-arg constructor must still work with no delay (backward compatibility). */
    @Test
    void testLegacyConstructorNoDelay(@TempDir File tempDir) throws IOException {
        File source = new File(tempDir, "test-legacy.log");
        File dest = new File(tempDir, "test-legacy.log.gz");
        try (FileWriter writer = new FileWriter(source)) {
            writer.write("legacy test data");
        }
        GzCompressAction action = new GzCompressAction(source, dest, true, 0);
        long start = System.currentTimeMillis();
        action.execute();
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 500, "Legacy constructor should have no delay, but took " + elapsed + "ms");
        assertTrue(dest.exists(), "Compressed file must exist");
        assertFalse(source.exists(), "Source file must be deleted");
    }
}
