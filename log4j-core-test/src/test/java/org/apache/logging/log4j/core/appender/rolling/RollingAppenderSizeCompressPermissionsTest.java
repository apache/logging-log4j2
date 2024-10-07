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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.rolling.action.CompressActionFactory;
import org.apache.logging.log4j.core.appender.rolling.action.CompressActionFactoryProvider;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.test.junit.CleanUpDirectories;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * LOG4J2-1699.
 */
@Tag("sleepy")
public class RollingAppenderSizeCompressPermissionsTest {

    private static final String CONFIG = "log4j-rolling-gz-posix.xml";

    private static final String DIR = "target/rollingpermissions1";

    @BeforeAll
    public static void beforeClass() {
        Assumptions.assumeTrue(FileUtils.isFilePosixAttributeViewSupported());
    }

    @Test
    @CleanUpDirectories(DIR)
    @LoggerContextSource(CONFIG)
    public void testAppenderCompressPermissions(final Logger logger, final LoggerContext context) throws Exception {
        for (int i = 0; i < 500; ++i) {
            final String message = "This is test message number " + i;
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
        int gzippedFiles1 = 0;
        int gzippedFiles2 = 0;
        for (final File file : files) {
            final CompressActionFactory ext = CompressActionFactoryProvider.newInstance(context.getConfiguration())
                    .createFactoryForFileName(file.getName());
            if (ext != null) {
                if (file.getName().startsWith("test1")) {
                    gzippedFiles1++;
                    assertEquals(
                            "rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(file.toPath())));
                } else {
                    gzippedFiles2++;
                    assertEquals(
                            "r--r--r--", PosixFilePermissions.toString(Files.getPosixFilePermissions(file.toPath())));
                }
            } else {
                if (file.getName().startsWith("test1")) {
                    assertEquals(
                            "rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(file.toPath())));
                } else {
                    assertEquals(
                            "rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(file.toPath())));
                }
            }
        }
        assertTrue(files.length > 2, "Files not rolled : " + files.length);
        assertTrue(gzippedFiles1 > 0, "Files 1 gzipped not rolled : " + gzippedFiles1);
        assertTrue(gzippedFiles2 > 0, "Files 2 gzipped not rolled : " + gzippedFiles2);
    }
}
