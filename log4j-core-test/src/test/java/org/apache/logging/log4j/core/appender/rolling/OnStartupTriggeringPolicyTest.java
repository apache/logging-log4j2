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

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link OnStartupTriggeringPolicy}.
 */
public class OnStartupTriggeringPolicyTest {

    private static final String TARGET_PATTERN = "/test1-%d{MM-dd-yyyy}-%i.log";
    private static final String TEST_DATA = "Hello world!";
    private static final FastDateFormat formatter = FastDateFormat.getInstance("MM-dd-yyyy");

    @TempDir
    Path tempDir;

    @Test
    public void testPolicy() throws Exception {
        final Configuration configuration = new DefaultConfiguration();
        final Path target = tempDir.resolve("testfile");
        final long timeStamp = Instant.now().minus(Duration.ofDays(1)).toEpochMilli();
        final String expectedDate = formatter.format(timeStamp);
        final Path rolled = tempDir.resolve("test1-" + expectedDate + "-1.log");
        final long copied;
        try (final InputStream is = new ByteArrayInputStream(TEST_DATA.getBytes(StandardCharsets.UTF_8))) {
            copied = Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
        }
        final long size = Files.size(target);
        assertTrue(size > 0);
        assertEquals(copied, size);

        final FileTime fileTime = FileTime.fromMillis(timeStamp);
        final BasicFileAttributeView attrs = Files.getFileAttributeView(target, BasicFileAttributeView.class);
        attrs.setTimes(fileTime, fileTime, fileTime);
        final PatternLayout layout = PatternLayout.newBuilder().withPattern("%msg").withConfiguration(configuration)
                .build();
        final RolloverStrategy strategy = DefaultRolloverStrategy.newBuilder().withCompressionLevelStr("0")
                .withStopCustomActionsOnError(true).withConfig(configuration).build();
        final OnStartupTriggeringPolicy policy = OnStartupTriggeringPolicy.createPolicy(1);

        try (final RollingFileManager manager = RollingFileManager.getFileManager(target.toString(), tempDir.toString() + TARGET_PATTERN, true,
                false, policy, strategy, null, layout, 8192, true, false, null, null, null, configuration)) {
            manager.initialize();
            final String files;
            try (Stream<Path> contents = Files.list(tempDir)) {
                files = contents.map(Path::toString).collect(Collectors.joining(", ", "[", "]"));
            }
            assertTrue(Files.exists(target), target.toString() + ", files = " + files);
            assertEquals(0, Files.size(target), target.toString());
            assertTrue(Files.exists(rolled), "Missing: " + rolled.toString() + ", files on disk = " + files);
            assertEquals(size, Files.size(rolled), rolled.toString());
        }
    }

}
