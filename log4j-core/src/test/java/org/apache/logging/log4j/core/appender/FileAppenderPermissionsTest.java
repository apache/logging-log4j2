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

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.junit.CleanUpDirectories;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.stream.Stream;

import static org.apache.logging.log4j.util.Unbox.box;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests {@link FileAppender}.
 */
@CleanUpDirectories(FileAppenderPermissionsTest.DIR)
public class FileAppenderPermissionsTest {

    static final String DIR = "target/permissions1";

    @BeforeAll
    public static void beforeClass() {
        System.setProperty("log4j2.debug", "true");
        assumeTrue(FileUtils.isFilePosixAttributeViewSupported());
    }

    @ParameterizedTest
    @CsvSource({ "rwxrwxrwx,true,2", "rw-r--r--,false,3", "rw-------,true,4", "rw-rw----,false,5" })
    public void testFilePermissionsAPI(final String filePermissions, final boolean createOnDemand, final int fileIndex)
            throws Exception {
        final File file = new File(DIR, "AppenderTest-" + fileIndex + ".log");
        final Path path = file.toPath();
        final Layout<String> layout = PatternLayout.newBuilder().withPattern(PatternLayout.SIMPLE_CONVERSION_PATTERN)
                .build();
        // @formatter:off
        final FileAppender appender = FileAppender.newBuilder()
            .withFileName(file.getAbsolutePath())
            .setName("test")
            .withImmediateFlush(false)
            .setIgnoreExceptions(false)
            .withBufferedIo(false)
            .withBufferSize(1)
            .setLayout(layout)
            .withCreateOnDemand(createOnDemand)
            .withFilePermissions(filePermissions)
            .build();
        // @formatter:on
        try {
            appender.start();
            assertTrue(appender.isStarted(), "Appender did not start");
            assertNotEquals(createOnDemand, Files.exists(path));
            long curLen = file.length();
            long prevLen = curLen;
            assertEquals(curLen, 0, "File length: " + curLen);
            for (int i = 0; i < 100; ++i) {
                final LogEvent event = Log4jLogEvent.newBuilder().setLoggerName("TestLogger") //
                        .setLoggerFqcn(FileAppenderPermissionsTest.class.getName()).setLevel(Level.INFO) //
                        .setMessage(new SimpleMessage("Test")).setThreadName(this.getClass().getSimpleName()) //
                        .setTimeMillis(System.currentTimeMillis()).build();
                try {
                    appender.append(event);
                    curLen = file.length();
                    assertTrue(curLen > prevLen, "File length: " + curLen);
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
        }
        assertFalse(appender.isStarted(), "Appender did not stop");
    }

    @ParameterizedTest
    @CsvSource({ "rwxrwxrwx,2", "rw-r--r--,3", "rw-------,4", "rw-rw----,5" })
    public void testFileUserGroupAPI(final String filePermissions, final int fileIndex)
            throws Exception {
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
            .withFileName(file.getAbsolutePath())
            .setName("test")
            .withImmediateFlush(true)
            .setIgnoreExceptions(false)
            .withBufferedIo(false)
            .withBufferSize(1)
            .setLayout(layout)
            .withFilePermissions(filePermissions)
            .withFileOwner(user)
            .withFileGroup(group)
            .build();
        // @formatter:on
        try {
            appender.start();
            assertTrue(appender.isStarted(), "Appender did not start");
            long curLen = file.length();
            long prevLen = curLen;
            assertEquals(curLen, 0, file + " File length: " + curLen);
            for (int i = 0; i < 100; ++i) {
                final LogEvent event = Log4jLogEvent.newBuilder().setLoggerName("TestLogger") //
                        .setLoggerFqcn(FileAppenderPermissionsTest.class.getName()).setLevel(Level.INFO) //
                        .setMessage(new SimpleMessage("Test")).setThreadName(this.getClass().getSimpleName()) //
                        .setTimeMillis(System.currentTimeMillis()).build();
                try {
                    appender.append(event);
                    curLen = file.length();
                    assertTrue(curLen > prevLen, "File length: " + curLen);
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
        }
        assertFalse(appender.isStarted(), "Appender did not stop");
    }

    @Test
    @LoggerContextSource(value = "log4j-posix.xml", timeout = 10)
    void testFilePermissions(final LoggerContext context) throws IOException {
        final ExtendedLogger logger = context.getLogger(getClass());
        for (int i = 0; i < 1000; i++) {
            logger.debug("This is test message number {}", box(i));
        }
        final String permissions = PosixFilePermissions.toString(
                Files.getPosixFilePermissions(Paths.get("target/permissions1/AppenderTest-1.log")));
        assertEquals("rw-------", permissions);
    }

    public static String findAGroup(final String user) throws IOException {
        if (SystemUtils.IS_OS_MAC_OSX) {
            return "staff";
        }
        try (final Stream<String> lines = Files.lines(Paths.get("/etc/group"))) {
            return lines.filter(group -> !group.startsWith(user) && group.contains(user))
                    .map(group -> group.substring(0, group.indexOf(':')))
                    .findAny()
                    .orElse(user);
        }
    }

    private static String findAUser() throws IOException {
        // On jenkins build within ubuntu, it is not possible to chmod to another user
        return System.getProperty("user.name");
    }


}
