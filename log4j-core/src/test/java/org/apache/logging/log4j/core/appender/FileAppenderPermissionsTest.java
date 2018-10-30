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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests {@link FileAppender}.
 */
@RunWith(Parameterized.class)
public class FileAppenderPermissionsTest {

    private static final String DIR = "target/permissions1";

    @Parameterized.Parameters(name = "{0} {1} {2}")
    public static Collection<Object[]> data() throws IOException {
        return Arrays.asList(new Object[][] { //
              // @formatter:off
             {"rwxrwxrwx", true, 2},
             {"rw-r--r--", false, 3},
             {"rw-------", true, 4},
             {"rw-rw----", false, 5},
              });
              // @formatter:on
    }

    private final boolean createOnDemand;
    private final String filePermissions;
    private final int fileIndex;

    public FileAppenderPermissionsTest(final String filePermissions, final boolean createOnDemand, final int fileIndex) {
        this.filePermissions = filePermissions;
        this.createOnDemand = createOnDemand;
        this.fileIndex = fileIndex;
    }

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("log4j2.debug", "true");
        Assume.assumeTrue(FileUtils.isFilePosixAttributeViewSupported());
    }

    @Test
    public void testFilePermissionsAPI() throws Exception {
        final File file = new File(DIR, "AppenderTest-" + fileIndex + ".log");
        final Path path = file.toPath();
        final Layout<String> layout = PatternLayout.newBuilder().withPattern(PatternLayout.SIMPLE_CONVERSION_PATTERN)
                .build();
        // @formatter:off
        final FileAppender appender = FileAppender.newBuilder()
        .withFileName(file.getAbsolutePath()).setName("test")
            .withImmediateFlush(false).setIgnoreExceptions(false)
            .withBufferedIo(false)
            .withBufferSize(1).setLayout(layout)
            .withCreateOnDemand(createOnDemand)
            .withFilePermissions(filePermissions)
            .build();
        // @formatter:on
        try {
            appender.start();
            assertTrue("Appender did not start", appender.isStarted());
            Assert.assertNotEquals(createOnDemand, Files.exists(path));
            long curLen = file.length();
            long prevLen = curLen;
            assertTrue("File length: " + curLen, curLen == 0);
            for (int i = 0; i < 100; ++i) {
                final LogEvent event = Log4jLogEvent.newBuilder().setLoggerName("TestLogger") //
                        .setLoggerFqcn(FileAppenderPermissionsTest.class.getName()).setLevel(Level.INFO) //
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
            assertEquals(filePermissions, PosixFilePermissions.toString(Files.getPosixFilePermissions(path)));
        } finally {
            appender.stop();
            Files.deleteIfExists(path);
        }
        assertFalse("Appender did not stop", appender.isStarted());
    }

    @Test
    public void testFileUserGroupAPI() throws Exception {
        final File file = new File(DIR, "AppenderTest-" + (1000 + fileIndex) + ".log");
        final Path path = file.toPath();
        final String user = findAUser();
        assertNotNull(user);
        final String group = findAGroup(user);
        assertNotNull(group);

        final Layout<String> layout = PatternLayout.newBuilder().withPattern(PatternLayout.SIMPLE_CONVERSION_PATTERN)
                .build();
        // @formatter:off
        final FileAppender appender = FileAppender.newBuilder()
        .withFileName(file.getAbsolutePath()).setName("test")
            .withImmediateFlush(true).setIgnoreExceptions(false)
            .withBufferedIo(false)
            .withBufferSize(1).setLayout(layout)
            .withFilePermissions(filePermissions)
            .withFileOwner(user)
            .withFileGroup(group)
            .build();
        // @formatter:on
        try {
            appender.start();
            assertTrue("Appender did not start", appender.isStarted());
            long curLen = file.length();
            long prevLen = curLen;
            assertTrue(file + " File length: " + curLen, curLen == 0);
            for (int i = 0; i < 100; ++i) {
                final LogEvent event = Log4jLogEvent.newBuilder().setLoggerName("TestLogger") //
                        .setLoggerFqcn(FileAppenderPermissionsTest.class.getName()).setLevel(Level.INFO) //
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
            assertEquals(filePermissions, PosixFilePermissions.toString(Files.getPosixFilePermissions(path)));
            assertEquals(user, Files.getOwner(path).getName());
            assertEquals(group, Files.readAttributes(path, PosixFileAttributes.class).group().getName());
        } finally {
            appender.stop();
            Files.deleteIfExists(path);
        }
        assertFalse("Appender did not stop", appender.isStarted());
    }

    public static String findAGroup(final String user) throws IOException {
        if (SystemUtils.IS_OS_MAC_OSX) {
            return "staff";
        }
        String group = user;
        try (FileInputStream fis = new FileInputStream("/etc/group")) {
            final List<String> groups = org.apache.commons.io.IOUtils.readLines(fis, Charset.defaultCharset());
            for (int i = 0; i < groups.size(); i++) {
                final String aGroup = groups.get(i);
                if (!aGroup.startsWith(user) && aGroup.contains(user)) {
                    group = aGroup.split(":")[0];
                    break;
                }
            }
        }
        return group;
    }

    private static String findAUser() throws IOException {
        // On jenkins build within ubuntu, it is not possible to chmod to another user
        return System.getProperty("user.name");
    }


}
