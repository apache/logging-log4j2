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

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.junit.CleanUpDirectories;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import static org.apache.logging.log4j.core.test.hamcrest.Descriptors.that;
import static org.apache.logging.log4j.core.test.hamcrest.FileMatchers.hasName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.jupiter.api.Assertions.*;

/**
 * LOG4J2-1766.
 */
@Tag("sleepy")
@DisabledOnOs(value = OS.MAC, disabledReason = "FileWatcher is not fast enough on macOS for this test")
public class RollingAppenderDirectWriteTempCompressedFilePatternTest {

    private static final String CONFIG = "log4j-rolling-direct-tmp-compress-folder.xml";

    private static final String DIR = "target/rolling-direct";

    @Test
    @CleanUpDirectories(DIR)
    @LoggerContextSource(value = CONFIG, timeout = 10)
    public void testAppender(final LoggerContext context) throws Exception {
        final File dir = new File(DIR);
        dir.mkdirs();
        final var logger = context.getLogger(getClass());
        try (final WatchService watcher = FileSystems.getDefault().newWatchService()) {
            WatchKey key = dir.toPath().register(watcher, StandardWatchEventKinds.ENTRY_CREATE);

            for (int i = 0; i < 100; ++i) {
                logger.debug("This is test message number " + i);
            }
            Thread.sleep(50);
            assertTrue(dir.exists() && dir.listFiles().length > 0, "Directory not created");
            final File[] files = dir.listFiles();
            assertNotNull(files);
            assertThat(files, hasItemInArray(that(hasName(that(endsWith(".gz"))))));

            int temporaryFilesCreated = 0;
            int compressedFiles = 0;
            key = watcher.take();

            for (final WatchEvent<?> event : key.pollEvents()) {
                final WatchEvent<Path> ev = (WatchEvent<Path>) event;
                final Path filename = ev.context();
                if (filename.toString().endsWith(".tmp")) {
                    temporaryFilesCreated++;
                }
                if (filename.toString().endsWith(".gz")) {
                    compressedFiles++;
                }
            }
            assertTrue(temporaryFilesCreated > 0, "No temporary file created during compression");
            assertEquals(compressedFiles, temporaryFilesCreated,
                    "Temporary files created not equals to compressed files");
        }
    }
}
