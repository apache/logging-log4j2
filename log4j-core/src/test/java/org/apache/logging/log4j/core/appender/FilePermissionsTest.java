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
package org.apache.logging.log4j.core.appender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.junit.CleanFiles;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests {@link FileAppender}.
 */
@RunWith(Parameterized.class)
public class FilePermissionsTest {

    @Parameterized.Parameters(name = "{0} {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { //
              // @formatter:off
             {"rwxrwxrwx", true},
             {"rw-rw-r--", false},
             {"rw-------", true},
              });
              // @formatter:on
    }

    @BeforeClass
    public static void beforeClass() {
        // TEMP
        // TODO Fix on non-Windows.
        Assume.assumeTrue(SystemUtils.IS_OS_WINDOWS);
    }

    private final boolean createOnDemand;
    private final String filePermissions;

    public FilePermissionsTest(final String filePermissions, final boolean createOnDemand) {
        this.filePermissions = filePermissions;
        this.createOnDemand = createOnDemand;
    }

    private static final String FILE_NAME = "target/fileAppenderTest.log";
    private static final Path PATH = Paths.get(FILE_NAME);

    @Rule
    public CleanFiles files = new CleanFiles(PATH);

    @AfterClass
    public static void cleanupClass() {
        assertTrue("Manager for " + FILE_NAME + " not removed", !AbstractManager.hasManager(FILE_NAME));
    }

    @Test
    public void testFilePermissions() throws Exception {
        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return;
        }
        final Layout<String> layout = PatternLayout.newBuilder().withPattern(PatternLayout.SIMPLE_CONVERSION_PATTERN)
                .build();
        // @formatter:off
        final FileAppender appender = FileAppender.newBuilder()
            .withFileName(FILE_NAME)
            .withName("test")
            .withImmediateFlush(false)
            .withIgnoreExceptions(false)
            .withBufferedIo(false)
            .withBufferSize(1)
            .withLayout(layout)
            .withCreateOnDemand(createOnDemand)
            .withFilePermissions(filePermissions)
            .build();
        // @formatter:on
        try {
            appender.start();
            final File file = new File(FILE_NAME);
            assertTrue("Appender did not start", appender.isStarted());
            Assert.assertNotEquals(createOnDemand, Files.exists(PATH));
            long curLen = file.length();
            long prevLen = curLen;
            assertTrue("File length: " + curLen, curLen == 0);
            for (int i = 0; i < 100; ++i) {
                final LogEvent event = Log4jLogEvent.newBuilder().setLoggerName("TestLogger") //
                        .setLoggerFqcn(FilePermissionsTest.class.getName()).setLevel(Level.INFO) //
                        .setMessage(new SimpleMessage("Test")).setThreadName(this.getClass().getSimpleName()) //
                        .setTimeMillis(System.currentTimeMillis()).build();
                try {
                    appender.append(event);
                    curLen = file.length();
                    assertTrue("File length: " + curLen, curLen > prevLen);
                    // Give up control long enough for another thread/process to occasionally do something.
                    Thread.sleep(25);
                } catch (final Exception ex) {
                    throw ex;
                }
                prevLen = curLen;
            }
            assertEquals(filePermissions, PosixFilePermissions.toString(Files.getPosixFilePermissions(PATH)));
        } finally {
            appender.stop();
        }
        assertFalse("Appender did not stop", appender.isStarted());
    }
}
