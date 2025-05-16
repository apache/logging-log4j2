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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.CleanFoldersRuleExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class RollingDirectTimeNewDirectoryTest {

    private static final String CONFIG = "log4j-rolling-folder-direct.xml";

    // Note that the path is hardcoded in the configuration!
    private static final String DIR = "target/rolling-folder-direct";

    @RegisterExtension
    CleanFoldersRuleExtension extension = new CleanFoldersRuleExtension(
            DIR,
            CONFIG,
            RollingDirectTimeNewDirectoryTest.class.getName(),
            this.getClass().getClassLoader());

    /**
     * This test logs directly to the target file. Rollover is set to happen once per second. We pause once
     * to ensure a rollover takes place. However, it is possible that 3 or 4 rollovers could occur, depending
     * on when the test starts and what else is going on on the machine running the test.
     * @throws Exception
     */
    @Test
    public void streamClosedError(final LoggerContext loggerContext) throws Exception {

        final Logger logger = loggerContext.getLogger(RollingDirectTimeNewDirectoryTest.class.getName());

        for (int i = 0; i < 1000; i++) {
            logger.info("nHq6p9kgfvWfjzDRYbZp");
        }
        Thread.sleep(1500);
        for (int i = 0; i < 1000; i++) {
            logger.info("nHq6p9kgfvWfjzDRYbZp");
        }

        final File logDir = new File(DIR);
        final File[] logFolders = logDir.listFiles();
        assertNotNull(logFolders);
        Arrays.sort(logFolders);
        int totalFiles = 0;
        try {

            final int minExpectedLogFolderCount = 2;
            assertTrue(
                    logFolders.length >= minExpectedLogFolderCount,
                    "was expecting at least " + minExpectedLogFolderCount + " folders, " + "found "
                            + logFolders.length);

            for (File logFolder : logFolders) {
                final File[] logFiles = logFolder.listFiles();
                if (logFiles != null) {
                    assertTrue(logFiles.length <= 1, "Only 1 file per folder expected: got " + logFiles.length);
                    totalFiles += logFiles.length;
                }
            }
            assertTrue(totalFiles >= 2, "Expected at least 2 files");

        } catch (AssertionError error) {
            System.out.format("log directory (%s) contents:%n", DIR);
            final Iterator<File> fileIterator =
                    FileUtils.iterateFilesAndDirs(logDir, TrueFileFilter.TRUE, TrueFileFilter.TRUE);
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
