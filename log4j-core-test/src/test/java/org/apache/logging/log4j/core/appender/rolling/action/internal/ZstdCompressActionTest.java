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
package org.apache.logging.log4j.core.appender.rolling.action.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import org.apache.commons.compress.compressors.zstandard.ZstdConstants;
import org.apache.commons.compress.compressors.zstandard.ZstdUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.FileExtension;
import org.apache.logging.log4j.core.appender.rolling.NoOpTriggeringPolicy;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ZstdCompressActionTest {

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -1, 23})
    @UsingStatusListener
    void testMissingZstdJniOnlyFailsAsynchronousCompression(
            final int configuredLevel, @TempDir final File tempDir, final ListStatusListener statusListener)
            throws Exception {
        final File source = new File(tempDir, "missing-jni.log");
        final File renamed = new File(tempDir, "archive-1");
        writeContent(source, "source must be retained");
        final URL[] locations = {
            ZstdCompressAction.class.getProtectionDomain().getCodeSource().getLocation(),
            ZstdUtils.class.getProtectionDomain().getCodeSource().getLocation(),
            getClass().getProtectionDomain().getCodeSource().getLocation()
        };
        try (final URLClassLoader loader = new URLClassLoader(
                locations, getClass().getClassLoader()) {
            @Override
            protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("com.github.luben.zstd.")) {
                    throw new ClassNotFoundException(name);
                }
                if (name.startsWith("org.apache.logging.log4j.core.")
                        || name.startsWith("org.apache.commons.compress.")) {
                    synchronized (getClassLoadingLock(name)) {
                        Class<?> type = findLoadedClass(name);
                        if (type == null) {
                            type = findClass(name);
                        }
                        if (resolve) {
                            resolveClass(type);
                        }
                        return type;
                    }
                }
                return super.loadClass(name, resolve);
            }
        }) {
            statusListener.clear();
            final Thread thread = Thread.currentThread();
            final ClassLoader previous = thread.getContextClassLoader();
            try {
                thread.setContextClassLoader(loader);
                loader.loadClass(getClass().getName() + "$MissingZstdRollover")
                        .getMethod("verify", File.class, int.class)
                        .invoke(null, tempDir, configuredLevel);
            } finally {
                thread.setContextClassLoader(previous);
            }
            assertTrue(source.exists());
            assertEquals("Next", new String(Files.readAllBytes(source.toPath())));
            assertEquals("source must be retained", new String(Files.readAllBytes(renamed.toPath())));
            assertTrue(statusListener.findStatusData(Level.WARN).anyMatch(data -> data.getFormattedStatus()
                    .contains("zstd-jni is missing")));
        }
    }

    /** Runs the production rollover path in the loader that cannot access zstd-jni. */
    public static class MissingZstdRollover {
        public static void verify(final File tempDir, final int configuredLevel) throws IOException {
            final NullConfiguration configuration = new NullConfiguration();
            final RollingFileAppender appender = RollingFileAppender.newBuilder()
                    .setName("missing-zstd-jni")
                    .setConfiguration(configuration)
                    .setLayout(PatternLayout.newBuilder()
                            .setConfiguration(configuration)
                            .setPattern("%m")
                            .build())
                    .setFileName(new File(tempDir, "missing-jni.log").getPath())
                    .setFilePattern(new File(tempDir, "archive-%i.zst").getPath())
                    .setPolicy(NoOpTriggeringPolicy.INSTANCE)
                    .setStrategy(DefaultRolloverStrategy.newBuilder()
                            .setCompressionLevelStr(
                                    configuredLevel == CompressActionSupport.UNSPECIFIED_COMPRESSION_LEVEL
                                            ? null
                                            : Integer.toString(configuredLevel))
                            .setConfig(configuration)
                            .build())
                    .build();
            appender.start();
            try {
                appender.getManager().rollover();
                appender.append(Log4jLogEvent.newBuilder()
                        .setMessage(new SimpleMessage("Next"))
                        .build());
            } finally {
                appender.stop();
            }
        }
    }

    @Test
    void testMapsCompressionLevelZeroToDefault(@TempDir File tempDir) throws IOException {
        // Zstd reads level 0 as "use the default level", and it was accepted for `.zst` before 2.27.0
        File source = new File(tempDir, "zero.log");
        File dest = new File(tempDir, "zero.log.zst");
        writeContent(source, "zero compression level");
        ZstdCompressAction action = new ZstdCompressAction(source, dest, true, 0);

        assertEquals(0, ZstdCompressAction.resolveCompressionLevel(0));
        assertTrue(action.execute());
        assertTrue(dest.exists(), "Compressed file must exist after execute()");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -2, -5, -131072})
    void testAcceptsNativeNegativeLevels(final int level, @TempDir File tempDir) throws IOException {
        File source = new File(tempDir, "invalid-neg.log");
        File dest = new File(tempDir, "invalid-neg.log.zst");
        writeContent(source, "invalid negative compression level");
        ZstdCompressAction action = new ZstdCompressAction(source, dest, true, level);

        assertEquals(level, ZstdCompressAction.resolveCompressionLevel(level));
        assertTrue(action.execute());
        assertTrue(dest.exists());
        assertFalse(source.exists());
    }

    @Test
    void testFallsBackFromCompressionLevelAboveMax(@TempDir File tempDir) throws IOException {
        // Level 23 is above the currently supported maximum level (22)
        File source = new File(tempDir, "invalid-high.log");
        File dest = new File(tempDir, "invalid-high.log.zst");
        writeContent(source, "invalid high compression level");
        ZstdCompressAction action = new ZstdCompressAction(source, dest, true, 23);

        assertEquals(0, ZstdCompressAction.resolveCompressionLevel(23));
        assertTrue(action.execute());
        assertTrue(dest.exists());
        assertFalse(source.exists());
    }

    @Test
    void testUnsupportedCompressionLevelDoesNotEscapeActionRunner(@TempDir File tempDir) throws IOException {
        File source = new File(tempDir, "invalid.log");
        File dest = new File(tempDir, "invalid.log.zst");
        writeContent(source, "unsupported compression level");
        ZstdCompressAction action = new ZstdCompressAction(source, dest, true, 23);

        assertDoesNotThrow(action::run);
        assertTrue(action.isComplete());
        assertTrue(dest.exists());
    }

    @Test
    void testAssumedZstdLevelBoundsHaveNotChanged() {
        assertEquals(22, ZstdConstants.ZSTD_CLEVEL_MAX);
        assertEquals(3, ZstdConstants.ZSTD_CLEVEL_DEFAULT);
    }

    // Keep these bounds independent of the constants used by the implementation.
    @Test
    void testAcceptsZstdRangeBounds(@TempDir File tempDir) throws IOException {
        File minimumSource = new File(tempDir, "minimum.log");
        File minimumDestination = new File(tempDir, "minimum.log.zst");
        File maximumSource = new File(tempDir, "maximum.log");
        File maximumDestination = new File(tempDir, "maximum.log.zst");
        writeContent(minimumSource, "minimum compression level");
        writeContent(maximumSource, "maximum compression level");

        assertTrue(new ZstdCompressAction(minimumSource, minimumDestination, true, 1).execute());
        assertTrue(new ZstdCompressAction(maximumSource, maximumDestination, true, 22).execute());
    }

    @Test
    void testCompression(@TempDir File tempDir) throws IOException {
        File source = new File(tempDir, "test.log");
        File dest = new File(tempDir, "test.log.zst");
        writeContent(source, "test data");

        ZstdCompressAction action = new ZstdCompressAction(source, dest, true, ZstdConstants.ZSTD_CLEVEL_DEFAULT);

        assertTrue(action.execute());
        assertTrue(dest.exists(), "Compressed file must exist after execute()");
        assertFalse(source.exists(), "Source file must be deleted after compression");
    }

    @Test
    void testFileExtensionDefersUnspecifiedLevelMappingUntilExecution(@TempDir File tempDir) throws IOException {
        File source = new File(tempDir, "default.log");
        File destination = new File(tempDir, "default.log.zst");
        writeContent(source, "default compression level");

        ZstdCompressAction action = (ZstdCompressAction) FileExtension.ZSTD.createCompressAction(
                source.getPath(), destination.getPath(), true, CompressActionSupport.UNSPECIFIED_COMPRESSION_LEVEL);

        assertEquals(CompressActionSupport.UNSPECIFIED_COMPRESSION_LEVEL, action.getCompressionLevel());
        assertEquals(
                0, ZstdCompressAction.resolveCompressionLevel(CompressActionSupport.UNSPECIFIED_COMPRESSION_LEVEL));
        assertEquals(-1, ZstdCompressAction.resolveCompressionLevel(-1));
        assertTrue(action.execute());
        assertTrue(destination.exists(), "Compressed file must exist after execute()");
    }

    private static void writeContent(final File file, final String content) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }
}
