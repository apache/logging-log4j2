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
package org.apache.logging.log4j.core.appender;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.test.junit.CleanUpFiles;
import org.junit.jupiter.api.Test;

/**
 * Tests that logged strings appear in the file, that the initial file size is the specified region length,
 * that the file is extended by region length when necessary, and that the file is shrunk to its actual usage when done.
 *
 * @since 2.1
 */
@CleanUpFiles({
    "target/MemoryMappedFileAppenderTest.log",
    "target/MemoryMappedFileAppenderRemapTest.log",
    "target/MemoryMappedFileAppenderLocationTest.log"
})
class MemoryMappedFileAppenderTest {

    @Test
    @LoggerContextSource("MemoryMappedFileAppenderTest.xml")
    void testMemMapBasics(final LoggerContext context) throws Exception {
        final Logger log = context.getLogger(getClass());
        final Path logFile = Paths.get("target", "MemoryMappedFileAppenderTest.log");
        try {
            log.warn("Test log1");
            assertTrue(Files.exists(logFile));
            assertEquals(MemoryMappedFileManager.DEFAULT_REGION_LENGTH, Files.size(logFile));
            log.warn("Test log2");
            assertEquals(MemoryMappedFileManager.DEFAULT_REGION_LENGTH, Files.size(logFile));
        } finally {
            context.stop();
        }
        final int LINESEP = System.lineSeparator().length();
        assertEquals(18 + 2 * LINESEP, Files.size(logFile));

        final List<String> lines = Files.readAllLines(logFile);
        assertThat(lines, both(hasSize(2)).and(contains("Test log1", "Test log2")));
    }

    @Test
    @LoggerContextSource("MemoryMappedFileAppenderRemapTest.xml")
    void testMemMapExtendsIfNeeded(final LoggerContext context) throws Exception {
        final Logger log = context.getLogger(getClass());
        final Path logFile = Paths.get("target", "MemoryMappedFileAppenderRemapTest.log");
        final char[] text = new char[256];
        Arrays.fill(text, 'A');
        final String str = new String(text);
        try {
            log.warn("Test log1");
            assertTrue(Files.exists(logFile));
            assertEquals(256, Files.size(logFile));
            log.warn(str);
            assertEquals(2 * 256, Files.size(logFile));
            log.warn(str);
            assertEquals(3 * 256, Files.size(logFile));
        } finally {
            context.stop();
        }
        assertEquals(521 + 3 * System.lineSeparator().length(), Files.size(logFile), "Expected file size to shrink");

        final List<String> lines = Files.readAllLines(logFile);
        assertThat(lines, both(hasSize(3)).and(contains("Test log1", str, str)));
    }

    @Test
    @LoggerContextSource("MemoryMappedFileAppenderLocationTest.xml")
    void testMemMapLocation(final LoggerContext context) throws Exception {
        final Logger log = context.getLogger(getClass());
        final Path logFile = Paths.get("target", "MemoryMappedFileAppenderLocationTest.log");
        final int expectedFileLength = Integers.ceilingNextPowerOfTwo(32000);
        assertEquals(32768, expectedFileLength);
        try {
            log.warn("Test log1");
            assertTrue(Files.exists(logFile));
            assertEquals(expectedFileLength, Files.size(logFile));
            log.warn("Test log2");
            assertEquals(expectedFileLength, Files.size(logFile));
        } finally {
            context.stop();
        }
        assertEquals(272 + 2 * System.lineSeparator().length(), Files.size(logFile), "Expected file size to shrink");

        final List<String> lines = Files.readAllLines(logFile);
        assertThat(
                lines,
                both(hasSize(2))
                        .and(
                                contains(
                                        "org.apache.logging.log4j.core.appender.MemoryMappedFileAppenderTest.testMemMapLocation(MemoryMappedFileAppenderTest.java:105): Test log1",
                                        "org.apache.logging.log4j.core.appender.MemoryMappedFileAppenderTest.testMemMapLocation(MemoryMappedFileAppenderTest.java:108): Test log2")));
    }
}
