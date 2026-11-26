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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.CronExpression;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CronAndSizeRolloverFileNameTest {

    private static final String DAILY = "0 0 0 * * ?";

    private final NullConfiguration configuration = new NullConfiguration();

    @TempDir
    Path dir;

    @AfterEach
    void tearDown() {
        configuration.stop();
    }

    private TriggeringPolicy createPolicy() {
        return CompositeTriggeringPolicy.createPolicy(
                CronTriggeringPolicy.createPolicy(configuration, "false", DAILY),
                SizeBasedTriggeringPolicy.createPolicy("1"));
    }

    private static void write(final RollingFileManager manager, final String text) {
        final byte[] bytes = text.getBytes(UTF_8);
        manager.write(bytes, 0, bytes.length, true);
    }

    private static void writeAndRollBySize(final RollingFileManager manager, final String text) {
        write(manager, text);
        manager.checkRollover(Log4jLogEvent.newBuilder().build());
    }

    private Map<String, String> readFiles() throws IOException {
        final Map<String, String> files = new TreeMap<>();
        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (final Path file : stream) {
                files.put(file.getFileName().toString(), new String(Files.readAllBytes(file), UTF_8));
            }
        }
        return files;
    }

    @Test
    void testSizeRolloverAfterCronRolloverIsNamedAfterNewPeriod() throws Exception {
        final CronExpression cronExpression = new CronExpression(DAILY);
        final Date today = cronExpression.getPrevFireTime(new Date());
        final Date yesterday = cronExpression.getPrevFireTime(today);
        final Path file = dir.resolve("test.log");
        try (final RollingFileManager manager = new RollingFileManager(
                null,
                file.toString(),
                dir.resolve("test-%d{yyyy-MM-dd}-%i.log").toString(),
                new FileOutputStream(file.toFile()),
                true,
                false,
                0,
                System.currentTimeMillis(),
                createPolicy(),
                DefaultRolloverStrategy.newBuilder()
                        .setMax("2")
                        .setConfig(configuration)
                        .build(),
                null,
                PatternLayout.createDefaultLayout(),
                null,
                null,
                null,
                false,
                ByteBuffer.allocate(8192))) {
            manager.initialize();
            write(manager, "yesterday");
            manager.rollover(cronExpression.getPrevFireTime(today), today);
            writeAndRollBySize(manager, "today 1");
            writeAndRollBySize(manager, "today 2");
        }
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        final Map<String, String> expected = new TreeMap<>();
        expected.put("test.log", "");
        expected.put("test-" + format.format(yesterday) + "-1.log", "yesterday");
        expected.put("test-" + format.format(today) + "-1.log", "today 1");
        expected.put("test-" + format.format(today) + "-2.log", "today 2");
        assertEquals(expected, readFiles());
    }

    @Test
    void testDirectWriteSizeRolloverAfterCronRolloverStartsNewFile() throws Exception {
        final CronExpression cronExpression = new CronExpression(DAILY);
        final Date tomorrow = cronExpression.getNextValidTimeAfter(new Date());
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        final RollingFileAppender appender = RollingFileAppender.newBuilder()
                .setName("test")
                .setFilePattern(dir.resolve("test-%d{yyyy-MM-dd}-%i.log").toString())
                .setPolicy(createPolicy())
                .setConfiguration(configuration)
                .build();
        appender.start();
        try {
            final RollingFileManager manager = appender.getManager();
            write(manager, "today");
            manager.rollover(cronExpression.getPrevFireTime(tomorrow), tomorrow);
            writeAndRollBySize(manager, "tomorrow 1");
            writeAndRollBySize(manager, "tomorrow 2");
            write(manager, "tomorrow 3");
        } finally {
            appender.stop();
        }
        final Map<String, String> files = readFiles();
        assertEquals(4, files.size(), "each rollover must start a new file: " + files);
        assertEquals("today", files.get("test-" + format.format(new Date()) + "-1.log"), "files: " + files);
    }
}
