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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.apache.logging.log4j.core.util.FileUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * LOG4J2-1699.
 */
public class RollingAppenderSizeCompressPermissionsTest {

    private static final String CONFIG = "log4j-rolling-gz-posix.xml";

    private static final String DIR = "target/rollingpermissions1";

    public static LoggerContextRule loggerContextRule =
            LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

    private Logger logger;

    @BeforeClass
    public static void beforeClass() {
        Assume.assumeTrue(FileUtils.isFilePosixAttributeViewSupported());
    }

    @Before
    public void setUp() throws Exception {
        this.logger = loggerContextRule.getLogger(RollingAppenderSizeCompressPermissionsTest.class.getName());
    }

    @Test
    public void testAppenderCompressPermissions() throws Exception {
        for (int i = 0; i < 500; ++i) {
            final String message = "This is test message number " + i;
            logger.debug(message);
            if (i % 100 == 0) {
                Thread.sleep(500);
            }
        }
        if (!loggerContextRule.getLoggerContext().stop(30, TimeUnit.SECONDS)) {
            System.err.println("Could not stop cleanly " + loggerContextRule + " for " + this);
        }
        final File dir = new File(DIR);
        assertTrue("Directory not created", dir.exists());
        final File[] files = dir.listFiles();
        assertNotNull(files);
        int gzippedFiles1 = 0;
        int gzippedFiles2 = 0;
        for (final File file : files) {
            final FileExtension ext = FileExtension.lookupForFile(file.getName());
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
            } else if (file.getName().startsWith("test1")) {
                assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(file.toPath())));
            } else {
                assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(file.toPath())));
            }
        }
        assertTrue("Files not rolled : " + files.length, files.length > 2);
        assertTrue("Files 1 gzipped not rolled : " + gzippedFiles1, gzippedFiles1 > 0);
        assertTrue("Files 2 gzipped not rolled : " + gzippedFiles2, gzippedFiles2 > 0);
    }
}
