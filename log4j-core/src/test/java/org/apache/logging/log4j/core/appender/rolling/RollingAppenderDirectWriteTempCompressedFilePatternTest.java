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

import static org.apache.logging.log4j.hamcrest.Descriptors.that;
import static org.apache.logging.log4j.hamcrest.FileMatchers.hasName;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * LOG4J2-1766.
 */
public class RollingAppenderDirectWriteTempCompressedFilePatternTest {

    private static final String CONFIG = "log4j-rolling-direct-tmp-compress-folder.xml";

    private static final String DIR = "target/rolling-direct";

    public static LoggerContextRule loggerContextRule = LoggerContextRule
            .createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

    private Logger logger;

    @Before
    public void setUp() throws Exception {
        // Disable this test on MacOS. FileWatcher isn't fast enough to work properly.
        Assume.assumeTrue(!SystemUtils.IS_OS_MAC_OSX);
        this.logger = loggerContextRule.getLogger(RollingAppenderDirectWriteTest.class.getName());
    }

    @Test
    public void testAppender() throws Exception {
        final File dir = new File(DIR);
        dir.mkdirs();
        try (final WatchService watcher = FileSystems.getDefault().newWatchService()) {
            WatchKey key = dir.toPath().register(watcher, StandardWatchEventKinds.ENTRY_CREATE);

            for (int i = 0; i < 100; ++i) {
                logger.debug("This is test message number " + i);
            }
            Thread.sleep(50);
            assertTrue("Directory not created", dir.exists() && dir.listFiles().length > 0);
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
            assertTrue("No temporary file created during compression", temporaryFilesCreated > 0);
            assertTrue("Temporarys file created not equals to compressed files",
                    compressedFiles == temporaryFilesCreated);
        }
    }
}
