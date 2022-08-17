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
package org.apache.logging.log4j.core.appender.rolling;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.test.junit.CleanUpDirectories;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LOG4J2-1766.
 */
@DisabledOnOs(value = OS.MAC, disabledReason = "FileWatcher is not fast enough on macOS for this test")
@Tag("sleepy")
public class RollingAppenderTempCompressedFilePatternTest {

    private static final String CONFIG = "log4j-rolling-gz-tmp-compress.xml";

    private static final String DIR = "target/rolling2";
    private static final String DIR_TMP = "target/rolling-tmp";

    @Test
    @CleanUpDirectories({ DIR, DIR_TMP })
    @LoggerContextSource(CONFIG)
    public void testAppender(final Logger logger, final LoggerContext context) throws Exception {
        final File dirTmp = new File(DIR_TMP);
        dirTmp.mkdirs();
        try (final WatchService watcher = FileSystems.getDefault().newWatchService()) {
            WatchKey key = dirTmp.toPath().register(watcher, StandardWatchEventKinds.ENTRY_CREATE);

            final List<String> messages = new ArrayList<>();
            for (int i = 0; i < 500; ++i) {
                final String message = "This is test message number " + i;
                messages.add(message);
                logger.debug(message);
                if (i % 100 == 0) {
                    Thread.sleep(500);
                }
            }
            assertTrue(context.stop(30, TimeUnit.SECONDS), () -> "Could not stop cleanly " + context + " for " + this);
            final File dir = new File(DIR);
            assertTrue(dir.exists(), "Directory not created");
            final File[] files = dir.listFiles();
            assertNotNull(files);
            int gzippedFiles = 0;
            for (final File file : files) {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InputStream in = null;
                final FileExtension ext = FileExtension.lookupForFile(file.getName());
                try {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        if (ext != null) {
                            gzippedFiles++;
                            try {
                                in = new CompressorStreamFactory().createCompressorInputStream(ext.name().toLowerCase(),
                                        fis);
                            } catch (final CompressorException ce) {
                                ce.printStackTrace();
                                fail(
                                        "Error creating intput stream from " + file.toString() + ": " + ce.getMessage());
                            }
                        } else {
                            in = new FileInputStream(file);
                        }
                        assertNotNull(in, "No input stream for " + file.getName());
                        try {
                            IOUtils.copy(in, baos);
                        } catch (final Exception ex) {
                            ex.printStackTrace();
                            fail("Unable to decompress " + file.getAbsolutePath());
                        }
                    }
                } finally {
                    Closer.close(in);
                }
                final String text = baos.toString(Charset.defaultCharset());
                final String[] lines = text.split("[\\r\\n]+");
                for (final String line : lines) {
                    messages.remove(line);
                }
            }
            assertTrue(messages.isEmpty(), "Log messages lost : " + messages.size());
            assertTrue(files.length > 2, "Files not rolled : " + files.length);
            assertTrue(gzippedFiles > 0, "Files gzipped not rolled : " + gzippedFiles);

            int temporaryFilesCreated = 0;
            key = watcher.take();

            for (final WatchEvent<?> event : key.pollEvents()) {
                final WatchEvent<Path> ev = (WatchEvent<Path>) event;
                final Path filename = ev.context();
                if (filename.toString().endsWith(".tmp")) {
                    temporaryFilesCreated++;
                }
            }
            assertTrue(temporaryFilesCreated > 0, "No temporary file created during compression");
            assertEquals(gzippedFiles, temporaryFilesCreated,
                    "Temporarys file created not equals to compressed files " + temporaryFilesCreated + "/"
                            + gzippedFiles);
        }
    }
}
