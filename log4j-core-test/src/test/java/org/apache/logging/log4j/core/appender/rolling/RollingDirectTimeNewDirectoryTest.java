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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.test.junit.CleanUpDirectories;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RollingDirectTimeNewDirectoryTest {

    private static final String CONFIG = "log4j-rolling-folder-direct.xml";

    // Note that the path is hardcoded in the configuration!
    private static final String DIR = "target/rolling-folder-direct";
    private final AtomicLong currentTimeMillis = new AtomicLong(System.currentTimeMillis());

    @Factory
    Clock clock() {
        return currentTimeMillis::get;
    }

    @Test
    @CleanUpDirectories(DIR)
    @LoggerContextSource(CONFIG)
    public void streamClosedError(final LoggerContext context) throws Exception {

        final Logger logger = context.getLogger(RollingDirectTimeNewDirectoryTest.class.getName());

        for (int i = 0; i < 1000; i++) {
            logger.info("nHq6p9kgfvWfjzDRYbZp");
        }
        currentTimeMillis.addAndGet(1500);
        for (int i = 0; i < 1000; i++) {
            logger.info("nHq6p9kgfvWfjzDRYbZp");
        }

        File logDir = new File(DIR);
        File[] logFolders = logDir.listFiles();
        assertNotNull(logFolders);
        Arrays.sort(logFolders);

        try {

            final int minExpectedLogFolderCount = 2;
            assertTrue(logFolders.length >= minExpectedLogFolderCount, "was expecting at least " + minExpectedLogFolderCount + " folders, " +
                    "found " + logFolders.length);

            for (File logFolder : logFolders) {
                File[] logFiles = logFolder.listFiles();
                if (logFiles != null) {
                    Arrays.sort(logFiles);
                }
                assertTrue(logFiles != null && logFiles.length > 0, "empty folder: " + logFolder);
            }

        } catch (AssertionError error) {
            System.out.format("log directory (%s) contents:%n", DIR);
            final Iterator<File> fileIterator =
                    FileUtils.iterateFilesAndDirs(
                            logDir, TrueFileFilter.TRUE, TrueFileFilter.TRUE);
            int totalFileCount = 0;
            while (fileIterator.hasNext()) {
                totalFileCount++;
                final File file = fileIterator.next();
                System.out.format("-> %s (%d)%n", file, file.length());
            }
            System.out.format("total file count: %d%n", totalFileCount);
            throw new AssertionError("check failure", error);
        }

    }

}
