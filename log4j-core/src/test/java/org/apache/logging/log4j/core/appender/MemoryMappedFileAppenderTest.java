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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.junit.CleanUpFiles;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.assertj.core.api.HamcrestCondition;
import org.junit.jupiter.api.Test;

/**
 * Tests that logged strings appear in the file, that the initial file size is the specified specified region length,
 * that the file is extended by region length when necessary, and that the file is shrunk to its actual usage when done.
 *
 * @since 2.1
 */
@CleanUpFiles({
        "target/MemoryMappedFileAppenderTest.log",
        "target/MemoryMappedFileAppenderRemapTest.log",
        "target/MemoryMappedFileAppenderLocationTest.log"
})
public class MemoryMappedFileAppenderTest {

    @Test
    @LoggerContextSource("MemoryMappedFileAppenderTest.xml")
    public void testMemMapBasics(final LoggerContext context) throws Exception {
        final Logger log = context.getLogger(getClass());
        final Path logFile = Paths.get("target", "MemoryMappedFileAppenderTest.log");
        try {
            log.warn("Test log1");
            assertThat(Files.exists(logFile)).isTrue();
            assertThat(Files.size(logFile)).isEqualTo(MemoryMappedFileManager.DEFAULT_REGION_LENGTH);
            log.warn("Test log2");
            assertThat(Files.size(logFile)).isEqualTo(MemoryMappedFileManager.DEFAULT_REGION_LENGTH);
        } finally {
            context.stop();
        }
        final int LINESEP = System.lineSeparator().length();
        assertThat(Files.size(logFile)).isEqualTo(18 + 2 * LINESEP);

        final List<String> lines = Files.readAllLines(logFile);
        assertThat(lines).is(new HamcrestCondition<>(both(hasSize(2)).and(contains("Test log1", "Test log2"))));
    }

    @Test
    @LoggerContextSource("MemoryMappedFileAppenderRemapTest.xml")
    public void testMemMapExtendsIfNeeded(final LoggerContext context) throws Exception {
        final Logger log = context.getLogger(getClass());
        final Path logFile = Paths.get("target", "MemoryMappedFileAppenderRemapTest.log");
        final char[] text = new char[256];
        Arrays.fill(text, 'A');
        final String str = new String(text);
        try {
            log.warn("Test log1");
            assertThat(Files.exists(logFile)).isTrue();
            assertThat(Files.size(logFile)).isEqualTo(256);
            log.warn(str);
            assertThat(Files.size(logFile)).isEqualTo(2 * 256);
            log.warn(str);
            assertThat(Files.size(logFile)).isEqualTo(3 * 256);
        } finally {
            context.stop();
        }
        assertThat(Files.size(logFile)).describedAs("Expected file size to shrink").isEqualTo(521 + 3 * System.lineSeparator().length());

        final List<String> lines = Files.readAllLines(logFile);
        assertThat(lines).is(new HamcrestCondition<>(both(hasSize(3)).and(contains("Test log1", str, str))));
    }

    @Test
    @LoggerContextSource("MemoryMappedFileAppenderLocationTest.xml")
    void testMemMapLocation(final LoggerContext context) throws Exception {
        final Logger log = context.getLogger(getClass());
        final Path logFile = Paths.get("target", "MemoryMappedFileAppenderLocationTest.log");
        final int expectedFileLength = Integers.ceilingNextPowerOfTwo(32000);
        assertThat(expectedFileLength).isEqualTo(32768);
        try {
            log.warn("Test log1");
            assertThat(Files.exists(logFile)).isTrue();
            assertThat(Files.size(logFile)).isEqualTo(expectedFileLength);
            log.warn("Test log2");
            assertThat(Files.size(logFile)).isEqualTo(expectedFileLength);
        } finally {
            context.stop();
        }
        assertThat(Files.size(logFile)).describedAs("Expected file size to shrink").isEqualTo(272 + 2 * System.lineSeparator().length());

        final List<String> lines = Files.readAllLines(logFile);
        assertThat(lines).is(new HamcrestCondition<>(both(hasSize(2)).and(contains(
                "org.apache.logging.log4j.core.appender.MemoryMappedFileAppenderTest.testMemMapLocation(MemoryMappedFileAppenderTest.java:104): Test log1",
                "org.apache.logging.log4j.core.appender.MemoryMappedFileAppenderTest.testMemMapLocation(MemoryMappedFileAppenderTest.java:107): Test log2"
        ))));
    }
}
