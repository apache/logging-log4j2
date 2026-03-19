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
package org.apache.logging.log4j.core.appender.rolling;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.logging.log4j.core.appender.rolling.action.Action;
import org.apache.logging.log4j.core.appender.rolling.action.GzCompressAction;
import org.apache.logging.log4j.core.appender.rolling.action.ZipCompressAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Issue #4012 — verifies that FileExtension.GZ and FileExtension.ZIP correctly pass
 * maxCompressionDelaySeconds through to the compression action.
 *
 * This was the root cause of the bug: the 5-argument createCompressAction() in GZ and ZIP
 * fell back to the 4-argument version, silently dropping the delay value.
 */
class FileExtensionCompressDelayTest {

    // ── GZ ────────────────────────────────────────────────────────────────

    /**
     * FileExtension.GZ.createCompressAction(5-args) must produce a GzCompressAction
     * that applies the random delay — not fall back to 0.
     */
    @Test
    void testGzCreateCompressActionWithDelay(@TempDir File tempDir) throws Exception {
        File source = new File(tempDir, "app.log");
        File dest = new File(tempDir, "app.log.gz");
        writeContent(source, "gz test content");

        int maxDelay = 2;
        Action action = FileExtension.GZ.createCompressAction(source.getPath(), dest.getPath(), true, -1, maxDelay);

        // Must return a GzCompressAction (not some other type)
        assertInstanceOf(GzCompressAction.class, action, "Expected GzCompressAction");

        long start = System.currentTimeMillis();
        action.execute();
        long elapsed = System.currentTimeMillis() - start;

        // Must finish within maxDelay + margin (delay IS applied via FileExtension)
        assertTrue(
                elapsed <= (maxDelay * 1000L) + 500,
                "GZ compress via FileExtension exceeded maxDelay=" + maxDelay + "s: " + elapsed + "ms");
        assertTrue(dest.exists(), "Compressed .gz file must exist");
        assertFalse(source.exists(), "Source must be deleted after compression");
    }

    /**
     * FileExtension.GZ.createCompressAction(5-args, delay=0) must behave identically
     * to the 4-arg version — instant compression, no delay.
     */
    @Test
    void testGzCreateCompressActionNoDelay(@TempDir File tempDir) throws Exception {
        File source = new File(tempDir, "app-nodelay.log");
        File dest = new File(tempDir, "app-nodelay.log.gz");
        writeContent(source, "gz no-delay content");

        Action action = FileExtension.GZ.createCompressAction(source.getPath(), dest.getPath(), true, -1, 0);

        assertInstanceOf(GzCompressAction.class, action);

        long start = System.currentTimeMillis();
        action.execute();
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 500, "GZ with delay=0 should be instant, took " + elapsed + "ms");
        assertTrue(dest.exists(), "Compressed .gz file must exist");
        assertFalse(source.exists(), "Source must be deleted");
    }

    // ── ZIP ───────────────────────────────────────────────────────────────

    /**
     * FileExtension.ZIP.createCompressAction(5-args) must produce a ZipCompressAction
     * that applies the random delay — not fall back to 0.
     */
    @Test
    void testZipCreateCompressActionWithDelay(@TempDir File tempDir) throws Exception {
        File source = new File(tempDir, "app.log");
        File dest = new File(tempDir, "app.log.zip");
        writeContent(source, "zip test content");

        int maxDelay = 2;
        Action action = FileExtension.ZIP.createCompressAction(source.getPath(), dest.getPath(), true, 0, maxDelay);

        assertInstanceOf(ZipCompressAction.class, action, "Expected ZipCompressAction");

        long start = System.currentTimeMillis();
        action.execute();
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(
                elapsed <= (maxDelay * 1000L) + 500,
                "ZIP compress via FileExtension exceeded maxDelay=" + maxDelay + "s: " + elapsed + "ms");
        assertTrue(dest.exists(), "Compressed .zip file must exist");
        assertFalse(source.exists(), "Source must be deleted after compression");
    }

    /**
     * FileExtension.ZIP.createCompressAction(5-args, delay=0) must behave identically
     * to the 4-arg version — instant compression, no delay.
     */
    @Test
    void testZipCreateCompressActionNoDelay(@TempDir File tempDir) throws Exception {
        File source = new File(tempDir, "app-nodelay.log");
        File dest = new File(tempDir, "app-nodelay.log.zip");
        writeContent(source, "zip no-delay content");

        Action action = FileExtension.ZIP.createCompressAction(source.getPath(), dest.getPath(), true, 0, 0);

        assertInstanceOf(ZipCompressAction.class, action);

        long start = System.currentTimeMillis();
        action.execute();
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 500, "ZIP with delay=0 should be instant, took " + elapsed + "ms");
        assertTrue(dest.exists(), "Compressed .zip file must exist");
        assertFalse(source.exists(), "Source must be deleted");
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private static void writeContent(final File file, final String content) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }
}
